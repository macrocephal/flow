package cloud.macrocephal.flow.core.publisher;

public enum BackPressureStrategy {
    FEEDBACK,
    ERROR,
    THROW,
    DROP,
}
