package cloud.macrocephal.flow.core.publisher.v2.strategy;

public sealed interface DispatchStrategy permits DispatchStrategy.Direct, DispatchStrategy.Sharing {
    record Sharing(int capacity) implements DispatchStrategy {
    }

    record Direct() implements DispatchStrategy {
    }
}
