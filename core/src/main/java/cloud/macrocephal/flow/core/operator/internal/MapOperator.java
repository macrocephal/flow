package cloud.macrocephal.flow.core.operator.internal;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.Swarm;

import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

public record MapOperator<T, U>(Function<T, U> mapper) implements Operator<T, Swarm<U>> {
    @Override
    public Swarm<U> apply(Publisher<T> operand) {
        return new Swarm<>(subscriber ->
                operand.subscribe(new SpySubscriber<T, U>((next, subscription) -> {
                    final U applied;

                    try {
                        applied = mapper().apply(next);
                    } catch (Throwable throwable) {
                        subscriber.onError(throwable);
                        subscription.cancel();
                        return;
                    }

                    subscriber.onNext(applied);
                }, subscriber)));
    }
}
