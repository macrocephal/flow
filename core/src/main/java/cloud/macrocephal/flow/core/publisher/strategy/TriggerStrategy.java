package cloud.macrocephal.flow.core.publisher.strategy;

public sealed interface TriggerStrategy permits TriggerStrategy.HotTrigger, TriggerStrategy.ColdTrigger {
    record ColdTrigger() implements TriggerStrategy {
    }

    record HotTrigger() implements TriggerStrategy {
    }
}
