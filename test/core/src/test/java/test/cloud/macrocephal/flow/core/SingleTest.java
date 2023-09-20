package test.cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Single;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
