package cloud.macrocephal.flow.core.operator.internal;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.Swarm;

import java.math.BigInteger;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicReference;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public record CountingOperator<T>() implements Operator<T, Swarm<BigInteger>> {
    @Override
    public Swarm<BigInteger> apply(Publisher<T> operand) {
        return new Swarm<>(subscriber -> {
            final var count = new AtomicReference<>(ZERO);
            operand.subscribe(new SpySubscriber<>((next, subscription) -> count.updateAndGet(ONE::add), subscriber));
        });
    }
}
