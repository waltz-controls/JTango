package org.tango.client.rx;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceData;
import fr.soleil.tango.clientapi.command.ITangoCommand;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author ingvord
 * @since 06.09.2019
 */
public class TangoCommandExecutionPublisher implements Publisher<Object> {

    private final ITangoCommand command;
    private final DevFailed failure;

    public TangoCommandExecutionPublisher(ITangoCommand command, DevFailed failure) {
        this.command = command;
        this.failure = failure;
    }

    public TangoCommandExecutionPublisher(ITangoCommand command) {
        this(command, null);
    }


    @Override
    public void subscribe(Subscriber<? super Object> subscriber) {
        if(subscriber == null) throw new NullPointerException("Publisher can not be null!");


        Future<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return command.executeExtract();
            } catch (DevFailed devFailed) {
                throw new CompletionException(devFailed);
            }
        });

        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                if(failure != null) {
                    subscriber.onError(failure);
                    return;
                }

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
}
