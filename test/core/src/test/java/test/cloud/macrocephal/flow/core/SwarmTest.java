package test.cloud.macrocephal.flow.core;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Swarm;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
