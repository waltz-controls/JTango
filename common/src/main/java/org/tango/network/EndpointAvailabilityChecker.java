package org.tango.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Predicate;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class EndpointAvailabilityChecker implements Predicate<String> {
    private final Logger logger = LoggerFactory.getLogger(EndpointAvailabilityChecker.class);

    @Override
    public boolean test(String endpoint) {
        logger.debug("Check endpoint: {}", endpoint);
        URI uri = null;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException e) {
            logger.debug("Bad endpoint: " + endpoint, e);
            return false;
        }

        //  Try to connect
        InetSocketAddress ip = new InetSocketAddress(uri.getHost(), uri.getPort());
        try (Socket socket = new Socket()) {
            socket.connect(ip, 10);
            return true;
        } catch (IOException e) {
            logger.debug("Failed to connect to " + ip, e);
            return false;
        }
    }
}
