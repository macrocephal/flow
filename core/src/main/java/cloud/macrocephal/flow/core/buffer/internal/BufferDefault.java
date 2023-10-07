package cloud.macrocephal.flow.core.buffer.internal;

import cloud.macrocephal.flow.core.buffer.Buffer;

import java.math.BigInteger;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.*;
import static java.util.function.Function.identity;

public class BufferDefault<T> implements Buffer<T> {
    private final AtomicLong iteratorCount = new AtomicLong(0);
    private final BigInteger capacity;
    private BigInteger size = ZERO;
    protected Node<T> first;
    protected Node<T> last;

    public BufferDefault(BigInteger capacity) {
        this.capacity = capacity;
    }

    public BufferDefault(Buffer<T> buffer) {
        this((BigInteger) null);

        for (final T value : requireNonNull(buffer)) {
            add(value);
        }
    }

    @Override
    synchronized public boolean contains(T value) {
        var node = first;

        while (nonNull(node)){
            if (Objects.equals(node.value, value)) {
                return true;
            }

            node = node.next;
        }

        return false;
    }

    @Override
    synchronized public boolean remove(T value) {
        final var iterator = iterator();

        while (iterator.hasNext()) {
            if (Objects.equals(value, iterator.next())) {
                iterator.remove();
                iterator.forEachRemaining(identity()::apply);
                return true;
            }
        }

        return false;
    }

    synchronized public BigInteger size() {
        return size;
    }

    synchronized public boolean add(T value) {
        if (isNull(capacity) || 0 < capacity.compareTo(size)) {
            if (1 < iteratorCount.get()) {
                throw new ConcurrentModificationException();
            } else if (isNull(first)) {
                first = new Node<>(null, value, null);
            } else if (isNull(last)) {
                last = new Node<>(first, value, null);
                first.next = last;
            } else {
                final var last = new Node<>(this.last, value, null);
                this.last.next = last;
                this.last = last;
            }

            size = size.add(ONE);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isEmpty() {
        return ZERO.equals(size);
    }

    @Override
    public Iterator<T> iterator() {
        iteratorCount.incrementAndGet();

        return new Iterator<>() {
            private boolean decrementedIteratorCount;
            private boolean nextMethodCalled;
            private Node<T> current;
            private boolean removed;

            {
                if (ZERO.equals(size)) {
                    tryDecrement();
                }
            }

            @Override
            public void remove() {
                if (1 < iteratorCount.get()) {
                    throw new ConcurrentModificationException();
                } else if (!nextMethodCalled) {
                    throw new IllegalStateException("Iterator's next method has not been called yet.");
                } else if (removed) {
                    throw new IllegalStateException("Iterator's remove method has already been called.");
                } else if (!isNull(current)) {
                    if (!isNull(current.previous)) {
                        current.previous.next = current.next;
                    }

                    if (!isNull(current.next)) {
                        current.next.previous = current.previous;
                    }

                    if (first == current) {
                        first = current.next;
                    }

                    if (current == last) {
                        last = null;
                    }

                    size = size.subtract(ONE);
                    removed = true;
                }
            }

            @Override
            public boolean hasNext() {
                return isNull(current) ? !nextMethodCalled && !isNull(first) : !isNull(current.next);
            }

            @Override
            public T next() {
                if (hasNext()) {
                    current = nextMethodCalled ? current.next : first;
                    nextMethodCalled = true;
                    removed = false;

                    if (!hasNext()) {
                        tryDecrement();
                    }

                    return current.value;
                } else {
                    tryDecrement();
                    throw new NoSuchElementException();
                }
            }

            private void tryDecrement() {
                if (!decrementedIteratorCount) {
                    decrementedIteratorCount = true;
                    iteratorCount.decrementAndGet();
                }
            }
        };
    }

    public static <T> Buffer<T> from(Buffer<T> source) {
        if (source instanceof BufferDefault<T> bufferDefault) {
            return new BufferDefault<>(bufferDefault.capacity) {{
                first = bufferDefault.first;
                last = bufferDefault.last;
            }};
        } else {
            return new BufferDefault<>(source);
        }
    }

    private static final class Node<T> {
        private Node<T> previous;
        private final T value;
        private Node<T> next;

        private Node(Node<T> previous, T value, Node<T> next) {
            this.previous = previous;
            this.value = value;
            this.next = next;
        }
    }
}
