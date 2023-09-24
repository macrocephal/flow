package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;

public final class Swarm<T> extends PublisherDefault<T> {
    public Swarm(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
    }
}
