package org.tango.server;

import fr.esrf.Tango.DevFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.export.TangoExporter;
import org.tango.server.servant.DeviceImpl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.12.2019
 */
public class ServerManagerUtils {
    private final Logger logger = LoggerFactory.getLogger(ServerManagerUtils.class);


    public static final String PID_FILE = "pid.file";

    public static final ServerManagerUtils INSTANCE = new ServerManagerUtils();

    public static ServerManagerUtils getInstance(){
        return INSTANCE;
    }

    private ServerManagerUtils(){}

    public void dumpPID(String pid, String serverName){
        String fileName = System.getProperty(PID_FILE, serverName + ".pid");

        Path path = Paths.get(fileName);

        try {
            Files.write(path, pid.getBytes(Charset.defaultCharset()));
            path.toFile().deleteOnExit();
        } catch (IOException e) {
            logger.warn("Failed to create pid file {} due to {}", path.toAbsolutePath().toString(), e.toString());
        }
    }

    /**
     * @param instance
     * @param clazz
     * @param <T>
     * @return collections that contains business objects of the Tango instance
     * @throws java.lang.RuntimeException
     */
    public static <T> List<T> getBusinessObjects(String instance, final Class<T> clazz) {
        try {
            final TangoExporter tangoExporter = ServerManager.getInstance().getTangoExporter();

            final String[] deviceList = tangoExporter.getDevicesOfClass(clazz.getSimpleName());

            if (deviceList.length == 0) //No tango devices were found. Simply skip the following
                return Collections.emptyList();

            return Arrays.stream(deviceList)
                    .map(s -> {
                        try {
                            DeviceImpl deviceImpl = tangoExporter.getDevice(clazz.getSimpleName(), s);
                            return (T) deviceImpl.getBusinessObject();
                        } catch (DevFailed devFailed) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (DevFailed e) {
            throw new RuntimeException(e);
        }
    }
}
