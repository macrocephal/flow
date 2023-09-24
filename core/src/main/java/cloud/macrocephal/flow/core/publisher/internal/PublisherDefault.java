package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.util.Objects.requireNonNull;

public class PublisherDefault<T> implements Publisher<T> {
    private final BasePublisherStrategy<T> strategy;

    protected PublisherDefault(PublisherStrategy<T> publisherStrategy) {
        strategy = switch (requireNonNull(publisherStrategy)) {
            case Pull<T> pull -> 0 <= pull.capacity()
                    ? new DirectPullPublisherStrategy<>(publisherStrategy)
                    : new SharingPullPublisherStrategy<>(publisherStrategy);
            case Push<T> push -> 0 <= push.capacity()
                    ? new DirectPushPublisherStrategy<>(publisherStrategy)
                    : new SharingPushPublisherStrategy<>(publisherStrategy);
        };
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        strategy.subscribe(subscriber);
    }
}
