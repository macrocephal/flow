package test.cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Swarm;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
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
}
