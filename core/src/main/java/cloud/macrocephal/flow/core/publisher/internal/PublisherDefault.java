package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.publisher.Operator;
import cloud.macrocephal.flow.core.publisher.internal.strategy.direct.DirectPullPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.direct.DirectPushPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.sharing.SharingPullPublisherStrategy;
import cloud.macrocephal.flow.core.publisher.internal.strategy.sharing.SharingPushPublisherStrategy;
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

    protected PublisherDefault(PublisherStrategy<T> publisherStrategy) {
        strategy = switch (requireNonNull(publisherStrategy)) {
            case Pull<T> pull -> isNull(pull.capacity()) || 0 < pull.capacity().compareTo(ZERO)
                    ? new SharingPullPublisherStrategy<>(publisherStrategy)
                    : new DirectPullPublisherStrategy<>(publisherStrategy);
            case Push<T> push -> isNull(push.capacity()) || 0 < push.capacity().compareTo(ZERO)
                    ? new SharingPushPublisherStrategy<>(publisherStrategy)
                    : new DirectPushPublisherStrategy<>(publisherStrategy);
        };
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        strategy.subscribe(subscriber);
    }

    protected <U, P extends Publisher<U>> P pipe(Operator<T, U, P> operator) {
        return operator.apply(this);
    }
}
