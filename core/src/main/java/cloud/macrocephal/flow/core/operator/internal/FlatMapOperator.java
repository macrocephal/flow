package cloud.macrocephal.flow.core.operator.internal;

import cloud.macrocephal.flow.core.buffer.Buffer;
import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.Swarm;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;

import static java.util.function.Function.identity;

public record FlatMapOperator<T, U>(Function<T, Publisher<U>> flatMapper) implements Operator<T, Swarm<U>> {
    @Override
    public Swarm<U> apply(Publisher<T> operand) {
        return new Swarm<>(subscriber -> operand.subscribe(new Subscriber<>() {
            private final Buffer<Subscription> subscriptions = Buffer.of();
            private final Subscriber<? super T> root = this;
            private long rememberBackpressureRequest;
            private Subscription subscription;
            private boolean upstreamCompleted;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                // NOTE: Decorate subscription to intercept cancel and request calls
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        synchronized (root) {
                            rememberBackpressureRequest = n;

                            if (!subscriptions.isEmpty()) {
                                final var iterator = subscriptions.iterator();
                                final var next = iterator.next();
                                iterator.forEachRemaining(identity()::apply);
                                next.request(n);
                            } else if (!upstreamCompleted) {
                                subscription.request(n);
                            }
                        }
                    }

                    @Override
                    public void cancel() {
                        synchronized (root) {
                            subscription.cancel();
                            final var iterator = subscriptions.iterator();
                            iterator.forEachRemaining(subscription -> {
                                subscription.cancel();
                                iterator.remove();
                            });
                        }
                    }
                });
            }

            @Override
            synchronized public void onError(Throwable throwable) {
                final var iterator = subscriptions.iterator();
                iterator.forEachRemaining(subscription -> {
                    subscription.cancel();
                    iterator.remove();
                });
                subscriber.onError(throwable);
            }

            @Override
            synchronized public void onNext(T item) {
                final Publisher<U> applied;
                try {
                    applied = flatMapper().apply(item);
                } catch (Throwable throwable) {
                    onError(throwable);
                    return;
                }

                applied.subscribe(new Subscriber<>() {
                    private Subscription lowLevelsubscription;

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        synchronized (root) {
                            subscriptions.add(lowLevelsubscription = subscription);
                            subscription.request(rememberBackpressureRequest);
                        }
                    }

                    @Override
                    public void onNext(U item) {
                        subscriber.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscription.cancel();
                        root.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        synchronized (root) {
                            subscriptions.remove(lowLevelsubscription);

                            if (subscriptions.isEmpty() && upstreamCompleted) {
                                subscriber.onComplete();
                            }
                        }
                    }
                });
            }

            @Override
            synchronized public void onComplete() {
                if (subscriptions.isEmpty()) {
                    subscriber.onComplete();
                }

                upstreamCompleted = true;
            }
        }));
    }
}
