package org.tango.client.rx;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.tango.client.ez.proxy.EventData;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;

import java.util.concurrent.TimeUnit;

/**
 * @author ingvord
 * @since 05.09.2019
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RxTangoAttributeChangePublisherTest {

    private TangoProxy proxy;

    @BeforeAll
    public void beforeAll() throws Exception {
        this.proxy = TangoProxies.newDeviceProxyWrapper("tango://hzgxenvtest:10000/development/test_server/0");
    }

    private TestSubscriber<EventData> createSubscriber(){
        return TestSubscriber.create(new Subscriber<EventData>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("Subscribed in " + Integer.toHexString(hashCode()));
            }

            @Override
            public void onNext(EventData eventData) {
                System.out.println("["+Integer.toHexString(hashCode())+"]" + eventData.getValue() + "@" + eventData.getTime());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                System.err.println(t.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Completed");
            }
        });
    }

    @Test
    @Disabled
    void subscribe() throws Exception {


        new RxTangoAttributeChangePublisher(proxy, "State")
                .subscribe(createSubscriber());

        new RxTangoAttributeChangePublisher(proxy, "State")
                .subscribe(createSubscriber());


        Thread.sleep(30000);
    }

    @Test
    @Disabled
    void testThrottleLatest() throws Exception {
        Disposable d = Observable.fromPublisher(
                new RxTangoAttributeChangePublisher<EventData<?>>(proxy, "State"))
                .take(100)
                .subscribe((eventData) -> {
                    System.out.println("[TAKE100]" + eventData.getValue() + "@" + eventData.getTime());
                });

        Thread.sleep(1000);

        Disposable d2 = Observable.fromPublisher(
            new RxTangoAttributeChangePublisher<EventData<?>>(proxy, "State"))
                .throttleLatest(1000, TimeUnit.MILLISECONDS)
                .subscribe((eventData) -> {
                    System.out.println("[THROTTLE1000]" + eventData.getValue() + "@" + eventData.getTime());
                });

        Thread.sleep(10000);
        d.dispose();
        d2.dispose();
    }

    @Test
    @Disabled
    void testReplay() throws Exception {

        Observable<EventData<?>> observable = Observable.fromPublisher(new RxTangoAttributeChangePublisher<EventData<?>>(proxy, "State"))
                .replay(1)
                .autoConnect();

        System.out.print("[1] Subscribes @");
        System.out.println(System.currentTimeMillis());
        Disposable d1 = observable.subscribe((eventData) -> {
            System.out.println("[1]" + eventData.getValue() + "@" + eventData.getTime());
        });


        Thread.sleep(10000);

        System.out.print("[2] Subscribes @");
        System.out.println(System.currentTimeMillis());
        Disposable d2 = observable.subscribe((eventData) -> {
            System.out.println("====>");
            System.out.println(System.currentTimeMillis());
            System.out.println("[2]" + eventData.getValue() + "@" + eventData.getTime());
            System.out.println("<====");
        });

        Thread.sleep(3000);
        d1.dispose();
        d2.dispose();
    }
}
