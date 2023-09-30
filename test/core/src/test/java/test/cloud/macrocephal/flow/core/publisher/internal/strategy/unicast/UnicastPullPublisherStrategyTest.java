package test.cloud.macrocephal.flow.core.publisher.internal.strategy.unicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.publisher.Swarm;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.util.UUID;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;

public class UnicastPullPublisherStrategyTest extends FlowPublisherVerification<java.util.UUID> {
    public UnicastPullPublisherStrategyTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<UUID> createFlowPublisher(long l) {
        return new Swarm<>(new Pull<>(() -> n -> LongStream.range(0, n)
                .boxed().map(__ -> randomUUID()).map(Signal.Value::new)));
    }

    @Override
    public Publisher<UUID> createFailedFlowPublisher() {
        return new Swarm<>(new Pull<>(() -> n -> Stream.of(new Signal.Error<>(new RuntimeException("Boom!")))));
    }
}
