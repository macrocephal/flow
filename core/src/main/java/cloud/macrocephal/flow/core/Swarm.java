package cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.internal.BasePublisher;

import java.util.function.Consumer;

public final class Swarm<T> extends BasePublisher<T> {
    public Swarm(Consumer<Consumer<Signal<T>>> publishExposure) {
        super(publishExposure);
    }
}
