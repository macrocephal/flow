package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.Iteration3BasePublisher;

public final class Single<T> extends Iteration3BasePublisher<T> {
    public Single(Driver<T> driver) {
        super(driver);
    }
}
