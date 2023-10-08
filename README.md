# Flow

## Table of Content

## Overview

This library provides a comprehensive implementation of the [Java 9 Flow API](https://docs.oracle.com/javase%2F9%2Fdocs%2Fapi%2F%2F/java/util/concurrent/Flow.html). It is heavily inspired from [RxJS](https://rxjs.dev/) operators chaining syntax while maintaining [Project Reactor](https://projectreactor.io/) similar idioms and capabilities.

## Features

1. Pull and Push based models for publisher strategies
2. Support for multiple back-pressure and lag strategies
3. Multicast and Unicast implementations
4. Customizable capacity to the infinite using BigInteger
5. Validated against [reactive-streams Flow TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.4/tck)tests

## Primitives

+ Publishers:
  + `Single`: a publisher that can complete, emit one error or at most one value;
  + `Swarm`: a publisher that can complete, emit on error or as many values as are possible from the source;
+ `Operator`: a transformation that can turn a publisher or more into another (`flow` provides a number of them out of the box)

## Installation

**Maven**

```xml
<dependency>
    <groupId>cloud.macrocephal.flow</groupId>
    <artifactId>flow-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usages

### Factories

```java
import cloud.macrocephal.flow.core.publisher.Single;
import cloud.macrocephal.flow.core.publisher.Swarm;

// Empty primitives
Single.empty();
Swarm.empty();

// Primitives of values
Single.of("Hello World!");
Swarm.of(Set.of(1, 2, 3, 5, 7, 11) /** or whatever collection **/);
```

Factories always produces unicast pull-based publishers.

### Constructors

```java
import cloud.macrocephal.flow.core.publisher.Single;
import cloud.macrocephal.flow.core.publisher.Swarm;

// Primitives from configuration
new Single(/** publisher strategy here **/);
new Swarm(/** publisher strategy here **/);

// Primitives adapters
new Single(/** some Java 9 Flow publisher **/);
new Swarm(/** some Java 9 Flow publisher **/);
```

Publisher strategies are implementations of the `cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy` sealed interface. They depict publishers behaviour around:

+ Push/Pull:
  + Push: The source of the publisher pushes as many data as it can, ideally, asynchronously
  + Pull: The source of the publisher is asked to compute n items which it tries to fully
+ Casting (decided by buffer capacity: _<= 0 translate to unicast, null or otherwise translate to multicast_):
  + Unicast: each subscriber got its source and tries receives its own sequence of data (e.g.: each subscriber get its sequence of random numbers)
  + Multicast: all subscribers share a source and receive the exact same sequence of data (e.g.: all subscribers get the same sequence of random numbers)
+ Back pressure / Lagging:
  + Back pressure: on push-based publisher, describe a state where subscribers cannot keep up with incoming data
    + Unicast: it's detected when a published item meets an accumulated request count at zero
    + Multicast: it's detected when some subscribers are slower that others and buffer reached capacity
  + Lagging: on pull-based publisher, describe that some subscribers cannot keep up with incoming data
    + Unicast: Not Applicable (N/A)
    + Multicast: it's detected when buffer that make subscribers sequence shared reach capacity
+ Eager/Lazy:
  + Pull: Not Applicable (N/A)
  + Push:
    + Eager: start pushing right after the first subscription
    + Lazy: start pushing right after the first request [from any subscriber]

Examples of strategies:
```java
// Unicast (capacity = 0) pull-based publisher: lag strategy is pointless on unicast
new Swarm<>(new Pull<>(
        0L, // Long is part of and overload constructor, could be null or a BigInteger
        LagStrategy.THROW,
        () -> request -> /** return a stream of n value **/));

// Multicast (capacity = Flow.defaultBufferSize()) pull-based publisher: will blow up the call stack if lag is detected
new Swarm<>(new Pull<>(
        LagStrategy.THROW,
        () -> request -> /** return a stream of n value **/));

// Multicast (capacity > 0) push-based publisher: will signal back pressure to source when detected
new Swarm<>(new Push<>(
        true, // Lazy publisher
        new BigInteger("0xfffffffffffffffffffff"), // Buffer capacity
        BackPressureStrategy.FEEDBACK, // Back pressure strategy
        push -> /** should start asynchronously call push.accept(signal, null or BackPressureFeedback-instance) **/
```

> **NOTE:** Setting buffer capacity to `null` should be a careful design decision as this translate to an infinite buffer. The buffers used in this implementation intentionally support a theoretical infinite size as real use case can be applied here. `java.lang.Long.MAX_MAX_VALUE` can be smaller than what a system can afford. Plus, buffer is processed sequentially and need not O(1) operations most of the time. Beware of Out Of Memory (OOM) exceptions. 

> **NOTE:** Eager/Lazy terminology should be disambiguated from Hot/Cold. Hot in reactive systems usually means that we start computing value without waiting for a subscription. I made the design choice to not support the latter as it is prone to lose values.

### Back Pressure / Lagging Strategies

They define the behaviour when back pressure / lag are detected, respectively.

LagStrategy:
+ `LagStrategy.THROW`: blow up the stack trace
+ `LagStrategy.ERROR`: mark publisher as in error state and propagate to subscribers
+ `LagStrategy.DROP`: forget that value as if it never happened

BackPressureStrategy:
+ `BackPressureStrategy.FEEDBACK`: try to pause the source (actual pausing is dependent on the source, it is implied that the value that triggered a positive back pressure detection was not accepted: the source might want to send it again)
+ `BackPressureStrategy.ERROR`: mark publisher as in error state and propagate to subscribers
+ `BackPressureStrategy.THROW`: blow up the stack trace
+ `BackPressureStrategy.DROP`: forget that value as if it never happened
+ `BackPressureStrategy.STOP`: try to stop the source from emitting more values

### Operators
```java
import cloud.macrocephal.flow.core.publisher.Single;
import cloud.macrocephal.flow.core.publisher.Swarm;
import static java.math.BigInteger.ONE;
import static cloud.macrocephal.flow.core.operator.Operator.counting;
import static cloud.macrocephal.flow.core.operator.Operator.flatMap;
import static cloud.macrocephal.flow.core.operator.Operator.map;
import static cloud.macrocephal.flow.core.operator.Operator.nthLast;

Single.of("Hello World!")
        .pipe(map(String::toUpperCase))
        .subscriber(/** some subscriber that print values **/); // HELLO WORLD!

Swarm.of(Set.of(1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 51))
        .pipe(counting()) // count and emit count as BigInteger, for each received values
        .pipe(nthLast(ONE)) // retain only the last count 
        .subscriber(/** some subscriber that print values **/); // 17

Swarm.of(Set.of(1, 2, 3, 4, 5, 6, 210))
        .pipe(flatMap(n -> /** publisher of prime factors of n **/))
        .subscriber(/** some subscriber that print values **/); // 1
                                                                // 2
                                                                // 3
                                                                // 2
                                                                // 2
                                                                // 5
                                                                // 2
                                                                // 3
                                                                // 2
                                                                // 3
                                                                // 5
                                                                // 7
```

**BONUS:** You can implement your custom operators by implementing `cloud.macrocephal.flow.core.operator.Operator`.

> **NOTE:** Although the specification says nothing about chaining operations [and thus operators], built-in operators consistently adhere to the principle that request are forwarded to upstream publisher by default. The exception being the `flatMap` case for which request count applies at the top most publisher initially, then, if there are active children, to the oldest of those children, until all children complete before it passes next request to the upstream publisher again.

## Roadmap

+ [ ] Core Module
  + [x] Publisher primitives
  + [x] Compatibility with Java Platform Module System (JPMS)
  + [x] Compatibility with [reactive-streams Flow TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.4/tck)
  + [x] Builtin operators (counting, map, flatMap, nth, nthLast, etc.)
  + [ ] Context and context propagation
+ [ ] Data Module
  + [ ] Redis
  + [ ] R2DBC
  + [ ] RabbitMQ
  + [ ] Apache Kafka
+ [ ] Validation
  + [ ] Hibernate Validator
+ [ ] Web Module

## Contribution

**Requirements**

+ Java JDK 21
+ Maven 4 _(I guess it could work with Maven 3, but I've grown accustomed to Maven 4)_
+ Git: to version-control your contribution

**Fork Project & Clone**

```shell
git clone git@github.com:macrocephal/flow.git
```

**Run Tests**

Test are implemented with [TestNG](https://testng.org/doc/), mainly because that is what the [reactive-streams Flow TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.4/tck) uses.

```shell
mvn clean install
```

**Compliance**

+ Stick to the [Reactive Manifesto](https://www.reactivemanifesto.org/) design principles
+ Adhere to the [Reactive Streams](http://www.reactive-streams.org/) standards, validated to the TCK tests
+ Ensure consistency through test cases: I welcome pull requests, documentation improvement and new features

## License

[MIT License](./LICENSE)

Copyright (c) 2023 Macrocephal Corp.
