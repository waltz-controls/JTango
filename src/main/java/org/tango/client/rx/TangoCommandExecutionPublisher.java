package org.tango.client.rx;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceData;
import fr.soleil.tango.clientapi.command.ITangoCommand;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author ingvord
 * @since 06.09.2019
 */
public class TangoCommandExecutionPublisher implements Publisher<DeviceData> {

    private final ITangoCommand command;
    private final DevFailed failure;

    public TangoCommandExecutionPublisher(ITangoCommand command, DevFailed failure) {
        this.command = command;
        this.failure = failure;
    }


    @Override
    public void subscribe(Subscriber<? super DeviceData> subscriber) {
        if(subscriber == null) throw new NullPointerException("Publisher can not be null!");


        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                //TODO
            }

            @Override
            public void cancel() {
                //TODO
            }
        });

        if(failure != null) {
            subscriber.onError(failure);
            return;
        }



        //TODO subscriber.onNext();

        //TODO subscriber.onComplete();

        //TODO subscriber.onError();
    }
}
