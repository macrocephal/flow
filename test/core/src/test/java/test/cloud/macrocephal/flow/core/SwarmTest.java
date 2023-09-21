package test.cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Single;
import cloud.macrocephal.flow.core.Swarm;
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

public class SwarmTest {
    @Test
    public void throw_when_constructor_with_publishExposure_is_invoked_with_null() {
        assertThrows(NullPointerException.class, () -> new Swarm<>(null));
    }

    @Test
    public void call_publishExposure_when_constructor_with_publishExposure_is_invoked() {
        @SuppressWarnings("unchecked") final Consumer<Consumer<Signal<UUID>>> publishExposure = mock(Consumer.class);
        new Swarm<>(publishExposure);
        //noinspection unchecked
        verify(publishExposure).accept(any(Consumer.class));
    }

    @Test
    public void call_once_publishExposure_when_constructor_with_publishExposure_is_invoked() {
        @SuppressWarnings("unchecked") final Consumer<Consumer<Signal<UUID>>> publishExposure = mock(Consumer.class);
        new Swarm<>(publishExposure);
        //noinspection unchecked
        verify(publishExposure, times(1)).accept(any(Consumer.class));
    }

    @Test
    public void throw_when_publisher_subscribe_method_is_invoked_with_null() {
        assertThrows(NullPointerException.class, () -> new Swarm<>(Function.identity()::apply).subscribe(null));
    }

    @Test
    public void subscriber_onSubscribe_is_invoked_with_subscription_when_subscribed_to_publisher() {
        final var subscriber = mock(Flow.Subscriber.class);
        //noinspection unchecked
        new Swarm<>(Function.identity()::apply).subscribe(subscriber);
        verify(subscriber).onSubscribe(any(Flow.Subscription.class));
    }

    @Test
    public void subscriber_onSubscribe_is_invoked_with_subscription_when_subscribed_to_publisher_only_first_time() {
        final var subscriber = mock(Flow.Subscriber.class);
        final var swarm = new Swarm<>(Function.identity()::apply);
        //noinspection unchecked
        swarm.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(any(Flow.Subscription.class));
        //noinspection unchecked
        swarm.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(any(Flow.Subscription.class));
    }

    @Test
    public void calling_cancel_on_subscription_remove_the_associated_subscriber_from_publisher() {
        final var subscriber = mock(Flow.Subscriber.class);
        final var swarm = new Swarm<>(Function.identity()::apply);
        final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
        //noinspection unchecked
        swarm.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());

