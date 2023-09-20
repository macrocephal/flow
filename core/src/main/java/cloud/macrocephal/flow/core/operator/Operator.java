package cloud.macrocephal.flow.core.operator;

import java.util.concurrent.Flow;

@FunctionalInterface
public interface Operator<T, U> {
    Flow.Publisher<U> apply(Flow.Publisher<T> operand);
}
