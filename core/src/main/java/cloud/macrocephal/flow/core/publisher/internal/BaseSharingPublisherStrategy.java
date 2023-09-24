package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.publisher.Driver;
import cloud.macrocephal.flow.core.publisher.Driver.Pull;
import cloud.macrocephal.flow.core.publisher.Driver.Push;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Flow.Subscriber;

import static java.util.Objects.isNull;

public abstract class BaseSharingPublisherStrategy<T> extends PublisherStrategy<T> {
    protected final List<Entry<T>> entries = new LinkedList<>();
    protected boolean active = true;
    protected final int capacity;
    protected boolean completed;
    protected Throwable error;

    protected BaseSharingPublisherStrategy(Driver<T> driver) {
        super(driver);
        switch (driver) {
            //noinspection PatternVariableHidesField
            case Pull<T>(final var capacity, final var ignoredPullerFactory) when 0 < capacity ->
                    this.capacity = capacity;
            //noinspection PatternVariableHidesField
            case Push<T>(final var ignoredHot, final var capacity, final var ignoredPushConsumer) when 0 < capacity ->
                    this.capacity = capacity;
            default -> throw new IllegalArgumentException("%s not accepted here.".formatted(driver));
        }
    }

    @Override
    synchronized protected void cancel(Subscriber<? super T> subscriber) {
        if (subscribers.contains(subscriber)) {
            final var iterator = entries.iterator();

            while (iterator.hasNext()) {
                final var subscribers = iterator.next().subscribers();

                if (subscribers.remove(subscriber) && subscribers.isEmpty()) {
                    iterator.remove();
                }
            }
        }

        super.cancel(subscriber);
    }

    synchronized protected boolean tryAdvance(Subscriber<? super T> subscriber) {
        if (completed) {
            complete(subscriber);
            return false;
        } else if (!isNull(error)) {
            error(subscriber, error);
            return false;
        } else {
            return true;
        }
    }

    protected record Entry<T>(T value, Set<Subscriber<? super T>> subscribers) {
    }
}
