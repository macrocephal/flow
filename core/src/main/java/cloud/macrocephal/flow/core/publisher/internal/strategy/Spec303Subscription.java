package cloud.macrocephal.flow.core.publisher.internal.strategy;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.lang.Long.MAX_VALUE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import static java.util.Objects.requireNonNull;

public record Spec303Subscription<T>(Subscriber<? super T> subscriber,
                                     Consumer<Subscriber<? super T>> cancelHandler,
                                     LongConsumer requestHandler,
                                     List<Long> requests) implements Subscription {
    public Spec303Subscription(Subscriber<? super T> subscriber,
                               Consumer<Subscriber<? super T>> cancelHandler,
                               LongConsumer requestHandler) {
        this(subscriber, cancelHandler, requestHandler, new LinkedList<>());
    }

    public Spec303Subscription {
        requireNonNull(subscriber);
        requireNonNull(cancelHandler);
        requireNonNull(requestHandler);
        requireNonNull(requests);
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            cancel();
            subscriber.onError(new IllegalArgumentException("Request count must be > 0. Given: %d".formatted(n)));
        } else if (requests.isEmpty()) {
            requests.add(0L);
            requestHandler.accept(n);
            requests.removeFirst();

            if (!requests.isEmpty()) {
                n = requests.stream()
                        .map(BigInteger::valueOf)
                        .reduce(ZERO, BigInteger::add)
                        .min(valueOf(MAX_VALUE))
                        .longValue();
                System.out.println("@@@ " + n);
                requests.clear();
                request(n);
            }
        } else {
            requests.add(n);
        }
    }

    @Override
    public void cancel() {
        cancelHandler.accept(subscriber);
    }
}
