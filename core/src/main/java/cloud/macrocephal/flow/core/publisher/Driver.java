package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.Signal;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public sealed interface Driver<T> permits Driver.Push, Driver.Pull {
    record Pull<T>(int capacity,
                   LagStrategy lagStrategy,
                   Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory) implements Driver<T> {
    }

    record Push<T>(boolean hot,
                   int capacity,
                   BackPressureStrategy backPressureStrategy,
                   Consumer<Function<Signal<T>, Boolean>> pushConsumer) implements Driver<T> {
    }
}
