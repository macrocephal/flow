package cloud.macrocephal.flow.core.internal;

import cloud.macrocephal.flow.core.Signal;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class BasePublisher<T> implements Flow.Publisher<T> {
    private final List<Map.Entry<T, Set<Flow.Subscriber<? super T>>>> valueTracks = new LinkedList<>();
    private final Map<Flow.Subscriber<? super T>, BigInteger> subscriberCount = new LinkedHashMap<>();
    private volatile boolean opened = true;

    protected BasePublisher(Consumer<Consumer<Signal<T>>> publishExposure) {
        requireNonNull(publishExposure);
        publishExposure.accept(this::publish);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (opened) {
            requireNonNull(subscriber);
            subscriberCount.computeIfAbsent(subscriber, ignored -> {
                final var subscription = new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        BasePublisher.this.request(subscriber, n);
                        dispatch(subscriber);
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
    }

    synchronized private void dispatch(Flow.Subscriber<? super T> subscriber, BigInteger count) {
        if (opened) {
            Map.Entry<T, Set<Flow.Subscriber<? super T>>> next;
            Set<Flow.Subscriber<? super T>> subscribers;
            final var iterator = valueTracks.iterator();

            while (BigInteger.ZERO.compareTo(count) < 0 && iterator.hasNext()) {
                next = iterator.next();
                subscribers = next.getValue();

                if (subscribers.remove(subscriber)) {
                    count = count.subtract(BigInteger.ONE);
                    subscriber.onNext(next.getKey());

                    if (subscribers.isEmpty()) {
                        iterator.remove();
                    }
                }
            }

            subscriberCount.put(subscriber, count);
        }
    }

    synchronized private void dispatch(Flow.Subscriber<? super T> subscriber) {
        dispatch(subscriber, subscriberCount.getOrDefault(subscriber, BigInteger.ZERO));
    }

    synchronized private void request(Flow.Subscriber<? super T> subscriber, long n) {
        if (opened) {
            subscriberCount.computeIfPresent(subscriber, (ignored, counter) ->
                    counter.add(BigInteger.valueOf(Math.max(0L, n))));
        }
    }

    synchronized private void cancel(Flow.Subscriber<? super T> subscriber) {
        subscriberCount.remove(subscriber);
    }

    synchronized private void publish(Signal<T> signal) {
        if (opened) {
            requireNonNull(signal);
            switch (signal) {
                case Signal.Complete() -> {
                    subscriberCount.forEach((subscriber, ignored) -> subscriber.onComplete());
                    subscriberCount.clear();
                    opened = false;
                }
                case Signal.Value<T>(final var value) -> {
                    valueTracks.add(new AbstractMap.SimpleEntry<>(value, new HashSet<>(subscriberCount.keySet())));
                    subscriberCount.forEach(this::dispatch);
                }
                case Signal.Error<T>(final var throwable) -> {
                    subscriberCount.forEach((subscriber, ignored) -> subscriber.onError(throwable));
                    subscriberCount.clear();
                    opened = false;
                }
            }
        }
    }
}
