package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Error;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.Driver;
import cloud.macrocephal.flow.core.publisher.Driver.Pull;
import cloud.macrocephal.flow.core.publisher.Driver.Push;
import cloud.macrocephal.flow.core.publisher.Single;
import cloud.macrocephal.flow.core.publisher.Swarm;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.Spliterator.ORDERED;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

public abstract sealed class Iteration3BasePublisher<T> implements Flow.Publisher<T> permits Single, Swarm {
    private final Set<Subscriber<? super T>> subscribers = new LinkedHashSet<>();
    // FIXME: 24/09/2023 Prefer a data structure that can hold as much as Long.MAX_VALUE items
    //                   because pullers return streams that can hold that much (and even more,
    //                   but since the Java Flow API request(long), let's be happy with long so far).
    private final List<Buffer<T>> buffers = new LinkedList<>();
    private LongFunction<Stream<Signal<T>>> puller;
    private boolean coldPushTriggerred;
    private final Driver<T> driver;
    private final int capacity;
    private boolean completed;
    private Throwable error;

    protected Iteration3BasePublisher(Driver<T> driver) {
        switch (this.driver = requireNonNull(driver)) {
            case Pull(int ignoredCapacity, final var pullerFactory) -> {
                requireNonNull(pullerFactory);
                capacity = -1;
            }
            //noinspection PatternVariableHidesField
            case Push(final var hot, final var capacity, final var pushConsumer) -> {
                requireNonNull(pushConsumer);
                this.capacity = capacity;

                if (hot) {
                    pushConsumer.accept(this::push);
                }
            }
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber);
        if (subscribers.add(subscriber)) {
            if (!isNull(error)) {
                error(subscriber, error);
            } else if (completed) {
                complete(subscriber);
            } else {
                switch (driver) {
                    //noinspection PatternVariableHidesField
                    case Pull(int capacity, final var pullerFactory) -> {
                        if (capacity <= 0) {
                            subscribePullDirect(subscriber, pullerFactory);
                        } else {
                            subscribePullSharing(subscriber, pullerFactory);
                        }
                    }
                    //noinspection PatternVariableHidesField
                    case Push(final var hot, final var capacity, final var pushConsumer) -> {
                        if (!hot && !coldPushTriggerred) {
                            coldPushTriggerred = true;
                            pushConsumer.accept(this::push);
                        }

                        subscriber.onSubscribe(new Subscription() {
                            @Override
                            public void request(long n) {
                                final var counter$ = new long[]{max(0, n)};
                                synchronized (Iteration3BasePublisher.this) {
                                    if (0 < capacity &&                         // Is sharing driver
                                            0 <= counter$[0] &&                 // Counter is worth trying
                                            subscribers.contains(subscriber) && // The associated subscriber is active
                                            tryAdvance(subscriber)) {           // Error/Complete? We might want to stop
                                        Buffer<T> next;
                                        final var iterator = buffers.iterator();
                                        while (0 < counter$[0] && iterator.hasNext()) {
                                            if ((next = iterator.next()).subscribers().remove(subscriber)) {
                                                if (next.subscribers().isEmpty()) {
                                                    iterator.remove();
                                                }
                                                subscriber.onNext(next.value());
                                                --counter$[0];
                                            }
                                        }
                                        iterator.forEachRemaining(identity()::apply);
                                    }

                                    tryAdvance(subscriber);
                                }
                            }

                            @Override
                            public void cancel() {
                                Iteration3BasePublisher.this.cancel(subscriber);
                            }
                        });
                    }
                }
            }
        }
    }

