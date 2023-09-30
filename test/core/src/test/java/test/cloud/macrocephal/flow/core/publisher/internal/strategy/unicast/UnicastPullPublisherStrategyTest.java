package test.cloud.macrocephal.flow.core.publisher.internal.strategy.unicast;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.util.UUID;
import java.util.concurrent.Flow.Publisher;

public class UnicastPullPublisherStrategyTest extends FlowPublisherVerification<UUID> {
    public UnicastPullPublisherStrategyTest(TestEnvironment env, long publisherReferenceGCTimeoutMillis) {
        super(env, publisherReferenceGCTimeoutMillis);
    }

    @Override
    public Publisher<UUID> createFlowPublisher(long l) {
        return null;
    }

    @Override
    public Publisher<UUID> createFailedFlowPublisher() {
        return null;
    }
}
