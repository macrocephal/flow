package cloud.macrocephal.flow.core.publisher.strategy;

import cloud.macrocephal.flow.core.Signal;

import java.math.BigInteger;
import java.util.function.*;
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

    record Push<T>(boolean cold,
                   BigInteger capacity,
                   BackPressureStrategy backPressureStrategy,
                   Consumer<BiConsumer<Signal<T>, BackPressureFeedback>> pushConsumer) implements PublisherStrategy<T> {
        public Push {
            requireNonNull(pushConsumer);
            requireNonNull(backPressureStrategy);
        }

        public Push(boolean cold,
                    long capacity,
                    BackPressureStrategy backPressureStrategy,
                    Consumer<BiConsumer<Signal<T>, BackPressureFeedback>> pushConsumer) {
            this(cold, valueOf(capacity), backPressureStrategy, pushConsumer);
        }

        public Push(boolean cold,
                    BackPressureStrategy backPressureStrategy,
                    Consumer<BiConsumer<Signal<T>, BackPressureFeedback>> pushConsumer) {
            this(cold, defaultBufferSize(), backPressureStrategy, pushConsumer);
        }

        public Push(boolean cold, Consumer<BiConsumer<Signal<T>, BackPressureFeedback>> pushConsumer) {
            this(cold, FEEDBACK, pushConsumer);
        }
    }

    interface BackPressureFeedback {
        void resume();
        void pause();
        void stop();
    }
}
