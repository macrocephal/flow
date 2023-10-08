package cloud.macrocephal.flow.core.publisher.internal.strategy.multicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.buffer.Buffer;
import cloud.macrocephal.flow.core.exception.BackPressureException;
import cloud.macrocephal.flow.core.publisher.internal.strategy.Spec303Subscription;
import cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.BackPressureFeedback;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.math.BigInteger;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public class MulticastPushPublisherStrategy<T> extends BaseMulticastPublisherStrategy<T> {
    private final Consumer<BiConsumer<Signal<T>, BackPressureFeedback>> pushConsumer;
    private final BackPressureStrategy backPressureStrategy;
    private BackPressureFeedback backPressureFeedback;
    private boolean lazyPushBasedPublisherTriggerred;
    private final boolean lazy;
    private boolean paused;

    public MulticastPushPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        //noinspection PatternVariableHidesField
        if (publisherStrategy instanceof Push<T>(
                final var lazy,
                final var capacity,
                final var backPressureStrategy,
                final var pushConsumer
        ) && (isNull(capacity) || 0 < capacity.compareTo(ZERO))) {
            this.backPressureStrategy = backPressureStrategy;
            this.pushConsumer = pushConsumer;
            this.lazy = lazy;
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(publisherStrategy));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (subscriber) {
            final var richSubscriber = new RichSubscriber<>(new AtomicReference<>(ZERO), subscriber);
            if (active && !subscribers.contains(richSubscriber) && subscribers.add(richSubscriber)) {
                richSubscriber.onSubscribe(new Spec303Subscription<>(
                        richSubscriber,
                        ignored -> {
                            synchronized (MulticastPushPublisherStrategy.this) {
                                MulticastPushPublisherStrategy.this.cancel(richSubscriber);
                            }
                        },
                        n -> {
                            synchronized (MulticastPushPublisherStrategy.this) {
                                if (subscribers.contains(richSubscriber)) {
                                    richSubscriber.requested.updateAndGet(valueOf(n)::add);

                                    if (consumeAll(richSubscriber)) {
                                        tryTerminate(richSubscriber);
                                    }
                                }

                                if (lazy && !lazyPushBasedPublisherTriggerred) {
                                    lazyPushBasedPublisherTriggerred = true;
                                    startPushing();
                                }
                            }
                        }));

                if (!lazy) {
                    startPushing();
                }
            }
        }
    }

    synchronized private void startPushing() {
        pushConsumer.accept((signal, feedback) -> {
            synchronized (MulticastPushPublisherStrategy.this) {
                backPressureFeedback = feedback;

                switch (requireNonNull(signal)) {
                    case Signal.Error(var throwable) -> {
                        active = false;
                        error = throwable;
                        ofNullable(feedback).ifPresent(BackPressureFeedback::stop);
                        Buffer.from(subscribers).forEach(subscriber -> {
                            if (consumeAll((RichSubscriber<? super T>) subscriber)) {
                                tryTerminate(subscriber);
                            }
                        });
                    }
                    case Signal.Value(var value) -> {
                        final var next = requireNonNull(value);

                        if (isBufferFullCapacity()) {
                            switch (backPressureStrategy) {
                                case DROP -> {
                                }
                                case STOP -> {
                                    active = false;
                                    completed = true;
                                    ofNullable(feedback).ifPresent(BackPressureFeedback::stop);
                                    Buffer.from(subscribers).forEach(subscriber -> {
                                        if (consumeAll((RichSubscriber<? super T>) subscriber)) {
                                            error(subscriber, error);
                                        }
                                    });
                                }
                                case PAUSE -> {
                                    ofNullable(feedback).ifPresent(BackPressureFeedback::pause);
                                    paused = true;
                                }
                                case ERROR -> {
                                    active = false;
                                    error = new BackPressureException(this, null);
                                    ofNullable(feedback).ifPresent(BackPressureFeedback::stop);
                                    Buffer.from(subscribers).forEach(subscriber -> {
                                        if (consumeAll((RichSubscriber<? super T>) subscriber)) {
                                            error(subscriber, error);
                                        }
                                    });
                                }
                                case THROW -> throw new BackPressureException(this, capacity);
                            }
                        } else {
                            entries.add(new Entry<>(next, Buffer.from(subscribers)));
                            Buffer.from(subscribers).forEach(sub -> consumeAll((RichSubscriber<? super T>) sub));
                        }
                    }
                    case Signal.Complete() -> {
                        active = false;
                        completed = true;
                        ofNullable(feedback).ifPresent(BackPressureFeedback::stop);
                        Buffer.from(subscribers).forEach(subscriber -> {
                            if (consumeAll((RichSubscriber<? super T>) subscriber)) {
                                tryTerminate(subscriber);
                            }
                        });
                    }
                }
            }
        });
    }

    synchronized private boolean consumeAll(RichSubscriber<? super T> subscriber) {
        final var iterator = entries.iterator();
        boolean consumedAll = true;

        while (iterator.hasNext()) {
            //noinspection PatternVariableHidesField
            if (iterator.next() instanceof Entry(var value, var subscribers) && subscribers.contains(subscriber)) {
                if (0 < subscriber.requested.get().compareTo(ZERO)) {
                    subscriber.requested.updateAndGet(valueOf(-1)::add);
                    subscribers.remove(subscriber);
                    subscriber.onNext(value);

                    if (subscribers.isEmpty() && paused) {
                        paused = false;
                        ofNullable(backPressureFeedback).ifPresent(BackPressureFeedback::resume);
                    }
                } else if (consumedAll) {
                    consumedAll = false;
                }
            }
        }

        return consumedAll;
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class RichSubscriber<T> implements Subscriber<T> {
        private final AtomicReference<BigInteger> requested;
        private final Subscriber<T> subscriber;

        private RichSubscriber(AtomicReference<BigInteger> requested, Subscriber<T> subscriber) {
            this.requested = requested;
            this.subscriber = subscriber;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Subscriber<?> sub && subscriber.equals(sub);
        }

        @Override
        public int hashCode() {
            return subscriber.hashCode();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(T item) {
            subscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
        }
    }
}
