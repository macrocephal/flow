package cloud.macrocephal.flow.core.operator.internal;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

public class SpySubscriber<T, U> implements Subscriber<T> {
    protected final BiConsumer<? super T, Subscription> onNext;
    protected final Subscriber<? super U> subscriber;
    protected Subscription subscription;

    public SpySubscriber(BiConsumer<T, Subscription> onNext, Subscriber<? super U> subscriber) {
        this.subscriber = requireNonNull(subscriber);
        this.onNext = requireNonNull(onNext);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onNext(T item) {
        onNext.accept(item, subscription);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
