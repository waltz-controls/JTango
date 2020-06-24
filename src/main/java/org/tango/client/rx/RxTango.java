package org.tango.client.rx;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.tango.client.ez.proxy.TangoProxy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author ingvord
 * @since 24.06.2020
 */
public abstract class RxTango<T> implements Publisher<T> {
    protected final TangoProxy proxy;
    protected final String name;

    public RxTango(TangoProxy proxy, String name) {
        this.proxy = proxy;
        this.name = name;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) throw new NullPointerException("subscriber can not be null!");

        Future<T> future = getFuture();

        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                try {
                    subscriber.onNext(future.get());
                    subscriber.onComplete();
                } catch (InterruptedException | ExecutionException devFailed) {
                    subscriber.onError(devFailed);
                }
            }

            @Override
            public void cancel() {
                future.cancel(true);
            }
        });
    }

    protected abstract Future<T> getFuture();
}
