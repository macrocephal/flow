package cloud.macrocephal.flow.core.internal;

import cloud.macrocephal.flow.core.Signal;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class BasePublisher<T> implements Flow.Publisher<T> {
    private final Map<Flow.Subscriber<? super T>, BigInteger> subscriberCount = new LinkedHashMap<>();

    protected BasePublisher(Consumer<Consumer<Signal<T>>> publishExposure) {
        requireNonNull(publishExposure);
        publishExposure.accept(this::publish);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber);
        subscriberCount.computeIfAbsent(subscriber, ignored -> {
            final var subscription = new Flow.Subscription() {
                @Override
                public void request(long n) {
                    BasePublisher.this.request(subscriber, n);
                }

                @Override
                public void cancel() {
                    BasePublisher.this.cancel(subscriber);
                }
            };
            subscriber.onSubscribe(subscription);
            return BigInteger.ZERO;
        });
    }

    synchronized private void request(Flow.Subscriber<? super T> subscriber, long n) {
        subscriberCount.computeIfPresent(subscriber, (ignored, counter) ->
                counter.add(BigInteger.valueOf(Math.max(0L, n))));
    }

    synchronized private void cancel(Flow.Subscriber<? super T> subscriber) {
        subscriberCount.remove(subscriber);
    }

    synchronized private void publish(Signal<T> signal) {
        requireNonNull(signal);
        switch (signal) {
            case Signal.Complete() -> {
                subscriberCount.forEach((subscriber, ignored) -> subscriber.onComplete());
                subscriberCount.clear();
            }
            default -> {}
        }
    }
}
