package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;

public final class Swarm<T> extends PublisherDefault<T> {
    public Swarm(Driver<T> driver) {
        super(driver);
    }
}
