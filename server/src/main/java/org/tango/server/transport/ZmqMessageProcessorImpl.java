package org.tango.server.transport;

import com.google.gson.Gson;
import org.tango.server.admin.AdminDevice;
import org.tango.server.servant.DeviceImpl;
import org.tango.transport.Message;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class ZmqMessageProcessorImpl implements ZmqMessageProcessor {
    private final String msg;
    private final AdminDevice admin;

    public ZmqMessageProcessorImpl(String msg, AdminDevice admin) {
        this.msg = msg;
        this.admin = admin;
    }

    @Override
    public Message process() {
        Message message = Message.fromString(msg);

        Message.Target target = Message.Target.fromString(message.target);

        DeviceImpl device = admin.getDeviceImpl(target.device);

        switch (message.action) {
            case "read":
                return new ReadMessageProcessor(device, target.member, message.dataType).process();
            case "write":
                return new WriteMessageProcessor(device, target.member, new Gson().fromJson(message.value, Object.class)).process();
            case "exec":
            default:
                return new Message.Error("Unsupported message action - " + message.action);
        }
    }


}
