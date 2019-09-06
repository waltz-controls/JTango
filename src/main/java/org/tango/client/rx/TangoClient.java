package org.tango.client.rx;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceData;
import fr.soleil.tango.clientapi.command.ITangoCommand;
import fr.soleil.tango.clientapi.command.RealCommand;
import org.reactivestreams.Publisher;

/**
 * @author ingvord
 * @since 06.09.2019
 */
public class TangoClient {
    private TangoClient(){}

    public Publisher<DeviceData> executeCommand(String commandEndpoint){
        ITangoCommand command = null;
        DevFailed failure = null;
        try {
            command = new RealCommand(commandEndpoint);
        } catch (DevFailed devFailed){
            failure = devFailed;
        }


        return new TangoCommandExecutionPublisher(command, failure);
    }
}
