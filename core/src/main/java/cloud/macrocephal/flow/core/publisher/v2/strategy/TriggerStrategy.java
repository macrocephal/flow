package cloud.macrocephal.flow.core.publisher.v2.strategy;

public sealed interface TriggerStrategy permits TriggerStrategy.Hot, TriggerStrategy.Cold {
    record Cold() implements TriggerStrategy {
    }

    record Hot() implements TriggerStrategy {
    }
}
