package org.tango.client.rx;

import fr.esrf.TangoApi.events.EventData;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * @author ingvord
 * @since 05.09.2019
 */
public class TangoAttributeChangePublisher implements Publisher<EventData> {
    @Override
    public void subscribe(Subscriber<? super EventData> subscriber) {
        if(subscriber == null) throw new NullPointerException("Subscriber can not be null");

        //TODO onSubscribe


        //TODO signals
    }
}
