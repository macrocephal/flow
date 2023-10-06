package cloud.macrocephal.flow.core.publisher.strategy;

public enum BackPressureStrategy {
    FEEDBACK,
    ERROR,
    THROW,
    DROP,
    STOP,
}
