package cloud.macrocephal.flow.core.internal;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.exception.BackPressureException;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Flow.defaultBufferSize;

public class BasePublisher<T> implements Flow.Publisher<T> {
    private final List<Map.Entry<Signal<T>, Set<Flow.Subscriber<? super T>>>> valueTracks = new LinkedList<>();
    private final Map<Flow.Subscriber<? super T>, BigInteger> subscriberCount = new LinkedHashMap<>();
    private volatile boolean opened = true;
    // FIXME: This capacity has room for ...values + error/complete, b/c Java collection size is int.
    //        We want capacity to mean what it says: number of ...values and nothing more.
    //        The issue really gets tricky with Integer.MAX_VALUE as capacity
    private final int capacity;

    protected BasePublisher(Consumer<Consumer<Signal<T>>> publishExposure, int capacity) {
        requireNonNull(publishExposure);
        publishExposure.accept(this::publish);
        this.capacity = capacity < Integer.MAX_VALUE ? capacity + 1 : capacity;
    }

    protected BasePublisher(Consumer<Consumer<Signal<T>>> publishExposure) {
        this(publishExposure, defaultBufferSize());
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

    synchronized private void dispatch(Flow.Subscriber<? super T> subscriber) {
        final var overstepped = new AtomicBoolean(false);
        valueTracks.removeIf(entry -> {
            if (entry.getValue().contains(subscriber)) {
                switch (entry.getKey()) {
                    case Signal.Value<T>(var value) when !overstepped.get() -> {
                        final var counter = subscriberCount.get(subscriber);

                        if (0 > BigInteger.ZERO.compareTo(counter)) {
                            subscriberCount.put(subscriber, counter.subtract(BigInteger.ONE));
                            entry.getValue().remove(subscriber);
                            subscriber.onNext(value);
                        } else {
                            overstepped.set(true);
                        }
                    }
                    case Signal.Complete() when !overstepped.get() -> {
                        entry.getValue().remove(subscriber);
                        subscriberCount.remove(subscriber);
                        subscriber.onComplete();
                    }
                    case Signal.Error<T>(var throwable) when !overstepped.get() -> {
                        entry.getValue().remove(subscriber);
                        subscriberCount.remove(subscriber);
                        subscriber.onError(throwable);
                    }
                    default -> {
                    }
                }
            }
            return entry.getValue().isEmpty();
        });
    }

    synchronized private void request(Flow.Subscriber<? super T> subscriber, long n) {
        subscriberCount.computeIfPresent(subscriber, (ignored, counter) ->
                counter.add(BigInteger.valueOf(Math.max(0L, n))));
    }

    synchronized private void cancel(Flow.Subscriber<? super T> subscriber) {
        valueTracks.removeIf(entry -> entry.getValue().remove(subscriber) && entry.getValue().isEmpty());
        subscriberCount.remove(subscriber);
    }

    synchronized private void publish(Signal<T> signal) {
        if (opened) {
            requireNonNull(signal);

            opened = signal instanceof Signal.Value<T>;

            if (capacity > valueTracks.size()) {
                valueTracks.add(new AbstractMap.SimpleEntry<>(signal, new HashSet<>(subscriberCount.keySet())));
                new LinkedHashSet<>(subscriberCount.keySet()).forEach(this::dispatch);
            } else {
                // FIXME: If there is no room in the buffer, what on earth is this going to achieve?
                //        And we do not want to rush lagging subscribers...
                //        Are we okay dropping last value for this one?
                //        Or should rather supersede ordering and push one last one?
                publish(new Signal.Error<>(new BackPressureException(this, capacity)));
            }
        }
    }
}
