package cloud.macrocephal.flow.core.publisher.internal.strategy.multicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Error;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.exception.BackPressureException;
import cloud.macrocephal.flow.core.publisher.internal.strategy.Spec303Subscription;
import cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static cloud.macrocephal.flow.core.buffer.Buffer.from;
import static java.lang.Math.max;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class MulticastPushPublisherStrategy<T> extends BaseMulticastPublisherStrategy<T> {
    private final Consumer<Function<Signal<T>, Boolean>> pushConsumer;
    private final BackPressureStrategy backPressureStrategy;
    private boolean coldPushBasedPublisherTriggerred;
    private final boolean cold;

    public MulticastPushPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        //noinspection PatternVariableHidesField
        if (publisherStrategy instanceof Push<T>(
                final var hot,
                final var capacity,
                final var backPressureStrategy,
                final var pushConsumer
        ) && (isNull(capacity) || 0 < capacity.compareTo(ZERO))) {
            this.backPressureStrategy = backPressureStrategy;
            this.pushConsumer = pushConsumer;
            this.cold = !hot;

            if (hot) {
                pushConsumer.accept(this::push);
            }
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(publisherStrategy));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        if (active && !subscribers.contains(subscriber) && subscribers.add(subscriber)) {
            if (cold && !coldPushBasedPublisherTriggerred) {
                coldPushBasedPublisherTriggerred = true;
                pushConsumer.accept(this::push);
            }

            subscriber.onSubscribe(new Spec303Subscription<T>(subscriber,
                    MulticastPushPublisherStrategy.this::cancel,
                    n -> MulticastPushPublisherStrategy.this.request(subscriber, n)));
        }
    }

    synchronized private void request(Subscriber<? super T> subscriber, long n) {
        final var counter$ = new long[]{max(0, n)};

        if (subscribers.contains(subscriber)) {
            final var iterator = entries.iterator();
            //noinspection PatternVariableHidesField
            while (0 < counter$[0] &&
                    iterator.hasNext() &&
                    iterator.next() instanceof Entry<T>(var value, var subscribers)) {
                if (subscribers.remove(subscriber)) {
                    if (subscribers.isEmpty()) {
                        iterator.remove();
                    }

                    subscriber.onNext(value);
                    --counter$[0];
                }
            }

            final var stillGotLeftOver = new AtomicBoolean();
            iterator.forEachRemaining(entry ->
                    stillGotLeftOver.set(stillGotLeftOver.get() || entry.subscribers().contains(subscriber)));

            if (!stillGotLeftOver.get()) {
                tryAdvance(subscriber);
            }
        }
    }

    synchronized private Boolean push(Signal<T> signal) {
        if (active) {
            switch (requireNonNull(signal)) {
                case Error(var throwable) -> {
                    error = throwable;
                    active = false;
                }
                case Value(var value) -> {
                    final var next = requireNonNull(value);

                    if (isBufferFullCapacity()) {
                        return switch (backPressureStrategy) {
                            case DROP -> true;
                            case FEEDBACK -> null;
                            case ERROR -> {
                                push(new Error<>(new BackPressureException(this, capacity)));
                                yield active;
                            }
                            case THROW -> throw new BackPressureException(this, capacity);
                        };
                    } else {
                        entries.add(new Entry<>(next, from(subscribers)));
                    }
                }
                case Complete() -> {
                    completed = true;
                    active = false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
