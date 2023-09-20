package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.operator.Operator;

import java.util.concurrent.Flow;

public final class Pipe<T, U> {
    private final Operator<T, U> operator;

    public Pipe(Operator<T, U> operator) {
        this.operator = operator;
    }

    public <V> Pipe<T, V> pipe(Operator<U, V> operator) {
        return new Pipe<>(operand -> operator.apply(this.operator.apply(operand)));
    }

    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