    private void subscribePullSharing(Subscriber<? super T> subscriber,
                                      Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) {
        ofNullable(puller).orElseGet(() -> puller = pullerFactory.get());
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                synchronized (Iteration3BasePublisher.this) {
                    if (subscribers.contains(subscriber)) {
                        if (!isNull(error)) {
                            error(subscriber, error);
                        } else if (completed) {
                            complete(subscriber);
                        } else {
                            final var counter$ = new long[]{max(0, n)};
                            if (0 == counter$[0]) return;
                            final var bufferIterator = buffers.iterator();
                            final var buffered = stream(new AbstractSpliterator<T>(MAX_VALUE, ORDERED) {
                                @Override
                                public boolean tryAdvance(Consumer<? super T> action) {
                                    Buffer<T> next;
                                    if (0 < counter$[0] &&
                                            bufferIterator.hasNext() &&
                                            (next = bufferIterator.next()).subscribers().remove(subscriber)) {
                                        if (next.subscribers().isEmpty()) {
                                            bufferIterator.remove();
                                        }
                                        action.accept(next.value());
                                        --counter$[0];
                                        return true;
                                    }
                                    return false;
                                }
                            }, false);

                            if (0 < counter$[0]) {
                                // FIXME: See note on buffer size
                                final var remainder$ = new long[]{counter$[0]};
                                final var iterator = puller.apply(remainder$[0]).iterator();
                                final var queried = stream(new AbstractSpliterator<T>(MAX_VALUE, ORDERED) {
                                    @Override
                                    public boolean tryAdvance(Consumer<? super T> action) {
                                        if (0 < remainder$[0] && iterator.hasNext()) {
                                            switch (requireNonNull(iterator.next())) {
                                                case Value(final var value) -> {
                                                    action.accept(requireNonNull(value));
                                                    buffers.add(new Buffer<>(value,
                                                            new LinkedHashSet<>(subscribers)));
                                                }
                                                case Error(final var throwable) -> {
                                                    Iteration3BasePublisher.this.error = throwable;
                                                    return false;
                                                }

                                                case Complete() -> {
                                                    Iteration3BasePublisher.this.completed = true;
                                                    return false;
                                                }
                                            }

                                            --remainder$[0];
                                            return true;
                                        }
                                        return false;
                                    }
                                }, false);
                                concat(buffered, queried).forEach(subscriber::onNext);

                                if (!isNull(error)) {
                                    error(subscriber, error);
                                } else if (completed) {
                                    complete(subscriber);
                                }
                            } else {
                                buffered.forEach(subscriber::onNext);
                            }
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                Iteration3BasePublisher.this.cancel(subscriber);
            }
        });
    }

    private void subscribePullDirect(Subscriber<? super T> subscriber,
                                     Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) {
        final var puller = pullerFactory.get();
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                synchronized (Iteration3BasePublisher.this) {
                    if (subscribers.contains(subscriber)) {
                        final var counter$ = new long[]{max(0, n)};
                        if (0 < counter$[0]) {
                            final var iterator = puller.apply(counter$[0]).iterator();
                            while (iterator.hasNext() && 0 < counter$[0]) {
                                switch (iterator.next()) {
                                    case Error(final var throwable) -> {
                                        Iteration3BasePublisher.this.error(subscriber, throwable);
                                        return;
                                    }
                                    case Complete() -> {
                                        Iteration3BasePublisher.this.complete(subscriber);
                                        return;
                                    }
                                    case Value(final var value) -> {
                                        subscriber.onNext(requireNonNull(value));
                                        --counter$[0];
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                Iteration3BasePublisher.this.cancel(subscriber);
            }
        });
    }

    synchronized private boolean tryAdvance(Subscriber<? super T> subscriber) {
        if (!completed) {
            subscriber.onComplete();
            return false;
        } else if (!isNull(error)) {
            subscriber.onError(error);
            return false;
        } else {
            return true;
        }
    }

    synchronized private void error(Subscriber<? super T> subscriber, Throwable throwable) {
        subscriber.onError(throwable);
        cancel(subscriber);
    }

    synchronized private void complete(Subscriber<? super T> subscriber) {
        subscriber.onComplete();
        cancel(subscriber);
    }

    synchronized private void cancel(Subscriber<? super T> subscriber) {
        subscribers.remove(subscriber);
    }

    synchronized private void push(Signal<T> signal) {
        requireNonNull(signal);
        // TODO: implement
    }

    private record Buffer<T>(T value, Set<Subscriber<? super T>> subscribers) {
    }
}
