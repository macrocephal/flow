package test.cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Single;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SingleTest {
    @Test
    public void throw_when_constructor_with_publishExposure_is_invoked_with_null() {
        assertThrows(NullPointerException.class, () -> new Single<>(null));
    }

    @Test
    public void call_publishExposure_when_constructor_with_publishExposure_is_invoked() {
        @SuppressWarnings("unchecked") final Consumer<Consumer<Signal<UUID>>> publishExposure = mock(Consumer.class);
        new Single<>(publishExposure);
        //noinspection unchecked
        verify(publishExposure).accept(any(Consumer.class));
    }

    @Test
    public void call_once_publishExposure_when_constructor_with_publishExposure_is_invoked() {
        @SuppressWarnings("unchecked") final Consumer<Consumer<Signal<UUID>>> publishExposure = mock(Consumer.class);
        new Single<>(publishExposure);
        //noinspection unchecked
        verify(publishExposure, times(1)).accept(any(Consumer.class));
    }

    @Test
    public void throw_when_publisher_subscribe_method_is_invoked_with_null() {
        assertThrows(NullPointerException.class, () -> new Single<>(Function.identity()::apply).subscribe(null));
    }

    @Test
    public void subscriber_onSubscribe_is_invoked_with_subscription_when_subscribed_to_publisher() {
        final var subscriber = mock(Flow.Subscriber.class);
        //noinspection unchecked
        new Single<>(Function.identity()::apply).subscribe(subscriber);
        verify(subscriber).onSubscribe(any(Flow.Subscription.class));
    }

    @Test
    public void subscriber_onSubscribe_is_invoked_with_subscription_when_subscribed_to_publisher_only_first_time() {
        final var subscriber = mock(Flow.Subscriber.class);
        final var single = new Single<>(Function.identity()::apply);
        //noinspection unchecked
        single.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(any(Flow.Subscription.class));
        //noinspection unchecked
        single.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(any(Flow.Subscription.class));
    }

    @Test
    public void calling_cancel_on_subscription_remove_the_associated_subscriber_from_publisher() {
        final var subscriber = mock(Flow.Subscriber.class);
        final var single = new Single<>(Function.identity()::apply);
        final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
        //noinspection unchecked
        single.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());

        final var subscription = subscriptionArgumentCaptor.getValue();
        subscription.cancel();
        //noinspection unchecked
        single.subscribe(subscriber);
        verify(subscriber, times(2)).onSubscribe(any(Flow.Subscription.class));
    }

    @Test
    public void reused_subscriber_receive_different_subscription() {
        final var subscriber = mock(Flow.Subscriber.class);
        final var single = new Single<>(Function.identity()::apply);
        final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
        //noinspection unchecked
        single.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());

        final var subscription = subscriptionArgumentCaptor.getValue();
        subscription.cancel();
        //noinspection unchecked
        single.subscribe(subscriber);
        verify(subscriber, times(2)).onSubscribe(subscriptionArgumentCaptor.capture());
        assertThat(subscription).isNotEqualTo(subscriptionArgumentCaptor.getValue());
    }

    @Nested
    class PublishNullSignal {
        private Consumer<Signal<Object>> publish;

        @Test
        public void throw_NPE() {
            new Single<>(publish -> this.publish = publish);
            assertThrows(NullPointerException.class, () -> publish.accept(null));
        }
    }

    @Nested
    class PublishCompleteSignal {
        private Consumer<Signal<Object>> publish;

        @Test
        public void run_onComplete_for_each_subscriber() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            publish.accept(new Signal.Complete<>());
            verify(subscriberOne).onComplete();
            verify(subscriberTwo).onComplete();
        }

        @Test
        public void run_onComplete_once_for_each_subscriber() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            publish.accept(new Signal.Complete<>());
            verify(subscriberOne, times(1)).onComplete();
            verify(subscriberTwo, times(1)).onComplete();
        }

        @Test
        public void run_onComplete_once_for_each_subscriber_in_subscription() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriberOne, subscriberTwo);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            publish.accept(new Signal.Complete<>());
            inOrder.verify(subscriberOne, times(1)).onComplete();
            inOrder.verify(subscriberTwo, times(1)).onComplete();
        }

        @Test
        public void cause_further_calls_to_publish_to_be_ignored() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Complete<>());
            assertDoesNotThrow(() -> publish.accept(null));
        }

        @Test
        public void cause_further_calls_to_subscribe_to_be_ignored() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Complete<>());
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            verify(subscriberOne, times(1)).onSubscribe(any(Flow.Subscription.class));
        }
    }

    @Nested
    class PublishErrorSignal {
        private Consumer<Signal<Object>> publish;

        @Test
        public void run_onError_for_each_subscriber() {
            final var throwable = mock(Throwable.class);
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            publish.accept(new Signal.Error<>(throwable));
            verify(subscriberOne).onError(throwable);
            verify(subscriberTwo).onError(throwable);
        }

        @Test
        public void run_onError_once_for_each_subscriber() {
            final var throwable = mock(Throwable.class);
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            publish.accept(new Signal.Error<>(throwable));
            verify(subscriberOne, times(1)).onError(throwable);
            verify(subscriberTwo, times(1)).onError(throwable);
        }

        @Test
        public void run_onError_once_for_each_subscriber_in_subscription() {
            final var throwable = mock(Throwable.class);
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriberOne, subscriberTwo);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            publish.accept(new Signal.Error<>(throwable));
            inOrder.verify(subscriberOne, times(1)).onError(throwable);
            inOrder.verify(subscriberTwo, times(1)).onError(throwable);
        }

        @Test
        public void cause_further_calls_to_publish_to_be_ignored() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Error<>(mock(Throwable.class)));
            assertDoesNotThrow(() -> publish.accept(null));
        }

        @Test
        public void cause_further_calls_to_subscribe_to_be_ignored() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var publisher = new Single<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Error<>(mock(Throwable.class)));
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            verify(subscriberOne, times(1)).onSubscribe(any(Flow.Subscription.class));
        }
    }
}
