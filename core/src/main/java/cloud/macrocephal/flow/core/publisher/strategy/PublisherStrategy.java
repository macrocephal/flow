package cloud.macrocephal.flow.core.publisher.strategy;

import cloud.macrocephal.flow.core.Signal;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public sealed interface PublisherStrategy<T> permits PublisherStrategy.Push, PublisherStrategy.Pull {
    record Pull<T>(BigInteger capacity,
                   LagStrategy lagStrategy,
                   Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) implements PublisherStrategy<T> {
    }

    record Push<T>(boolean hot,
                   BigInteger capacity,
                   BackPressureStrategy backPressureStrategy,
                   Consumer<Function<Signal<T>, Boolean>> pushConsumer) implements PublisherStrategy<T> {
    }
}
