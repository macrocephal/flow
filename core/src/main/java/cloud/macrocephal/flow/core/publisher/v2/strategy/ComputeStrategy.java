package cloud.macrocephal.flow.core.publisher.v2.strategy;

import cloud.macrocephal.flow.core.Signal;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface ComputeStrategy<T>
        permits ComputeStrategy.PullSharing, ComputeStrategy.Pull, ComputeStrategy.Push {
    record Pull<T>(Supplier<Function<Long, List<Signal<T>>>> producerFactory) implements ComputeStrategy<T> {
    }

    record PullSharing<T>(Function<Long, List<Signal<T>>> producer) implements ComputeStrategy<T> {
    }

    record Push<T>(Consumer<Consumer<Signal<T>>> pushConsumer) implements ComputeStrategy<T> {
    }
}
