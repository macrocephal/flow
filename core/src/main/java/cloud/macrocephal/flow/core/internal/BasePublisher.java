package cloud.macrocephal.flow.core.internal;

import java.util.concurrent.Flow;

public class BasePublisher<T> implements Flow.Publisher<T> {
    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
    }
}
