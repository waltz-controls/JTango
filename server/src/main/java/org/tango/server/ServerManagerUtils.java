package org.tango.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        } catch (IOException e) {
            logger.warn("Failed to create pid file {} due to {}", path.toAbsolutePath().toString(), e.toString());
        }
    }

    public void deletePIDFile(String serverName){
        String fileName = System.getProperty(PID_FILE, serverName + ".pid");

        Path path = Paths.get(fileName);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete pid file {} due to {}", path.toAbsolutePath().toString(), e.toString());
        }
    }
}
