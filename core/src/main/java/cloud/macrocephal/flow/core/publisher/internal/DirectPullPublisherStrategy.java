package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.publisher.Driver;
import cloud.macrocephal.flow.core.publisher.Driver.Pull;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

public class DirectPullPublisherStrategy<T> extends PublisherStrategy<T> {
    private final Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory;

    public DirectPullPublisherStrategy(Driver<T> driver) {
        super(driver);
        if (driver instanceof Pull<T> pull && pull.capacity() <= 0) {
            this.pullerFactory = requireNonNull(pull.pullerFactory());
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(driver));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        if (subscribers.add(subscriber)) {
            final var puller = requireNonNull(pullerFactory.get());
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    DirectPullPublisherStrategy.this.request(puller, subscriber, n);
                }

                @Override
                public void cancel() {
                    DirectPullPublisherStrategy.this.cancel(subscriber);
                }
            });
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
                switch (iterator.next()) {
                    case Signal.Value(final var value) when 0 < counter$[0] -> {
                        final var next = requireNonNull(value);
                        subscriber.onNext(next);
                        --counter$[0];
                    }
                    case Signal.Error(final var throwable) -> {
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
