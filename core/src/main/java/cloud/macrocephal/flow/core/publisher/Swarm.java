package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.Iteration3BasePublisher;

public final class Swarm<T> extends Iteration3BasePublisher<T> {
    public Swarm(Driver<T> driver) {
        super(driver);
    }
}
