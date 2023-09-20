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
            return BigInteger.ZERO;
        });
    }

    synchronized private void publish(Signal<T> signal) {
    }
}
