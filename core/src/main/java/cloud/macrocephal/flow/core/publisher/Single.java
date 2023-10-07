package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.operator.internal.NthOperator;
import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.stream.Stream;

import static cloud.macrocephal.flow.core.publisher.strategy.LagStrategy.ERROR;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.requireNonNull;

public final class Single<T> extends PublisherDefault<T> {
    public Single(Publisher<T> publisher) {
        super(publisher);
    }

    public Single(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
    }

    public <P extends Publisher<?>> P pipe(Operator<T, P> operator) {
        return super.pipe(operand -> operator.apply(new NthOperator<T>(ONE).apply(operand)));
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        pipe(operand -> operand).subscribe(subscriber);
    }

    public static <T> Single<T> empty() {
        final var completeSignal = new Signal.Complete<T>();
        return new Single<>(new Pull<>(ZERO, ERROR, () -> ignored -> Stream.of(completeSignal)));
    }

    public static <T> Single<T> of(T value) {
        final var next = requireNonNull(value);
        final var valueSignal = new Signal.Value<>(next);
        final var completeSignal = new Signal.Complete<T>();
        return new Single<>(new Pull<>(ZERO, ERROR, () -> ignored -> Stream.of(valueSignal, completeSignal)));
    }
}
