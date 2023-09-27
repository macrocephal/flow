package cloud.macrocephal.flow.core.operator.internal;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.Single;

import java.math.BigInteger;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicReference;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.requireNonNull;

public record NthOperator<T>(BigInteger nth) implements Operator<T, Single<T>> {
    public NthOperator {
        requireNonNull(nth);
    }

    @Override
    public Single<T> apply(Publisher<T> operand) {
        return new Single<>(subscriber -> {
            final var nth = new AtomicReference<>(ZERO);
            operand.subscribe(new SpySubscriber<T, T>((next, subscription) -> {
                if (nth().equals(nth.updateAndGet(ONE::add))) {
                    subscriber.onNext(next);
                    subscription.cancel();
                }
            }, subscriber));
        });
    }
}
