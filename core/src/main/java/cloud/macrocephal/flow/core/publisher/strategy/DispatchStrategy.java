package cloud.macrocephal.flow.core.publisher.strategy;

public sealed interface DispatchStrategy permits DispatchStrategy.PullDispatch, DispatchStrategy.PushDispatch {
    record PullDispatch() implements DispatchStrategy {
    }

    record PushDispatch() implements DispatchStrategy {
    }
}
