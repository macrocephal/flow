package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.publisher.Driver;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.util.Objects.requireNonNull;

public abstract class PublisherStrategy<T> implements Publisher<T> {
    protected final Set<Subscriber<? super T>> subscribers = new LinkedHashSet<>();

    protected PublisherStrategy(Driver<T> driver) {
        requireNonNull(driver);
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
        subscribers.remove(subscriber);
    }

    protected <U> void noop(U ignored) {
    }
}
