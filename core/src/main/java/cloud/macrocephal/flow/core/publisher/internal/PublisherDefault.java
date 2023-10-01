package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.internal.strategy.multicast.MulticastPullPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.multicast.MulticastPushPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.unicast.UnicastPullPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.unicast.UnicastPushPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.math.BigInteger.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class PublisherDefault<T> implements Publisher<T> {
    private final Publisher<T> strategy;

    protected PublisherDefault(Publisher<T> publisher) {
        strategy = requireNonNull(publisher);
    }

    protected PublisherDefault(PublisherStrategy<T> publisherStrategy) {
        strategy = switch (publisherStrategy) {
            case Pull<T> pull -> isNull(pull.capacity()) || 0 < pull.capacity().compareTo(ZERO)
                    ? new MulticastPullPublisherStrategy<>(publisherStrategy)
                    : new UnicastPullPublisherStrategy<>(publisherStrategy);
            case Push<T> push -> isNull(push.capacity()) || 0 < push.capacity().compareTo(ZERO)
                    ? new MulticastPushPublisherStrategy<>(publisherStrategy)
                    : new UnicastPushPublisherStrategy<>(publisherStrategy);
        };
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        strategy.subscribe(subscriber);
    }

    protected <P extends Publisher<?>> P pipe(Operator<T, P> operator) {
        return operator.apply(this);
    }
}
