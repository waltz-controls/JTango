package org.tango.client.rx;

import fr.esrf.TangoApi.events.EventData;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ingvord
 * @since 05.09.2019
 */
class TangoAttributeChangePublisherTest {

    @Test
    void subscribe() {
        TestSubscriber<EventData> subscriber = new TestSubscriber<>();

        new TangoAttributeChangePublisher()
                .subscribe(subscriber);

        subscriber.assertNoErrors();
    }
}
