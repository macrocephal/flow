package cloud.macrocephal.flow.core.exception;

import java.util.concurrent.Flow.Publisher;

public class BackPressureException extends IllegalStateException {
    private final Publisher<?> publisher;
    private final int capacity;

    public BackPressureException(Publisher<?> publisher, int capacity) {
        this.publisher = publisher;
        this.capacity = capacity;
    }

    public Publisher<?> getPublisher() {
        return publisher;
    }

    public int getCapacity() {
        return capacity;
    }
}
