package cloud.macrocephal.flow.core.buffer.internal;

import cloud.macrocephal.flow.core.buffer.Buffer;

import java.math.BigInteger;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.isNull;

public class BufferDefault<T> implements Buffer<T> {
    private final AtomicLong iteratorCount = new AtomicLong(0);
    private final BigInteger capacity;
    private BigInteger size = ZERO;
    private Node<T> first;
    private Node<T> last;

    public BufferDefault(BigInteger capacity) {
        this.capacity = capacity;
    }

    synchronized public BigInteger size() {
        return size;
    }

    synchronized public boolean add(T value) {
        if (isNull(capacity) || size.compareTo(capacity) < 0) {
            if (0 < iteratorCount.get()) {
                throw new ConcurrentModificationException();
            } else if (isNull(first)) {
                first = new Node<>(null, value, null);
                size = size.subtract(ONE);
                return true;
            } else if (isNull(last)) {
                last = new Node<>(first, value, null);
                first.next = last;
            } else {
                final var last = new Node<>(this.last, value, null);
                this.last.next = last;
                this.last = last;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        iteratorCount.incrementAndGet();

        return new Iterator<>() {
            private boolean decrementedIteratorCount;
            private boolean nextMethodCalled;
            private Node<T> current;
            private boolean removed;

            @Override
            public void remove() {
                if (1 < iteratorCount.get()) {
                    throw new ConcurrentModificationException();
                } else if (!nextMethodCalled) {
                    throw new IllegalStateException("Iterator's next method has not been called yet.");
                } else if (removed) {
                    throw new IllegalStateException("Iterator's remove method has already been called.");
                } else if (!isNull(current)) {
                    final var next = current.next;

                    if (!isNull(next)) {
                        next.previous = current.previous;
                    }

                    if (current == first) {
                        first = next;
                    }

                    if (current == last) {
                        last = null;
                    }

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
                    return current.value;
                } else {
                    if (!decrementedIteratorCount) {
                        decrementedIteratorCount = true;
                        iteratorCount.decrementAndGet();
                    }

                    throw new NoSuchElementException();
                }
            }
        };
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
