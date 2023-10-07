package cloud.macrocephal.flow.core.publisher.internal.strategy;

import java.math.BigInteger;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.lang.Long.MAX_VALUE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import static java.util.Objects.requireNonNull;

public class Spec303Subscription<T> implements Subscription {
    private static final BigInteger MAX_LONG_VALUE = valueOf(MAX_VALUE);
    private final Consumer<Subscriber<? super T>> cancelHandler;
    private final Subscriber<? super T> subscriber;
    private final LongConsumer requestHandler;
    private BigInteger requests = ZERO;
    private boolean uncancelled = true;
    private boolean pending;

    public Spec303Subscription(Subscriber<? super T> subscriber,
                               Consumer<Subscriber<? super T>> cancelHandler, LongConsumer requestHandler) {
        this.requestHandler = requireNonNull(requestHandler);
        this.cancelHandler = requireNonNull(cancelHandler);
        this.subscriber = requireNonNull(subscriber);
    }

    @Override
    synchronized public void request(long n) {
        if (uncancelled) {
            if (n <= 0) {
                cancel();
                subscriber.onError(new IllegalArgumentException("Request count must be > 0. Given: %d".formatted(n)));
            } else if (pending) {
                requests = requests.add(valueOf(n));
            } else {
                pending = true;
                requestHandler.accept(n);

                while (0 < requests.compareTo(ZERO)) {
                    final var next = requests.min(MAX_LONG_VALUE);
                    requests = requests.subtract(next);
                    requestHandler.accept(next.longValue());
                }

                pending = false;
            }
        }
    }

    @Override
    synchronized public void cancel() {
        uncancelled = false;
        cancelHandler.accept(subscriber);
    }
}
