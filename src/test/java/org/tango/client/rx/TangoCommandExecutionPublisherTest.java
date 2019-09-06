package org.tango.client.rx;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceData;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
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

        TestSubscriber<DeviceData> testSubscriber = new TestSubscriber<>();

        instance.subscribe(testSubscriber);

        testSubscriber.assertError(failure);
    }
}
