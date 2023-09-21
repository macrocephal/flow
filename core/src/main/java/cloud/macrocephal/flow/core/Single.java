package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.internal.BasePublisher;

import java.util.function.Consumer;

public final class Single<T> extends BasePublisher<T> {
    public Single(Consumer<Consumer<Signal<T>>> publishExposure) {
        super(publishExposure);
    }

    public Single(Consumer<Consumer<Signal<T>>> publishExposure, int capacity) {
        super(publishExposure, capacity);
    }
}
