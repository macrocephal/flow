package cloud.macrocephal.flow.core.publisher.internal.strategy.unicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.publisher.internal.strategy.BasePublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.Spec303Subscription;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;

import java.util.concurrent.Flow.Subscriber;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.requireNonNull;

public class UnicastPullPublisherStrategy<T> extends BasePublisherStrategy<T> {
    private final Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory;

    public UnicastPullPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        if (publisherStrategy instanceof Pull<T> pull && 0 <= ZERO.compareTo(pull.capacity())) {
            this.pullerFactory = pull.pullerFactory();
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(publisherStrategy));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        if (!subscribers.contains(subscriber) && subscribers.add(subscriber)) {
            final var puller = requireNonNull(pullerFactory.get());
            subscriber.onSubscribe(new Spec303Subscription<T>(
                    subscriber,
                    UnicastPullPublisherStrategy.this::cancel,
                    n -> UnicastPullPublisherStrategy.this.request(puller, subscriber, n)));
        }
    }

    synchronized private void request(final LongFunction<Stream<Signal<T>>> puller,
                                      final Subscriber<? super T> subscriber,
                                      final long n) {
        final var counter$ = new long[]{max(0, n)};

        if (subscribers.contains(subscriber)) {
            final var response = requireNonNull(puller.apply(counter$[0]));
            final var iterator = response.iterator();

            while (iterator.hasNext()) {
                switch (requireNonNull(iterator.next())) {
                    case Signal.Value(var value) -> {
                        final var next = requireNonNull(value);
                        subscriber.onNext(next);
                        --counter$[0];
                    }
                    case Signal.Error(var throwable) -> {
                        iterator.forEachRemaining(this::noop);
                        error(subscriber, throwable);
                        return;
                    }
                    case Signal.Complete() -> {
                        iterator.forEachRemaining(this::noop);
                        complete(subscriber);
                        return;
                    }
                    default -> {
                    }
                }
            }

            iterator.forEachRemaining(this::noop);
        }
    }
}
