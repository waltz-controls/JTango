package org.tango.server.transport;

import fr.esrf.Tango.AttributeValue;
import fr.esrf.Tango.DevFailed;
import org.tango.server.idl.TangoIDLAttributeUtil;
import org.tango.server.servant.DeviceImpl;
import org.tango.transport.Message;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class WriteMessageProcessor implements ZmqMessageProcessor {
    private final DeviceImpl device;
    private final String attributeName;
    private final Object value;

    public WriteMessageProcessor(DeviceImpl device, String attributeName, Object value) {
        this.device = device;
        this.attributeName = attributeName;
        this.value = value;
    }

    @Override
    public Message process() {
        try {
            device.write_attributes(
                    new AttributeValue[]{
                            TangoIDLAttributeUtil.toAttributeValue(
                                    device.getAttributeImpl(attributeName).get(),
                                    new org.tango.server.attribute.AttributeValue(value))});
            return new Message.Ok();
        } catch (DevFailed devFailed) {
            return new Message.Error(devFailed.errors[0].reason);
        }
    }
}
