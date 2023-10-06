package cloud.macrocephal.flow.core.publisher.internal.strategy.multicast;

import cloud.macrocephal.flow.core.buffer.Buffer;
import cloud.macrocephal.flow.core.publisher.internal.strategy.BasePublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.math.BigInteger;
import java.util.concurrent.Flow.Subscriber;

import static java.math.BigInteger.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public abstract class BaseMulticastPublisherStrategy<T> extends BasePublisherStrategy<T> {
    protected final Buffer<Entry<T>> entries = Buffer.of();
    protected final BigInteger capacity;
    protected boolean active = true;
    protected boolean completed;
    protected Throwable error;

    protected BaseMulticastPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        switch (publisherStrategy) {
            case Pull<T> pull when isNull(pull.capacity()) || 0 < pull.capacity().compareTo(ZERO) ->
                    this.capacity = pull.capacity();
            case Push<T> push when isNull(push.capacity()) || 0 < push.capacity().compareTo(ZERO) ->
                    this.capacity = push.capacity();
            default -> throw new IllegalArgumentException("%s not accepted here.".formatted(publisherStrategy));
        }
    }

    @Override
    protected synchronized void error(Subscriber<? super T> subscriber, Throwable throwable) {
        freeUpSubscriber(subscriber);
        super.error(subscriber, throwable);
    }

    @Override
    synchronized protected void cancel(Subscriber<? super T> subscriber) {
        freeUpSubscriber(subscriber);

        super.cancel(subscriber);
    }

    synchronized protected boolean tryAdvance(Subscriber<? super T> subscriber) {
        if (completed) {
            complete(subscriber);
            return false;
        } else if (nonNull(error)) {
            error(subscriber, error);
            return false;
        } else {
            return true;
        }
    }

    private void freeUpSubscriber(Subscriber<? super T> subscriber) {
        if (subscribers.contains(subscriber)) {
            final var iterator = entries.iterator();

            while (iterator.hasNext()) {
                final var subscribers = iterator.next().subscribers();

                if (subscribers.remove(subscriber) && subscribers.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    protected boolean isBufferFullCapacity() {
        return nonNull(capacity) && capacity.equals(entries.size());
    }

    protected record Entry<T>(T value, Buffer<Subscriber<? super T>> subscribers) {
    }
}
