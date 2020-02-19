package org.tango.server.transport;

import org.tango.server.network.NetworkInterfacesExtractor;
import org.tango.transport.TransportMeta;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

/**
 * This class maintains ZMQ REQ/REP required data
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.02.2020
 */
public class TransportManager {
    private final ZContext context = new ZContext();

    private String port;

    public ZMQ.Socket upgradeTransport() {
        ZMQ.Socket socket = createZMQSocket();
        port = String.valueOf(socket.bindToRandomPort("tcp://*"));
        return socket;
    }

    public TransportMeta getTransportMeta() {
        List<String> connectionPoints = new NetworkInterfacesExtractor().getIp4Addresses();

        TransportMeta result = new TransportMeta();

        connectionPoints.stream()
                .map(s -> "tcp://" + s + ":" + port)
                .forEach(result::addEndpoint);

        return result;
    }

    public ZMQ.Socket createZMQSocket() {
        final ZMQ.Socket socket = context.createSocket(ZMQ.REP);
        socket.setLinger(0);
        socket.setReconnectIVL(-1);
        return socket;
    }

}
