package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.buffer.Buffer;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.util.Objects.requireNonNull;

public abstract class BasePublisherStrategy<T> implements Publisher<T> {
    //    protected final Set<Subscriber<? super T>> subscribers = new LinkedHashSet<>();
    protected final Buffer<Subscriber<? super T>> subscribers = Buffer.of();

    protected BasePublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        requireNonNull(publisherStrategy);
    }

    synchronized protected void error(Subscriber<? super T> subscriber, Throwable throwable) {
        subscriber.onError(throwable);
        cancel(subscriber);
    }

    synchronized protected void complete(Subscriber<? super T> subscriber) {
        subscriber.onComplete();
        cancel(subscriber);
    }

    synchronized protected void cancel(Subscriber<? super T> subscriber) {
        final var iterator = subscribers.iterator();

        while (iterator.hasNext()) {
            if (subscriber == iterator.next()) {
                iterator.remove();
                iterator.forEachRemaining(this::noop);
            }
        }
    }

    protected <U> void noop(U ignored) {
    }
}
