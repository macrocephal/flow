package cloud.macrocephal.flow.core.flow;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public final class Flow<T, U> {
    private final Operator<T, U> operator;

    public Flow(Operator<T, U> operator) {
        this.operator = operator;
    }

    public <V> Flow<T, V> pipe(Operator<U, V> operator) {
        return new Flow<>(new Operator<>() {
            @Override
            public <P extends Publisher<V>> P apply(Publisher<T> operand) {
                return operator.apply(Flow.this.operator.apply(operand));
            }
        });
    }

    public void subscribe(Subscriber<? super T> subscriber) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
