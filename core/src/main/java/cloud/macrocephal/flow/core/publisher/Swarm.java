package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;

import java.util.concurrent.Flow.Publisher;

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
}
