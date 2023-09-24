package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.internal.BasePublisher;

import java.util.function.Consumer;

public final class OldSwarm<T> extends BasePublisher<T> {
    public OldSwarm(Consumer<Consumer<Signal<T>>> publishExposure) {
        super(publishExposure);
    }

    public OldSwarm(Consumer<Consumer<Signal<T>>> publishExposure, int capacity) {
        super(publishExposure, capacity);
    }
}