package cloud.macrocephal.flow.core.buffer;

import cloud.macrocephal.flow.core.buffer.internal.BufferDefault;

import java.math.BigInteger;

public interface Buffer<T> extends Iterable<T> {
    boolean add(T value);

    BigInteger size();

    static <T> Buffer<T> of(BigInteger capacity) {
        //noinspection RedundantCast,unchecked
        return (Buffer<T>) new BufferDefault<>(capacity);
    }

    static <T> Buffer<T> of() {
        return of(null);
    }
}
