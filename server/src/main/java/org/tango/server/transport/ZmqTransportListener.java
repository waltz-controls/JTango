package org.tango.server.transport;

import org.tango.server.admin.AdminDevice;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class ZmqTransportListener implements Runnable {
    private final ZMQ.Socket socket;
    private final AdminDevice admin;

    public ZmqTransportListener(ZMQ.Socket socket, AdminDevice admin) {
        this.socket = socket;
        this.admin = admin;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] data = socket.recv();
            String msg = new String(data, StandardCharsets.UTF_8);

            //TODO non blocking
            //TODO thread pool
            socket.send(
                    new ZmqMessageProcessorImpl(msg, admin)
                            .process()
                            .toString()
                            .getBytes(StandardCharsets.UTF_8));

        }
    }
}
