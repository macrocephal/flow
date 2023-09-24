package cloud.macrocephal.flow.core.publisher.v2;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Error;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.v2.strategy.ComputeStrategy;
import cloud.macrocephal.flow.core.publisher.v2.strategy.DispatchStrategy;
import cloud.macrocephal.flow.core.publisher.v2.strategy.TriggerStrategy;

import java.util.*;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;

public final class Single<T> implements Publisher<T> {
    private final Set<Subscriber<? super T>> subscribers = new LinkedHashSet<>();
    private final List<Entry<T>> buffer = new LinkedList<>();
    private final ComputeStrategy<T> computeStrategy;
    private final DispatchStrategy dispatchStrategy;
    private final TriggerStrategy triggerStrategy;
    private boolean coldTriggered = false;
    private boolean completed = false;
    private Throwable error;

    public Single(TriggerStrategy triggerStrategy,
                  DispatchStrategy dispatchStrategy,
                  ComputeStrategy<T> computeStrategy) {
        this.dispatchStrategy = requireNonNull(dispatchStrategy);
        this.computeStrategy = requireNonNull(computeStrategy);
        this.triggerStrategy = requireNonNull(triggerStrategy);

        if (triggerStrategy instanceof TriggerStrategy.Hot) {
            if (computeStrategy instanceof ComputeStrategy.Push(final var pushConsumer)) {
                pushConsumer.accept(this::push);
            } else {
                throw new IllegalArgumentException("Cannot combine TriggerStrategy.Hot and DispatchStrategy.Pull");
            }
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber);

        if (subscribers.add(subscriber)) {
            switch (triggerStrategy) {
                case TriggerStrategy.Cold() -> {
                    switch (computeStrategy) {
                        case ComputeStrategy.Push<T>(final var pushConsumer) -> {
                            if (!coldTriggered) {
                                coldTriggered = true;
                                pushConsumer.accept(this::push);
                            }

                            subscriber.onSubscribe(new Subscription() {
                                @Override
                                public void request(long n) {
                                    // ???
                                }

                                @Override
                                public void cancel() {
                                    Single.this.cancel(subscriber);
                                }
                            });
                        }
                        case ComputeStrategy.PullSharing<T>(final var producer) -> subscriber.onSubscribe(new Subscription() {
                            @Override
                            public void request(long n) {
                                // ???
                            }

                            @Override
                            public void cancel() {
                                Single.this.cancel(subscriber);
                            }
                        });
                    }
                }
                case TriggerStrategy.Hot() -> subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        if (dispatchStrategy instanceof DispatchStrategy.Sharing) {

                        }
                    }

                    @Override
                    public void cancel() {
                        Single.this.cancel(subscriber);
                    }
                });
            }
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
        switch (dispatchStrategy) {
            case DispatchStrategy.Direct() -> {
                switch (signal) {
                    case Complete() -> subscribers.forEach(subscriber -> {
                        subscriber.onComplete();
                        cancel(subscriber);
                    });
                    case Error(final var throwable) -> subscribers.forEach(subscriber -> subscriber.onError(throwable));
                    case Value(final var value) -> subscribers.forEach(subscriber -> subscriber.onNext(value));
                }
            }
            case DispatchStrategy.Sharing(final var capacity) -> {
                switch (signal) {
                    case Complete() -> {
                        completed = true;
                        final var LAGGING_SUBSCRIBER = getLaggingSubscribers();
                        subscribers.stream()
                                .filter(subscriber -> !LAGGING_SUBSCRIBER.contains(subscriber))
                                .forEach(this::complete);
                    }
                    case Error(final var throwable) -> {
                        error = throwable;
                        final var LAGGING_SUBSCRIBER = getLaggingSubscribers();
                        subscribers.stream()
                                .filter(subscriber -> !LAGGING_SUBSCRIBER.contains(subscriber))
                                .forEach(subscriber -> error(subscriber, throwable));
                    }
                    case Value(final var value) -> {
                        if (capacity < buffer.size()) {
                            buffer.add(new Entry<>(value, new LinkedHashSet<>(subscribers)));
                        } else {
                            throw new IllegalStateException("Shared buffer out of capacity(%d)".formatted(capacity));
                        }
                    }
                }
            }
        }
    }

    private Set<Subscriber<? super T>> getLaggingSubscribers() {
        return buffer.stream()
                .map(Entry::subscribers)
                .flatMap(Collection::stream)
                .collect(toUnmodifiableSet());
    }

    private record Entry<T>(T value, Set<Subscriber<? super T>> subscribers) {
    }
}
