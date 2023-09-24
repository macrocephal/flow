package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;

public final class Single<T> extends PublisherDefault<T> {
    public Single(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
    }
}
