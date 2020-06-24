package org.tango.client.rx;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.tango.client.ez.proxy.EventData;
import org.tango.client.ez.proxy.TangoEvent;
import org.tango.client.ez.proxy.TangoEventListener;
import org.tango.client.ez.proxy.TangoProxy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ingvord
 * @since 05.09.2019
 */
public class RxTangoAttributeChangePublisher<T> implements Publisher<EventData<?>>, TangoEventListener<T> {
    private final TangoProxy proxy;
    private final String name;
    private final List<? super Subscriber<? super EventData<?>>> subscribers = new CopyOnWriteArrayList<>();

    public RxTangoAttributeChangePublisher(TangoProxy proxy, String name) throws Exception {
        this.proxy = proxy;
        this.name = name;
        this.proxy.subscribeToEvent(name, TangoEvent.CHANGE);
        this.proxy.addEventListener(name, TangoEvent.CHANGE, this);
    }

    @Override
    public void subscribe(Subscriber<? super EventData<?>> subscriber) {
        if (subscriber == null) throw new NullPointerException("subscriber can not be null");

        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                //Noop
            }

            @Override
            public void cancel() {
                RxTangoAttributeChangePublisher.this.subscribers.remove(subscriber);
            }
        });

        this.subscribers.add(subscriber);
    }

    @Override
    public void onEvent(final org.tango.client.ez.proxy.EventData<T> eventData) {
        this.subscribers.forEach(subscriber -> ((Subscriber<EventData<T>>) subscriber).onNext(eventData));
    }

    @Override
    public void onError(Exception e) {
        this.subscribers.forEach(subscriber -> ((Subscriber<EventData<Exception>>) subscriber).onNext(new EventData<>(e, System.currentTimeMillis(), null)));
    }
}
