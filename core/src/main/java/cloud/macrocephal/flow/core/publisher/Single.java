package cloud.macrocephal.flow.core.publisher;

import cloud.macrocephal.flow.core.operator.Operator;
import cloud.macrocephal.flow.core.operator.internal.NthOperator;
import cloud.macrocephal.flow.core.publisher.internal.PublisherDefault;
import cloud.macrocephal.flow.core.publisher.strategy.PublisherStrategy;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import static java.math.BigInteger.ONE;

public final class Single<T> extends PublisherDefault<T> {
    public Single(Publisher<T> publisher) {
        super(publisher);
    }

    public Single(PublisherStrategy<T> publisherStrategy) {
        super(publisherStrategy);
    }

    public <P extends Publisher<?>> P pipe(Operator<T, P> operator) {
        return super.pipe(operand -> operator.apply(new NthOperator<T>(ONE).apply(operand)));
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        pipe(operand -> operand).subscribe(subscriber);
    }
}
