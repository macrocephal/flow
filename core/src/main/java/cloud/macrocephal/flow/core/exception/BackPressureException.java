package cloud.macrocephal.flow.core.exception;

import java.util.concurrent.Flow;

public class BackPressureException extends IllegalStateException {
    private final Flow.Publisher<?> publisher;
    private final int capacity;

    public BackPressureException(Flow.Publisher<?> publisher, int capacity) {
        this.publisher = publisher;
        this.capacity = capacity;
    }

    public Flow.Publisher<?> getPublisher() {
        return publisher;
    }

    public int getCapacity() {
        return capacity;
    }
}
