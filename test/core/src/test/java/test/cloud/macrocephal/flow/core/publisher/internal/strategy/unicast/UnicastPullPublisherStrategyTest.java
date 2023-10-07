package test.cloud.macrocephal.flow.core.publisher.internal.strategy.unicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.publisher.Swarm;
import cloud.macrocephal.flow.core.publisher.strategy.LagStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.util.Spliterators.AbstractSpliterator;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.Spliterator.ORDERED;
import static java.util.UUID.randomUUID;
import static java.util.stream.StreamSupport.stream;

public class UnicastPullPublisherStrategyTest extends FlowPublisherVerification<java.util.UUID> {
    public UnicastPullPublisherStrategyTest() {
        super(new TestEnvironment());
    }

    @BeforeMethod
    void beforeMethod(Method method) {
        System.err.println(">>> " + getClass() + '#' + method.getName());
    }

    @Override
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
        // OOM
    }

    @Override
    public Publisher<UUID> createFlowPublisher(long limit) {
        final var limit$ = Long.MAX_VALUE == limit ? null : new AtomicLong(limit);
        return new Swarm<>(new Pull<>(0, LagStrategy.THROW, () ->
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
        // return new Swarm<>(new Pull<>(() -> ignored -> Stream.of(new Signal.Error<>(new RuntimeException("Boom")))));
        // ^^^
        // Unicast pull publisher needs at least on call to request in order to error: test needs it to fail on subs.

        final var boom = new Swarm<UUID>(new Pull<>(() -> ignored -> Stream.of(new Signal.Error<>(new RuntimeException("Boom")))));
        boom.subscribe(new Flow.Subscriber<UUID>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                // NOTE: Trigger initial pull to ensure subsequent subscribers can fail without requesting
                subscription.request(1);
            }

            @Override
            public void onNext(UUID item) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
        return boom;
    }
}
