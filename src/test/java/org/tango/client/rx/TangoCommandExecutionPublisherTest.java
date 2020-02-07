package org.tango.client.rx;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoApi.DeviceData;
import fr.soleil.tango.clientapi.command.ITangoCommand;
import fr.soleil.tango.clientapi.command.RealCommand;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tango.utils.DevFailedUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ingvord
 * @since 06.09.2019
 */
class TangoCommandExecutionPublisherTest {

    @Test
    void subscribeToFailedCommand() {
        DevFailed failure = DevFailedUtils.newDevFailed("Test failure");
        TangoCommandExecutionPublisher instance = new TangoCommandExecutionPublisher(null, failure);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

        instance.subscribe(testSubscriber);

        testSubscriber.assertError(failure);
    }

    @Test
    @Disabled
    void subscribeToCommand() throws Exception{
        ITangoCommand command = new RealCommand("tango://localhost:10000/sys/tg_test/1/State");

        TangoCommandExecutionPublisher instance = new TangoCommandExecutionPublisher(command);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

        instance.subscribe(testSubscriber);

        testSubscriber.assertValue(DevState.RUNNING);
        testSubscriber.assertComplete();
    }

    @Test
    @Disabled
    void subscribeCancel() throws Exception{
        ITangoCommand command = new RealCommand("tango://localhost:10000/sys/tg_test/1/State");

        TangoCommandExecutionPublisher instance = new TangoCommandExecutionPublisher(command);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

        instance.subscribe(testSubscriber);
        testSubscriber.cancel();

        testSubscriber.assertComplete();
    }
}
