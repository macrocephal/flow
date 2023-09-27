package cloud.macrocephal.flow.core.operator.internal;

import cloud.macrocephal.flow.core.buffer.Buffer;
import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.publisher.Single;

import java.math.BigInteger;
import java.util.concurrent.Flow.Publisher;

import static java.util.Objects.requireNonNull;

public record NthLastOperator<T>(BigInteger nthLast) implements Operator<T, Single<T>> {
    public NthLastOperator {
        requireNonNull(nthLast);
    }

    @Override
    public Single<T> apply(Publisher<T> operand) {
        return new Single<>(subscriber -> {
            final var buffer = Buffer.of(nthLast());
            new SpySubscriber<T, T>((next, subscription) -> {
                if (!buffer.add(next)) {
                    final var iterator = buffer.iterator();
                    iterator.next();
                    try {
                        iterator.remove();
                        buffer.add(next);
                    } catch (Throwable ignored) {
                    }
                }
            }, subscriber) {
                @Override
                public void onComplete() {
                    if (nthLast().equals(buffer.size())) {
                        final var iterator = buffer.iterator();

                        if (iterator.hasNext()) {
                            //noinspection unchecked
                            subscriber.onNext((T) iterator.next());
                        }
                    }
                    super.onComplete();
                }
            };
        });
    }
}
