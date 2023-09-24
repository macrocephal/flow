# Flow

A modern Java reactive library:

+ **Flow** is built on `java.util.concurrent.Flow` API.
+ It is heavily inspired from:
  + RxJS (pipe, operators)
  + Project Reactor (singular/plural primitives, context propagation, concurrency-aware)
+ Its extensions leverage Java 21 virtual threads to offer compatibility closest to native Java

## Primitives

The java Flow API defines them so let's focus on the specifics of **Flow**:

+ `Single`: a publisher that can emit at most one value, or one error or a sigle completion signal
+ `Swarm`: a publisher that can emit as many values as provided, only stopped by an error or a completion signal
+ `Operator`: a transformation that can turn a publisher [or more] into another
+ `Pipe`: an abstraction to chain operations on publisher, compose them

## Syntax

Usage:

```java
import cloud.macrocephal.flow.core.OldSingle;
import cloud.macrocephal.flow.core.OldSwarm;

import static cloud.macrocephal.flow.core.flow.Operator.map;
import static cloud.macrocephal.flow.core.flow.Operator.flatMap;

Single.from(randomUUID())                       // [1] Create a Single of UUID
        .pipe(map(UUID::toString))              // [2] Transform it to a String
        .pipe(map(String::chars))               // [3] Turn String into Stream of characters
        .pipe(flatMap(Swarm::from))             // [4] Turn character stream into Swarm of characters 
        .subscribeNext(System.out::println);    // [5] Print UUID chars one at the time
```

More control:

```java
import cloud.macrocephal.flow.core.OldSwarm;
import cloud.macrocephal.flow.core.Signal;

import static cloud.macrocephal.flow.core.flow.Operator.map;
import static cloud.macrocephal.flow.core.flow.Operator.flatMap;

final var publish=new AtomicReference<Consumer<Signal<UUID>>>();
final var uuids=new Swarm<UUID>(publish::set);

// Pass `uuids` around, pipe it, subscribe to it, or do nothing: I'm not your mamma

        publish.accept(randomUUID());
        publish.accept(randomUUID());
        publish.accept(randomUUID());
        publish.accept(randomUUID());
```

## Roadmap

+ [ ] Core Module
  + [x] Publisher primitives
  + [x] Pipes
  + [ ] Builtin operators (count, sort, map, flatMap, reduce, etc)
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

TODO

## License

[MIT License](./LICENSE)

Copyright (c) 2023 Macrocephal Corp.
