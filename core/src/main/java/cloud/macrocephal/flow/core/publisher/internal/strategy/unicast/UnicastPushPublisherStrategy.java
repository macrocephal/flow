package cloud.macrocephal.flow.core.publisher.internal.strategy.unicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.exception.BackPressureException;
import cloud.macrocephal.flow.core.publisher.internal.strategy.BasePublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.Spec303Subscription;
import cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.BackPressureFeedback;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.math.BigInteger;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import static java.util.Objects.*;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class UnicastPushPublisherStrategy<T> extends BasePublisherStrategy<T> {
    private final Consumer<BiConsumer<Signal<T>, BackPressureFeedback>> pushConsumer;
    private final BackPressureStrategy backPressureStrategy;
    private final boolean lazy;

    public UnicastPushPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        //noinspection PatternVariableHidesField
        if (publisherStrategy instanceof Push(
                final var lazy,
                final var capacity,
                final var backPressureStrategy,
                final var pushConsumer
        ) && 0 <= ZERO.compareTo(capacity)) {
            this.backPressureStrategy = backPressureStrategy;
            this.pushConsumer = pushConsumer;
            this.lazy = lazy;
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(publisherStrategy));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        synchronized (subscriber) {
            if (!subscribers.contains(subscriber) && subscribers.add(subscriber)) {
                final var lazyPushBasedPublisherTriggerred = new AtomicBoolean();
                final var active = new AtomicBoolean(true);
                final var resume = new AtomicReference<Runnable>();
                final var stop = new AtomicReference<Runnable>();
                final var requested = new AtomicReference<>(ZERO);
                subscriber.onSubscribe(new Spec303Subscription<T>(
                        subscriber,
                        ignored -> {
                            synchronized (UnicastPushPublisherStrategy.this) {
                                active.set(false);
                                UnicastPushPublisherStrategy.this.cancel(subscriber);
                                of(stop).map(AtomicReference::get).ifPresent(Runnable::run);
                            }
                        },
                        n -> {
                            synchronized (subscriber) {
                                final var TO_BE_RESUMED = ZERO.equals(requested.get());
                                requested.updateAndGet(valueOf(n)::add);
                                if (TO_BE_RESUMED && nonNull(resume.get())) {
                                    resume.getAndUpdate(__ -> null).run();
                                }

                                if (lazy && !lazyPushBasedPublisherTriggerred.get()) {
                                    lazyPushBasedPublisherTriggerred.set(true);
                                    startPushing(subscriber, requested, active, resume, stop);
                                }
                            }
                        }
                ));

                if (!lazy) {
                    startPushing(subscriber, requested, active, resume, stop);
                }
            }
        }
    }

    synchronized private void startPushing(Subscriber<? super T> subscriber,
                                           AtomicReference<BigInteger> requested,
                                           AtomicBoolean active,
                                           AtomicReference<Runnable> resume,
                                           AtomicReference<Runnable> stop) {
        pushConsumer.accept((signal, feedback) -> {
            synchronized (subscriber) {
                if (active.get()) {
                    ofNullable(stop.get()).orElseGet(() ->
                            stop.updateAndGet(ignored -> isNull(feedback) ? null : feedback::stop));

                    switch (requireNonNull(signal)) {
                        case Signal.Error(var throwable) -> {
                            active.set(false);
                            ofNullable(stop.get()).ifPresent(ignored -> {
                                resume.getAndUpdate(__ -> null);
                                stop.getAndUpdate(__ -> null).run();
                            });
                            error(subscriber, throwable);
                        }
                        case Signal.Value(var value) -> {
                            final var next = requireNonNull(value);

                            if (ZERO.equals(requested.get())) {
                                switch (backPressureStrategy) {
                                    case DROP -> {
                                    }
                                    case STOP -> {
                                        active.set(false);
                                        ofNullable(stop.get()).ifPresent(ignored -> {
                                            stop.getAndUpdate(__ -> null).run();
                                            resume.getAndUpdate(__ -> null);
                                        });
                                        complete(subscriber);
                                    }
                                    case PAUSE -> ofNullable(feedback).ifPresent(ignored -> {
                                        resume.updateAndGet(__ -> feedback::resume);
                                        feedback.pause();
                                    });
                                    case ERROR -> {
                                        active.set(false);
                                        ofNullable(stop.get()).ifPresent(ignored -> {
                                            stop.getAndUpdate(__ -> null).run();
                                            resume.getAndUpdate(__ -> null);
                                        });
                                        error(subscriber, new BackPressureException(this, ZERO));
                                    }
                                    case THROW -> throw new BackPressureException(this, ZERO);
                                }
                            } else {
                                requested.updateAndGet(valueOf(-1)::add);
                                subscriber.onNext(next);
                            }
                        }
                        case Signal.Complete() -> {
                            active.set(false);
                            complete(subscriber);
                        }
                    }
                }
            }
        });
    }
}
