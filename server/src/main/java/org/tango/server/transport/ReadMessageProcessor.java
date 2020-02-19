package org.tango.server.transport;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.DevFailed;
import org.omg.CORBA.Any;
import org.tango.server.idl.CleverAnyAttribute;
import org.tango.server.servant.DeviceImpl;
import org.tango.transport.Message;

import java.util.Arrays;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class ReadMessageProcessor implements ZmqMessageProcessor {
    private final DeviceImpl device;
    private final String attributeName;
    private final String dataType;

    public ReadMessageProcessor(DeviceImpl device, String attributeName, String dataType) {
        this.device = device;
        this.attributeName = attributeName;
        this.dataType = dataType;
    }

    @Override
    public Message process() {
        try {
            Any any = Arrays.stream(device.read_attributes(new String[]{attributeName})).map(attributeValue -> attributeValue.value).findFirst().get();

            Object result = CleverAnyAttribute.get(any, Integer.parseInt(dataType), AttrDataFormat.SCALAR);
            return new Message("response", attributeName, dataType, String.valueOf(result));
        } catch (DevFailed devFailed) {
            return new Message.Error(devFailed.errors[0].reason);
        }
    }
}
