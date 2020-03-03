package org.tango.utils;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.03.2020
 */
public class NetworkUtils {
    private static final NetworkUtils INSTANCE = new NetworkUtils();

    private final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);
    private final boolean nonParallelStream = false;

    private NetworkUtils() {
    }

    public static NetworkUtils getInstance() {
        return INSTANCE;
    }

    public List<String> getIp4Addresses() {
        Iterable<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            logger.error("Failed to get NICs due to " + e.getMessage(), e);
            return Collections.emptyList();
        }

        java.util.function.Predicate<NetworkInterface> filter = networkInterface -> {
            try {
                return !networkInterface.isLoopback() && !networkInterface.isVirtual() && networkInterface.isUp();
            } catch (SocketException e) {
                logger.warn("Ignoring NetworkInterface({}) due to an exception: {}", networkInterface.getName(), e);
                return false;
            }
        };

        Function<InterfaceAddress, String> interfaceAddressToString = interfaceAddress -> interfaceAddress.getAddress().getHostAddress();

        Iterable<NetworkInterface> filteredNICs = Iterables.filter(networkInterfaces, filter::test);

        //TODO #17
        return StreamSupport.stream(filteredNICs.spliterator(), nonParallelStream)
                .flatMap(networkInterface -> networkInterface.getInterfaceAddresses().stream())
                .map(interfaceAddressToString)
                .filter(s -> s.split("\\.").length == 4)
                .collect(Collectors.toList());
    }


    public boolean checkEndpointAvailable(String endpoint) {
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
