package org.tango.client.rx;

import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.tango.client.ez.proxy.EventData;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;

/**
 * @author ingvord
 * @since 05.09.2019
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RxTangoAttributeChangePublisherTest {

    private TangoProxy proxy;

    @BeforeAll
    public void beforeAll() throws Exception {
        this.proxy = TangoProxies.newDeviceProxyWrapper("tango://localhost:10000/sys/tg_test/1");
    }

    @Test
    void subscribe() throws Exception {
        TestSubscriber<EventData> subscriber = TestSubscriber.create(new Subscriber<EventData>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(EventData eventData) {
                System.out.println(eventData.getValue() + "@" + eventData.getTime());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        new RxTangoAttributeChangePublisher(proxy, "double_scalar")
                .subscribe(subscriber);


        subscriber.assertNoErrors();

        Thread.sleep(3000);
    }
}
