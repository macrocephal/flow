package cloud.macrocephal.flow.core.buffer;

import cloud.macrocephal.flow.core.buffer.internal.BufferDefault;

import java.math.BigInteger;

public interface Buffer<T> extends Iterable<T> {
    boolean contains(T value);

    boolean remove(T value);

    boolean add(T value);

    BigInteger size();

    boolean isEmpty();

    static <T> Buffer<T> from(Buffer<T> buffer) {
        //noinspection RedundantCast
        return (Buffer<T>) BufferDefault.from(buffer);
    }

    static <T> Buffer<T> of(BigInteger capacity) {
        //noinspection RedundantCast,unchecked
        return (Buffer<T>) new BufferDefault<>(capacity);
    }

    static <T> Buffer<T> of() {
        return of(null);
    }
}
