package cloud.macrocephal.flow.core.operator;

import cloud.macrocephal.flow.core.operator.internal.*;
import cloud.macrocephal.flow.core.publisher.Single;
import cloud.macrocephal.flow.core.publisher.Swarm;

import java.math.BigInteger;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

@FunctionalInterface
public interface Operator<T, O extends Publisher<?>> {
    O apply(Publisher<T> operand);

    static <T> Operator<T, Swarm<BigInteger>> counting() {
        return new CountingOperator<>();
    }

    static <T> Operator<T, Single<T>> nth(BigInteger count) {
        return new NthOperator<>(count);
    }

    static <T> Operator<T, Swarm<T>> limit(BigInteger count) {
        return new LimitOperator<>(count);
    }

    static <T> Operator<T, Single<T>> nthLast(BigInteger count) {
        return new NthLastOperator<>(count);
    }

    static <T, U> Operator<T, Swarm<U>> map(Function<T, U> mapper) {
        return new MapOperator<>(mapper);
    }
}