        final var subscription = subscriptionArgumentCaptor.getValue();
        subscription.cancel();
        //noinspection unchecked
        swarm.subscribe(subscriber);
        verify(subscriber, times(2)).onSubscribe(any(Flow.Subscription.class));
    }

    @Test
    public void reused_subscriber_receive_different_subscription() {
        final var subscriber = mock(Flow.Subscriber.class);
        final var swarm = new Swarm<>(Function.identity()::apply);
        final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
        //noinspection unchecked
        swarm.subscribe(subscriber);
        verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());

        final var subscription = subscriptionArgumentCaptor.getValue();
        subscription.cancel();
        //noinspection unchecked
        swarm.subscribe(subscriber);
        verify(subscriber, times(2)).onSubscribe(subscriptionArgumentCaptor.capture());
        assertThat(subscription).isNotEqualTo(subscriptionArgumentCaptor.getValue());
    }

    @Nested
    class PublishNullSignal {
        private Consumer<Signal<Object>> publish;

        @Test
        public void throw_NPE() {
            new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Complete<>());
            assertDoesNotThrow(() -> publish.accept(null));
        }

        @Test
        public void cause_further_calls_to_subscribe_to_be_ignored() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
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
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Error<>(mock(Throwable.class)));
            assertDoesNotThrow(() -> publish.accept(null));
        }

        @Test
        public void cause_further_calls_to_subscribe_to_be_ignored() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            publish.accept(new Signal.Error<>(mock(Throwable.class)));
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            verify(subscriberOne, times(1)).onSubscribe(any(Flow.Subscription.class));
        }
    }

    @Nested
    class PublishValueSignal {
        private Consumer<Signal<Object>> publish;

        @Test
        public void do_not_call_subscriber_onNext() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            publish.accept(new Signal.Value<>(0));
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
        }

        @Test
        public void call_subscriber_onNext_when_request_count_allow_it() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>(-1));
            //noinspection unchecked
            verify(subscriber).onNext(-1);
        }

        @Test
        public void call_subscriber_onNext_once_when_request_count_allow_it() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>(-1));
            //noinspection unchecked
            verify(subscriber, times(1)).onNext(-1);
        }

        @Test
        public void call_subscriber_onNext_once_as_much_as_request_count_allow_it() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriber);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(3);
            publish.accept(new Signal.Value<>(-1));
            publish.accept(new Signal.Value<>(+0));
            publish.accept(new Signal.Value<>(+1));
            publish.accept(new Signal.Value<>(+2));
            //noinspection unchecked
            verify(subscriber, times(3)).onNext(any());
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(-1);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+0);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+1);
        }

        @Test
        public void call_subscriber_onNext_once_for_each_subscriber_in_subscription_order_when_request_count_allows_it() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriberOne, subscriberTwo);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            verify(subscriberOne, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(1);
            verify(subscriberTwo, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>(-1));
            //noinspection unchecked
            inOrder.verify(subscriberOne, times(1)).onNext(-1);
            //noinspection unchecked
            inOrder.verify(subscriberTwo, times(1)).onNext(-1);
        }

        @Test
        public void do_not_call_subscriber_onError_even_when_request_count_allows_it() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>(-1));
            verify(subscriber, times(0)).onError(any());
        }

        @Test
        public void do_not_call_subscriber_onComplete_even_when_request_count_allows_it() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            subscriptionArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>(-1));
            verify(subscriber, times(0)).onComplete();
        }
    }

    @Nested
    class SubscriptionRequest {
        private Consumer<Signal<Object>> publish;

        @Test
        public void trigger_subscriber_onNext_if_there_were_pending_values() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            publish.accept(new Signal.Value<>(-1));
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
            subscriptionArgumentCaptor.getValue().request(1);
            //noinspection unchecked
            verify(subscriber, times(1)).onNext(-1);
        }

        @Test
        public void trigger_subscriber_onNext_if_there_were_pending_values_for_as_mush_as_possible() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriber);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            publish.accept(new Signal.Value<>(-1));
            publish.accept(new Signal.Value<>(+0));
            publish.accept(new Signal.Value<>(+1));
            publish.accept(new Signal.Value<>(+2));
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
            subscriptionArgumentCaptor.getValue().request(3);
            //noinspection unchecked
            verify(subscriber, times(3)).onNext(any());
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(-1);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+0);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+1);
        }

        @Test
        public void negative_count_does_not_matter() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            publish.accept(new Signal.Value<>(-1));
            subscriptionArgumentCaptor.getValue().request(-10);
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
        }

        @Test
        public void not_ignored_when_publisher_complete_but_there_are_pending_values() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriber);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            publish.accept(new Signal.Value<>(-1));
            publish.accept(new Signal.Value<>(+0));
            publish.accept(new Signal.Value<>(+1));
            publish.accept(new Signal.Value<>(+2));
            publish.accept(new Signal.Complete<>());
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
            subscriptionArgumentCaptor.getValue().request(4);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(-1);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+0);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+1);
            //noinspection unchecked
            inOrder.verify(subscriber, times(1)).onNext(+2);
            inOrder.verify(subscriber, times(1)).onComplete();
        }

        @Test
        public void ignored_when_subscription_is_cancelled() {
            final var subscriber = mock(Flow.Subscriber.class);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriber);
            verify(subscriber, times(1)).onSubscribe(subscriptionArgumentCaptor.capture());
            publish.accept(new Signal.Value<>(-1));
            publish.accept(new Signal.Value<>(+0));
            publish.accept(new Signal.Value<>(+1));
            publish.accept(new Signal.Value<>(+2));
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
            subscriptionArgumentCaptor.getValue().cancel();
            subscriptionArgumentCaptor.getValue().request(3);
            //noinspection unchecked
            verify(subscriber, times(0)).onNext(any());
        }
    }

    @Nested
    class Miscellaneous {
        private Consumer<Signal<Object>> publish;

        @Test
        public void publishing_error_do_not_rush_lagging_subscribers_to_error() {
            final var throwable = mock(Throwable.class);
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriberOne, subscriberTwo);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionOneArgumentCaptor = forClass(Flow.Subscription.class);
            final var subscriptionTwoArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            verify(subscriberOne).onSubscribe(subscriptionOneArgumentCaptor.capture());
            verify(subscriberTwo).onSubscribe(subscriptionTwoArgumentCaptor.capture());
            subscriptionOneArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>("Passion Fruit"));
            publish.accept(new Signal.Error<>(throwable));
            subscriptionTwoArgumentCaptor.getValue().request(1);
            //noinspection unchecked
            inOrder.verify(subscriberOne, times(1)).onNext("Passion Fruit");
            inOrder.verify(subscriberOne, times(1)).onError(throwable);
            //noinspection unchecked
            inOrder.verify(subscriberTwo, times(1)).onNext("Passion Fruit");
            inOrder.verify(subscriberTwo, times(1)).onError(throwable);
        }

        @Test
        public void publishing_complete_do_not_rush_lagging_subscribers_to_completion() {
            final var subscriberOne = mock(Flow.Subscriber.class);
            final var subscriberTwo = mock(Flow.Subscriber.class);
            final var inOrder = inOrder(subscriberOne, subscriberTwo);
            final var publisher = new Swarm<>(publish -> this.publish = publish);
            final var subscriptionOneArgumentCaptor = forClass(Flow.Subscription.class);
            final var subscriptionTwoArgumentCaptor = forClass(Flow.Subscription.class);
            //noinspection unchecked
            publisher.subscribe(subscriberOne);
            //noinspection unchecked
            publisher.subscribe(subscriberTwo);
            verify(subscriberOne).onSubscribe(subscriptionOneArgumentCaptor.capture());
            verify(subscriberTwo).onSubscribe(subscriptionTwoArgumentCaptor.capture());
            subscriptionOneArgumentCaptor.getValue().request(1);
            publish.accept(new Signal.Value<>("Passion Fruit"));
            publish.accept(new Signal.Complete<>());
            subscriptionTwoArgumentCaptor.getValue().request(1);
            //noinspection unchecked
            inOrder.verify(subscriberOne, times(1)).onNext("Passion Fruit");
            inOrder.verify(subscriberOne, times(1)).onComplete();
            //noinspection unchecked
            inOrder.verify(subscriberTwo, times(1)).onNext("Passion Fruit");
            inOrder.verify(subscriberTwo, times(1)).onComplete();
        }
    }
}
