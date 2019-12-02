/**
 * Copyright (C) :     2012
 * <p>
 * Synchrotron Soleil
 * L'Orme des merisiers
 * Saint Aubin
 * BP48
 * 91192 GIF-SUR-YVETTE CEDEX
 * <p>
 * This file is part of Tango.
 * <p>
 * Tango is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Tango is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with Tango.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tango.server.testserver;

import fr.esrf.Tango.DevFailed;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.tango.server.Constants;
import org.tango.server.ServerManager;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Starts, stop the server JTangoTest without tango db
 *
 * @author ABEILLE
 *
 */
public class NoDBDeviceManager {

    public static String deviceName;

    public static String adminName;

    @BeforeClass
    public static void startDevice() throws DevFailed, IOException {
        System.setProperty("org.tango.server.checkalarms", "false");
        ServerSocket ss1 = null;
        try {
            ss1 = new ServerSocket(0);
            ss1.setReuseAddress(true);
            ss1.close();
            JTangoTest.startNoDb(ss1.getLocalPort());

            deviceName = "tango://localhost:" + ss1.getLocalPort() + "/" + JTangoTest.NO_DB_DEVICE_NAME + "#dbase=no";
            adminName = "tango://localhost:" + ss1.getLocalPort() + "/" + Constants.ADMIN_DEVICE_DOMAIN + "/"
                    + ServerManager.getInstance().getServerName() + "#dbase=no";
            System.out.println("START " + deviceName);
        } finally {
            if (ss1 != null) {
                ss1.close();
            }
        }

    }

    @AfterClass
    public static void stopDevice() throws DevFailed {
        System.out.println("STOP");
        ServerManager.getInstance().stop();
    }

}
