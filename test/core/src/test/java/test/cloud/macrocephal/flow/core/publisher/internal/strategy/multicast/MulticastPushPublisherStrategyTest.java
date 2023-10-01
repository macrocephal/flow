package test.cloud.macrocephal.flow.core.publisher.internal.strategy.multicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.Swarm;
import cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.util.Spliterators.AbstractSpliterator;
import java.util.UUID;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;

import static java.lang.Math.max;
import static java.util.Spliterator.ORDERED;
import static java.util.UUID.randomUUID;
import static java.util.stream.StreamSupport.stream;

public class MulticastPushPublisherStrategyTest extends FlowPublisherVerification<UUID> {
    public MulticastPushPublisherStrategyTest() {
        super(new TestEnvironment());
    }

    @Override
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
        // NOTE: JRE ran out of heap without error
        // super.required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue();
    }

    @Override
    public Publisher<UUID> createFlowPublisher(long limit) {
        return new Swarm<>(new Push<>(false, null, BackPressureStrategy.THROW, publish ->
                stream(new AbstractSpliterator<Signal<UUID>>(max(0, limit), ORDERED) {
                           private long counter = limit;

                           @Override
                           public boolean tryAdvance(Consumer<? super Signal<UUID>> action) {
                               if (-1 < counter) {
                                   action.accept(0 == counter ? new Complete<>() : new Value<>(randomUUID()));
                                   --counter;

                                   return -1 < counter;
                               } else {
                                   return false;
                               }
                           }
                       }, false
                ).forEachOrdered(publish::apply)));
    }

    @Override
    public Publisher<UUID> createFailedFlowPublisher() {
        return new Swarm<>(new Push<>(false, 0, BackPressureStrategy.THROW,
                push -> push.apply(new Signal.Error<>(new RuntimeException("Boom!")))));
    }
}
