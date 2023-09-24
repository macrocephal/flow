package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.operator.Operator;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public final class Pipe<T, U> {
    private final Operator<T, U> operator;

    public Pipe(Operator<T, U> operator) {
        this.operator = operator;
    }

    public <V> Pipe<T, V> pipe(Operator<U, V> operator) {
        return new Pipe<>(new Operator<>() {
            @Override
            public <P extends Publisher<V>> P apply(Publisher<T> operand) {
                return operator.apply(Pipe.this.operator.apply(operand));
            }
        });
    }

    public void subscribe(Subscriber<? super T> subscriber) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
