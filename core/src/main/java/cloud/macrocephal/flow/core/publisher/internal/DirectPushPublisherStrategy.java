package cloud.macrocephal.flow.core.publisher.internal;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Error;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class DirectPushPublisherStrategy<T> extends BasePublisherStrategy<T> {
    private final Consumer<Function<Signal<T>, Boolean>> pushConsumer;
    private boolean coldPushBasedPublisherTriggerred;
    private boolean active = true;
    private final boolean cold;

    public DirectPushPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        //noinspection PatternVariableHidesField
        if (publisherStrategy instanceof Push(
                final var hot,
                final var capacity,
                final var ignoredBackPressureStrategy,
                final var pushConsumer
        ) && capacity <= 0) {
            this.pushConsumer = requireNonNull(pushConsumer);
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
        if (active && subscribers.add(subscriber)) {
            if (cold && !coldPushBasedPublisherTriggerred) {
                coldPushBasedPublisherTriggerred = true;
                pushConsumer.accept(this::push);
            }

            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                    DirectPushPublisherStrategy.this.cancel(subscriber);
                }
            });
        }
    }

    synchronized private boolean push(Signal<T> signal) {
        if (active) {
            switch (requireNonNull(signal)) {
                case Error(var throwable) -> {
                    subscribers.forEach(subscriber -> error(subscriber, throwable));
                    active = false;
                }
                case Value(var value) -> {
                    final var next = requireNonNull(value);
                    subscribers.forEach(subscriber -> subscriber.onNext(next));
                }
                case Complete() -> {
                    subscribers.forEach(this::complete);
                    active = false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
