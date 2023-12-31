package cloud.macrocephal.flow.core.publisher.internal.strategy.multicast;

import cloud.macrocephal.flow.core.Signal;
import cloud.macrocephal.flow.core.Signal.Complete;
import cloud.macrocephal.flow.core.Signal.Error;
import cloud.macrocephal.flow.core.Signal.Value;
import cloud.macrocephal.flow.core.exception.LagException;
import cloud.macrocephal.flow.core.publisher.internal.strategy.Spec303Subscription;
import cloud.macrocephal.flow.core.publisher.strategy.LagStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy.Pull;

import java.util.Iterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static cloud.macrocephal.flow.core.buffer.Buffer.from;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.max;
import static java.math.BigInteger.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.Spliterator.ORDERED;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

public class MulticastPullPublisherStrategy<T> extends BaseMulticastPublisherStrategy<T> {
    private final Supplier<LongFunction<Stream<Signal<T>>>> pullerFactory;
    private LongFunction<Stream<Signal<T>>> puller;
    private final LagStrategy lagStrategy;

    public MulticastPullPublisherStrategy(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
        //noinspection PatternVariableHidesField
        if (publisherStrategy instanceof Pull(
                final var capacity,
                final var lagStrategy,
                final var pullerFactory
        ) && (isNull(capacity) || 0 < capacity.compareTo(ZERO))) {
            this.pullerFactory = pullerFactory;
            this.lagStrategy = lagStrategy;
        } else {
            throw new IllegalArgumentException("%s not accepted here.".formatted(publisherStrategy));
        }
    }

    @Override
    synchronized public void subscribe(Subscriber<? super T> subscriber) {
        if (!subscribers.contains(subscriber) && subscribers.add(subscriber)) {
            subscriber.onSubscribe(new Spec303Subscription<T>(
                    subscriber,
                    MulticastPullPublisherStrategy.this::cancel,
                    n -> MulticastPullPublisherStrategy.this.request(subscriber, n)));

            tryTerminate(subscriber);
        }
    }

    synchronized private void request(Subscriber<? super T> subscriber, long n) {
        final var counter$ = new long[]{max(0, n)};

        if (active && subscribers.contains(subscriber) && !tryTerminate(subscriber)) {
            final var fromEntries = getFromEntries(subscriber, counter$);

            if (0 < counter$[0]) {
                ofNullable(this.puller).orElseGet(() -> this.puller = requireNonNull(pullerFactory.get()));
                final var fromPuller = getFromQueryWhileUpdatingEntries(counter$);
                concat(fromEntries, fromPuller).forEachOrdered(subscriber::onNext);
            } else {
                fromEntries.forEachOrdered(subscriber::onNext);
            }

            tryTerminate(subscriber);
        }
    }

    private Stream<T> getFromEntries(Subscriber<? super T> subscriber, long[] counter$) {
        return stream(new AbstractSpliterator<>(MAX_VALUE, ORDERED) {
            final Iterator<Entry<T>> iterator = entries.iterator();

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                //noinspection PatternVariableHidesField
                if (0 < counter$[0] &&
                        iterator.hasNext() &&
                        iterator.next() instanceof Entry<T>(var value, var subscribers)
                        && subscribers.remove(subscriber)) {
                    if (subscribers.isEmpty()) {
                        subscribers.remove(subscriber);
                    }

                    action.accept(value);
                    return true;
                } else {
                    return false;
                }
            }
        }, false);
    }

    private Stream<T> getFromQueryWhileUpdatingEntries(long[] counter$) {
        final var self = this;
        return stream(new AbstractSpliterator<>(MAX_VALUE, ORDERED) {
            final Iterator<Signal<T>> iterator = requireNonNull(puller.apply(counter$[0])).iterator();

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (0 < counter$[0] && iterator.hasNext()) {
                    switch (requireNonNull(iterator.next())) {
                        case Value(var value) when 0 < counter$[0] -> {
                            final var next = requireNonNull(value);

                            --counter$[0];
                            action.accept(next);
                            if (isBufferFullCapacity()) {
                                entries.add(new Entry<>(next, from(subscribers)));
                                return true;
                            } else {
                                switch (lagStrategy) {
                                    case DROP -> {
                                    }
                                    case ERROR -> {
                                        final var entryIterator = entries.iterator();
                                        final var nextEntry = entryIterator.next();
                                        entryIterator.remove();
                                        entryIterator.forEachRemaining(self::noop);
                                        nextEntry.subscribers().forEach(subscriber ->
                                                error(subscriber, new LagException(subscriber, self)));
                                        return true;
                                    }
                                    case THROW -> throw new LagException(null, MulticastPullPublisherStrategy.this);
                                }
                                return false;
                            }
                        }
                        case Error(var throwable) -> {
                            iterator.forEachRemaining(identity()::apply);
                            error = requireNonNull(throwable);
                            return active = false;
                        }
                        case Complete() -> {
                            iterator.forEachRemaining(identity()::apply);
                            completed = true;
                            return active = false;
                        }
                        default -> {
                            return false;
                        }
                    }

                }
                return false;
            }
        }, false);
    }
}
