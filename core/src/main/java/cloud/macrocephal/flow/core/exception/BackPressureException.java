package cloud.macrocephal.flow.core.exception;

import java.math.BigInteger;
import java.util.concurrent.Flow.Publisher;

public class BackPressureException extends IllegalStateException {
    private final Publisher<?> publisher;
    private final BigInteger capacity;

    public BackPressureException(Publisher<?> publisher, BigInteger capacity) {
        this.publisher = publisher;
        this.capacity = capacity;
    }

    public Publisher<?> getPublisher() {
        return publisher;
    }

    public BigInteger getCapacity() {
        return capacity;
    }
}
