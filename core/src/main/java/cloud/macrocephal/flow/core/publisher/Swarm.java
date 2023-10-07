package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.Stream;

import static cloud.macrocephal.flow.core.publisher.strategy.LagStrategy.ERROR;
import static java.math.BigInteger.ONE;
import static java.util.Objects.requireNonNull;

public final class Swarm<T> extends PublisherDefault<T> {
    public Swarm(Publisher<T> publisher) {
        super(publisher);
    }

    public Swarm(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
    }

    @Override
    public <P extends Publisher<?>> P pipe(Operator<T, P> operator) {
        return super.pipe(operator);
    }


    public static <T> Swarm<T> empty() {
        final var completeSignal = new Signal.Complete<T>();
        return new Swarm<>(new Pull<>(ONE, ERROR, () -> ignored -> Stream.of(completeSignal)));
    }

    public static <T> Swarm<T> of(Collection<T> values) {
        final var completeSignal = new Signal.Complete<T>();
        final var next = requireNonNull(values);
        return new Swarm<>(new Pull<>(ONE, ERROR, () -> ignored -> Stream.concat(
                next.stream().map(Objects::requireNonNull).map(Signal.Value::new), Stream.of(completeSignal))));
    }
}
