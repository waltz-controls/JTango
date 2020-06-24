package org.tango.client.rx;

import fr.esrf.Tango.DevState;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;

/**
 * @author ingvord
 * @since 06.09.2019
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RxTangoCommandTest {

    private TangoProxy proxy;

    @BeforeAll
    public void beforeAll() throws Exception {
        this.proxy = TangoProxies.newDeviceProxyWrapper("tango://localhost:10000/sys/tg_test/1");
    }

    @Test
    @Disabled
    void subscribeToCommand() throws Exception {
        RxTangoCommand instance = new RxTangoCommand(proxy, "State");

        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

        instance.subscribe(testSubscriber);

        testSubscriber.assertValue(DevState.RUNNING);
        testSubscriber.assertComplete();
    }

    @Test
    @Disabled
    void subscribeCancel() throws Exception{
        RxTangoCommand instance = new RxTangoCommand(proxy, "State");

        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

        instance.subscribe(testSubscriber);
        testSubscriber.cancel();

        testSubscriber.assertComplete();
    }
}
