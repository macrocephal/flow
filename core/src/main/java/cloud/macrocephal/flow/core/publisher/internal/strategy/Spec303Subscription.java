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

public class Spec303Subscription<T> implements Subscription {
    private final Consumer<Subscriber<? super T>> cancelHandler;
    private final List<Long> requests = new LinkedList<>();
    private final Subscriber<? super T> subscriber;
    private final LongConsumer requestHandler;

    public Spec303Subscription(Subscriber<? super T> subscriber,
                               Consumer<Subscriber<? super T>> cancelHandler,
                               LongConsumer requestHandler) {
        this.requestHandler = requireNonNull(requestHandler);
        this.cancelHandler = requireNonNull(cancelHandler);
        this.subscriber = requireNonNull(subscriber);
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
