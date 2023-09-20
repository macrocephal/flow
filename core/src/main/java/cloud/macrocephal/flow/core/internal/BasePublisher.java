package cloud.macrocephal.flow.core.internal;

import cloud.macrocephal.flow.core.Signal;

import java.util.concurrent.Flow;
import java.util.function.Consumer;
import static java.util.Objects.requireNonNull;

public class BasePublisher<T> implements Flow.Publisher<T> {
    protected BasePublisher(Consumer<Consumer<Signal<T>>> publishExposure) {
        requireNonNull(publishExposure);
        publishExposure.accept(this::publish);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
    }

    synchronized private void publish(Signal<T> signal) {
    }
}
