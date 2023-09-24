package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Error;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.exception.BackPressureException;
import cloud.macrocephal.flow.core.publisher.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.Driver;
import cloud.macrocephal.flow.core.publisher.Driver.Push;

import java.util.LinkedHashSet;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

public class SharingPushPublisherStrategy<T> extends BaseSharingPublisherStrategy<T> {
    private final Consumer<Function<Signal<T>, Boolean>> pushConsumer;
    private final BackPressureStrategy backPressureStrategy;
    private boolean coldPushBasedPublisherTriggerred;
    private final boolean cold;

    public SharingPushPublisherStrategy(Driver<T> driver) {
        super(driver);
        //noinspection PatternVariableHidesField
        if (driver instanceof Push<T>(
                final var hot,
                final var capacity,
                final var backPressureStrategy,
                final var pushConsumer
        ) && 0 < capacity) {
            this.backPressureStrategy = requireNonNull(backPressureStrategy);
            this.pushConsumer = requireNonNull(pushConsumer);
            this.cold = !hot;

            if (hot) {
                pushConsumer.accept(this::push);
            }
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(driver));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        if (active && subscribers.add(subscriber)) {
            if (cold && !coldPushBasedPublisherTriggerred) {
                coldPushBasedPublisherTriggerred = true;
                pushConsumer.accept(this::push);
            }

            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    SharingPushPublisherStrategy.this.request(subscriber, n);
                }

                @Override
                public void cancel() {
                    SharingPushPublisherStrategy.this.cancel(subscriber);
                }
            });
        }
    }

    synchronized private void request(Subscriber<? super T> subscriber, long n) {
        final var counter$ = new long[]{max(0, n)};

        if (active && subscribers.contains(subscriber) && tryAdvance(subscriber)) {
            final var iterator = entries.iterator();
            //noinspection PatternVariableHidesField
            while (0 < counter$[0] &&
                    iterator.hasNext() &&
                    iterator.next() instanceof Entry<T>(final var value, final var subscribers)) {
                if (subscribers.remove(subscriber)) {
                    if (subscribers.isEmpty()) {
                        iterator.remove();
                    }

                    subscriber.onNext(value);
                    --counter$[0];
                }
            }

            iterator.forEachRemaining(this::noop);
        }
    }

    synchronized private Boolean push(Signal<T> signal) {
        if (active) {
            switch (requireNonNull(signal)) {
                case Error(final var throwable) -> {
                    error = throwable;
                    active = false;
                }
                case Value(final var value) -> {
                    final var next = requireNonNull(value);

                    if (capacity < entries.size()) {
                        entries.add(new Entry<>(next, new LinkedHashSet<>(subscribers)));
                    } else {
                        return switch (backPressureStrategy) {
                            case DROP -> true;
                            case FEEDBACK -> null;
                            case ERROR -> {
                                push(new Error<>(new BackPressureException(this, capacity)));
                                yield active;
                            }
                            case THROW -> throw new BackPressureException(this, capacity);
                        };
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
