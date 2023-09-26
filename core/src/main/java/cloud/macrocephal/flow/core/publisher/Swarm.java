package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;

import java.util.concurrent.Flow.Publisher;

public final class Swarm<T> extends PublisherDefault<T> {
    public Swarm(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
    }

    @Override
    public <U, P extends Publisher<U>> P pipe(Operator<T, U, P> operator) {
        return super.pipe(operator);
    }
}
