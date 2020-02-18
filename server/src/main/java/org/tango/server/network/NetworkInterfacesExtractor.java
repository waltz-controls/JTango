package org.tango.server.network;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.tango.orb.ORBManager.OAI_ADDR;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.02.2020
 */
public class NetworkInterfacesExtractor {
    private final Logger logger = LoggerFactory.getLogger(NetworkInterfacesExtractor.class);

    public List<String> getIp4Addresses() {
        if (OAI_ADDR != null && !OAI_ADDR.isEmpty()) {
            return Lists.newArrayList(OAI_ADDR);
        } else {
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

            List<String> result = Lists.newArrayList();
            //TODO #17
            for (NetworkInterface nic : filteredNICs) {
                result.addAll(
                        Collections2.filter(
                                nic.getInterfaceAddresses()
                                        .stream()
                                        .map(interfaceAddressToString::apply)
                                        .collect(Collectors.toList()),
                                s -> s.split("\\.").length == 4)
                );
            }
            return result;
        }
    }
}
