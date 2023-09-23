package cloud.macrocephal.flow.core.publisher.strategy;

public sealed interface SharingStrategy permits SharingStrategy.HotSharing, SharingStrategy.ColdSharing {
    record ColdSharing() implements SharingStrategy {
    }

    record HotSharing() implements SharingStrategy {
    }
}
