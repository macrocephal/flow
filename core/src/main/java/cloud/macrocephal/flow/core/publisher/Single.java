package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;

public final class Single<T> extends PublisherDefault<T> {
    public Single(Driver<T> driver) {
        super(driver);
    }
}
