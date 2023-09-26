package cloud.macrocephal.flow.core.publisher;

import java.util.concurrent.Flow.Publisher;

@FunctionalInterface
public interface Operator<T, U, P extends Publisher<U>> {
    P apply(Publisher<T> operand);
}
