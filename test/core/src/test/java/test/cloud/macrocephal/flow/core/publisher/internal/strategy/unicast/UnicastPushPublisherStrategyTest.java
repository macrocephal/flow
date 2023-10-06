package test.cloud.macrocephal.flow.core.publisher.internal.strategy.unicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.Swarm;
import cloud.macrocephal.flow.core.publisher.strategy.BackPressureStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.BackPressureFeedback;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Push;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.UUID.randomUUID;

public class UnicastPushPublisherStrategyTest extends FlowPublisherVerification<UUID> {
    public UnicastPushPublisherStrategyTest() {
        super(new TestEnvironment());
    }

    @BeforeMethod
    void beforeMethod(Method method) {
        System.err.println(">>> " + getClass() + '#' + method.getName());
    }

    @Override
    public Publisher<UUID> createFlowPublisher(long limit) {
        return new Swarm<>(new Push<>(true, 0L, BackPressureStrategy.FEEDBACK, target -> {
            final var paused = new AtomicBoolean();
            final var stopped = new AtomicBoolean();
            final var theLimit = new AtomicLong(limit);
            final var backPressureFeedback = new BackPressureFeedback() {
                @Override
                public void resume() {
                    paused.set(false);
                }

                @Override
                public void pause() {
                    if (!paused.get()) {
                        theLimit.updateAndGet(value -> value + 1);
                        paused.set(true);
                    }
                }

                @Override
                public void stop() {
                    stopped.set(true);
                }
            };
            new Thread(() -> {
                while (!stopped.get() && 0 < theLimit.get()) {
                    if (!paused.get() && 0 < theLimit.getAndUpdate(value -> value - 1)) {
                        target.accept(new Value<>(randomUUID()), backPressureFeedback);
                    }
                }

                if (!stopped.get()) {
                    target.accept(new Signal.Complete<>(), backPressureFeedback);
                }
            }).start();
        }));
    }

    @Override
    public Publisher<UUID> createFailedFlowPublisher() {
        return new Swarm<>(new Push<>(false, 0L, BackPressureStrategy.FEEDBACK, target -> {
            target.accept(new Signal.Error<>(new RuntimeException("Boom!")), null);
        }));
    }
}
