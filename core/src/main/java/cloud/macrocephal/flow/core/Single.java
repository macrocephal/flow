package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.internal.BasePublisher;

import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

public final class Single<T> extends BasePublisher<T> {
    public Single(Consumer<Consumer<Signal<T>>> publishExposure) {
        super(decoratePublisher(publishExposure));
    }

    public Single(Consumer<Consumer<Signal<T>>> publishExposure, int capacity) {
        super(decoratePublisher(publishExposure), capacity);
    }

    private static <T> Consumer<Consumer<Signal<T>>> decoratePublisher(Consumer<Consumer<Signal<T>>> publishExposure) {
        return ofNullable(publishExposure)
                .map(ignored -> (Consumer<Consumer<Signal<T>>>) (signalConsumer -> {
                    publishExposure.accept(signal -> {
                        signalConsumer.accept(signal);
                        if (signal instanceof Signal.Value<T>) {
                            signalConsumer.accept(new Signal.Complete<>());
                        }
                    });
                }))
                .orElse(null);
    }
}
