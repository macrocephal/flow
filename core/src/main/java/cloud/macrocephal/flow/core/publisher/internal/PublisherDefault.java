package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.publisher.Driver;
import cloud.macrocephal.flow.core.publisher.Driver.Pull;
import cloud.macrocephal.flow.core.publisher.Driver.Push;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.util.Objects.requireNonNull;

public class PublisherDefault<T> implements Publisher<T> {
    private final PublisherStrategy<T> strategy;

    protected PublisherDefault(Driver<T> driver) {
        strategy = switch (requireNonNull(driver)) {
            case Pull<T> pull -> 0 <= pull.capacity()
                    ? new DirectPullPublisherStrategy<>(driver)
                    : new SharingPullPublisherStrategy<>(driver);
            case Push<T> push -> 0 <= push.capacity()
                    ? new DirectPushPublisherStrategy<>(driver)
                    : new SharingPushPublisherStrategy<>(driver);
        };
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        strategy.subscribe(subscriber);
    }
}
