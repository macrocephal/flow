package test.cloud.macrocephal.flow.core.publisher.internal.strategy.multicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.Swarm;
import cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.LagStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.util.Spliterators.AbstractSpliterator;
import java.util.UUID;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.Spliterator.ORDERED;
import static java.util.UUID.randomUUID;
import static java.util.stream.StreamSupport.stream;

public class MulticastPullPublisherStrategyTest extends FlowPublisherVerification<UUID> {
    public MulticastPullPublisherStrategyTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<UUID> createFlowPublisher(long limit) {
        final var limit$ = Long.MAX_VALUE == limit ? null : new AtomicLong(limit);
        return new Swarm<>(new Pull<>(-1, LagStrategy.THROW, () ->
                request -> stream(new AbstractSpliterator<>(max(0, min(limit, request)), ORDERED) {
                    private long counter = isNull(limit$) ? request : min(request, limit$.get());

                    @Override
                    public boolean tryAdvance(Consumer<? super Signal<UUID>> action) {
                        if (0 < counter) {
                            ofNullable(limit$).ifPresent(limit$ -> limit$.updateAndGet(operand -> operand - 1));
                            action.accept(new Value<>(randomUUID()));
                            --counter;

                            return -1 < counter;
                        } else {
                            if (ofNullable(limit$).map(AtomicLong::get).map(((Long) 0L)::equals).orElse(false)) {
                                action.accept(new Complete<>());
                            }

                            return false;
                        }
                    }
                }, false)));
    }

    @Override
    public Publisher<UUID> createFailedFlowPublisher() {
        return new Swarm<>(new Push<>(false, 0, BackPressureStrategy.THROW,
                push -> push.apply(new Signal.Error<>(new RuntimeException("Boom!")))));
    }
}
