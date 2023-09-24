package cloud.macrocephal.flow.core.exception;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.util.Objects.requireNonNull;

public class LagException extends IllegalStateException {
    private final Subscriber<?> subscriber;
    private final Publisher<?> publisher;

    public LagException(Subscriber<?> subscriber, Publisher<?> publisher) {
        this.publisher = requireNonNull(publisher);
        this.subscriber = subscriber;
    }

    public Subscriber<?> getSubscriber() {
        return subscriber;
    }

    public Publisher<?> getPublisher() {
        return publisher;
    }
}
