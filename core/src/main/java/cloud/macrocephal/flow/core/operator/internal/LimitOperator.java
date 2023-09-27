package cloud.macrocephal.flow.core.operator.internal;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.Swarm;

import java.math.BigInteger;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicReference;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.requireNonNull;

public record LimitOperator<T>(BigInteger count) implements Operator<T, Swarm<T>> {
    public LimitOperator {
        requireNonNull(count);
    }

    @Override
    public Swarm<T> apply(Publisher<T> operand) {
        return new Swarm<>(subscriber -> {
            final var count = new AtomicReference<>(ZERO);
            operand.subscribe(new SpySubscriber<T, T>((next, subscription) -> {
                if (0 < count().compareTo(count.get())) {
                    subscriber.onNext(next);

                    if (count().equals(count.updateAndGet(ONE::add))) {
                        subscriber.onComplete();
                        subscription.cancel();
                    }
                }
            }, subscriber));
        });
    }
}
