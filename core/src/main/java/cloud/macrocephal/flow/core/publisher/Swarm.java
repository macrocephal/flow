package cloud.macrocephal.flow.core.publisher;

import java.util.concurrent.Flow;

public final class Swarm<T> implements Flow.Publisher<T> {
    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
    }
}
