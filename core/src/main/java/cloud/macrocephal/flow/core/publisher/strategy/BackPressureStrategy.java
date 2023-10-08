package cloud.macrocephal.flow.core.publisher.strategy;

public enum BackPressureStrategy {
    PAUSE,
    ERROR,
    THROW,
    DROP,
    STOP,
}
