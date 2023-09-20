package cloud.macrocephal.flow.core;

public sealed interface Signal<T> permits Signal.Error, Signal.Value, Signal.Complete {
    record Complete<T>() implements Signal<T> {
    }

    record Value<T>(T value) implements Signal<T> {
    }

    record Error<T>(Throwable throwable) implements Signal<T> {
    }
}
