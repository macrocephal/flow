package cloud.macrocephal.flow.core.publisher.strategy;

import cloud.macrocephal.flow.core.Signal;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy.FEEDBACK;
import static cloud.macrocephal.flow.core.publisher.strategy.LagStrategy.ERROR;
import static java.math.BigInteger.valueOf;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Flow.defaultBufferSize;

@SuppressWarnings("unused")
public sealed interface PublisherStrategy<T> permits PublisherStrategy.Push, PublisherStrategy.Pull {
    record Pull<T>(BigInteger capacity,
                   LagStrategy lagStrategy,
                   Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) implements PublisherStrategy<T> {
        public Pull {
            requireNonNull(lagStrategy);
            requireNonNull(pullerFactory);
        }

        public Pull(long capacity,
                    LagStrategy lagStrategy,
                    Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) {
            this(valueOf(capacity), lagStrategy, pullerFactory);
        }

        public Pull(LagStrategy lagStrategy,
                    Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) {
            this(defaultBufferSize(), lagStrategy, pullerFactory);
        }

        public Pull(Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) {
            this(ERROR, pullerFactory);
        }
    }

    record Push<T>(boolean hot,
                   BigInteger capacity,
                   BackPressureStrategy backPressureStrategy,
                   Consumer<Function<Signal<T>, Boolean>> pushConsumer) implements PublisherStrategy<T> {
        public Push {
            requireNonNull(pushConsumer);
            requireNonNull(backPressureStrategy);
        }

        public Push(boolean hot,
                    long capacity,
                    BackPressureStrategy backPressureStrategy,
                    Consumer<Function<Signal<T>, Boolean>> pushConsumer) {
            this(hot, valueOf(capacity), backPressureStrategy, pushConsumer);
        }

        public Push(boolean hot,
                    BackPressureStrategy backPressureStrategy,
                    Consumer<Function<Signal<T>, Boolean>> pushConsumer) {
            this(hot, defaultBufferSize(), backPressureStrategy, pushConsumer);
        }

        public Push(boolean hot, Consumer<Function<Signal<T>, Boolean>> pushConsumer) {
            this(hot, FEEDBACK, pushConsumer);
        }
    }
}
