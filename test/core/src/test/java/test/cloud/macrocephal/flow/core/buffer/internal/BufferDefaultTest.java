package test.cloud.macrocephal.flow.core.buffer.internal;

import cloud.macrocephal.flow.core.buffer.Buffer;
import org.testng.annotations.Test;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Optional;

import static java.math.BigInteger.*;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertThrows;

public class BufferDefaultTest {
    @Test
    void is_instance_of_buffer() {
        assertThat(Buffer.of()).isInstanceOf(Buffer.class);
        assertThat(Buffer.of(ONE)).isInstanceOf(Buffer.class);
        assertThat(Buffer.of(null)).isInstanceOf(Buffer.class);
    }

    @Test
    void size_start_at_zero() {
        assertThat(Buffer.of()).hasSize(0);
        assertThat(Buffer.of(ONE)).hasSize(0);
        assertThat(Buffer.of(null)).hasSize(0);
    }

    @Test
    void add_increase_size_one() {
        of(Buffer.of(), Buffer.of(ONE), Buffer.of(null)).forEach(buffer -> {
            buffer.add(null);
            assertThat(buffer).hasSize(1);
        });
    }

    @Test
    void add_return_true_when_successful() {
        of(Buffer.of(), Buffer.of(ONE), Buffer.of(null)).forEach(buffer -> {
            assertThat(buffer.add(null)).isTrue();
        });
    }

    @Test
    void add_return_false_when_capacity_is_reached() {
        assertThat(Buffer.of(ZERO).add(null)).isFalse();
    }

    @Test
    void iterator_reflect_addition_order() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            assertThat(buffer).containsExactly(-3, -2, -1, -0, 1, 2, 3);
        });
    }

    @Test
    void iterator_remove_reflect_on_buffer_iterable() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            final var iterator = buffer.iterator();
            iterator.next();
            iterator.remove();
            assertThat(buffer).containsExactly(-2, -1, 0, 1, 2, 3);
        });
    }

    @Test
    void iterator_next_throw_NoSuchElementException_when_called_after_last_element() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            final var iterator = buffer.iterator();
            iterator.forEachRemaining(identity()::apply);
            assertThrows(NoSuchElementException.class, iterator::next);
        });
    }

    @Test
    void iterator_remove_throw_ConcurrentModificationException_when_there_are_other_active_iterators() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            final var iterator = buffer.iterator();
            buffer.iterator();
            iterator.next();
            assertThrows(ConcurrentModificationException.class, iterator::remove);
        });
    }

    @Test
    void iterator_remove_throw_IllegalStateException_when_next_method_has_not_been_called_yet() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            final var iterator = buffer.iterator();
            assertThrows(IllegalStateException.class, iterator::remove);
        });
    }

    @Test
    void iterator_remove_throw_IllegalStateException_when_called_more_than_once_on_the_same_element() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            final var iterator = buffer.iterator();
            iterator.next();
            iterator.remove();
            assertThrows(IllegalStateException.class, iterator::remove);
            assertThrows(IllegalStateException.class, iterator::remove);
            assertThrows(IllegalStateException.class, iterator::remove);
        });
    }

    @Test
    void contains_return_true_if_reference_exists_in_buffer() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(new Object[0], empty(), Optional.empty()).forEachOrdered(buffer::add);
            assertThat(buffer.contains(Optional.empty())).isTrue();
        });
    }

    @Test
    void contains_return_true_if_equals_entry_exists_in_buffer() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(new Object[0], empty(), "A").forEachOrdered(buffer::add);
            assertThat(buffer.contains("A")).isTrue();
        });
    }

    @Test
    void contains_return_true_if_no_entry_exists_with_same_reference_or_equals_matched_in_buffer() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            of(new Object[0], empty(), "A").forEachOrdered(buffer::add);
            assertThat(buffer.contains(30)).isFalse();
        });
    }

    @Test
    void remove_remove_first_matching_occurrence_from_buffer() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            final var EMPTY_STREAM = empty();
            //noinspection StringOperationCanBeSimplified
            of(new Object[0], EMPTY_STREAM, new String("A"), "A").forEachOrdered(buffer::add);
            buffer.remove("A");
            assertThat(buffer).containsExactly(new Object[0], EMPTY_STREAM, "A");
        });
    }

    @Test
    void remove_return_true_when_successfully_removed() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            final var EMPTY_STREAM = empty();
            //noinspection StringOperationCanBeSimplified
            of(new Object[0], EMPTY_STREAM, "A", new String("A")).forEachOrdered(buffer::add);
            assertThat(buffer.remove("A")).isTrue();
        });
    }

    @Test
    void remove_return_false_when_none_removed() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            final var EMPTY_STREAM = empty();
            //noinspection StringOperationCanBeSimplified
            of(new Object[0], EMPTY_STREAM, new String("A"), "A").forEachOrdered(buffer::add);
            assertThat(buffer.remove(0)).isFalse();
        });
    }

    @Test
    void isEmpty_return_true_when_buffer_contains_no_value() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null))
                .forEach(buffer -> assertThat(buffer.isEmpty()).isTrue());
    }

    @Test
    void isEmpty_return_false_when_buffer_is_empty() {
        of(Buffer.of(), Buffer.of(valueOf(7)), Buffer.of(null)).forEach(buffer -> {
            final var EMPTY_STREAM = empty();
            //noinspection StringOperationCanBeSimplified
            of(new Object[0], EMPTY_STREAM, "A", new String("A")).forEachOrdered(buffer::add);
            assertThat(buffer.isEmpty()).isFalse();
        });
    }

    @Test
    void iterator_count_is_eagerly_decremented() {
        of(Buffer.of(), Buffer.of(valueOf(8)), Buffer.of(null)).forEach(buffer -> {
            of(-3, -2, -1, 0, 1, 2, 3).forEachOrdered(buffer::add);
            buffer.iterator().forEachRemaining(identity()::apply);
            assertThat(buffer.add(null)).isTrue();
        });
    }
}
