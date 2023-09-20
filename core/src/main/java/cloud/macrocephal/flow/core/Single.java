package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.internal.BasePublisher;

import java.util.function.Consumer;

public final class Single<T> extends BasePublisher<T> {
    public Single(Consumer<Consumer<Signal<T>>> publishExposure) {
        super(publishExposure);
    }
}
