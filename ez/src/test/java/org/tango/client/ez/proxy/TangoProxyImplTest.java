// +======================================================================
//   $Source$
//
//   Project:   ezTangORB
//
//   Description:  java source code for the simplified TangORB API.
//
//   $Author: Igor Khokhriakov <igor.khokhriakov@hzg.de> $
//
//   Copyright (C) :      2014
//                        Helmholtz-Zentrum Geesthacht
//                        Max-Planck-Strasse, 1, Geesthacht 21502
//                        GERMANY
//                        http://hzg.de
//
//   This file is part of Tango.
//
//   Tango is free software: you can redistribute it and/or modify
//   it under the terms of the GNU Lesser General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   Tango is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU Lesser General Public License for more details.
//
//   You should have received a copy of the GNU Lesser General Public License
//   along with Tango.  If not, see <http://www.gnu.org/licenses/>.
//
//  $Revision: 25721 $
//
// -======================================================================

package org.tango.client.ez.proxy;

import fr.esrf.TangoApi.DevicePipe;
import fr.esrf.TangoApi.DeviceProxy;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.06.13
 */
public class TangoProxyImplTest {
    @Test
    @Ignore
    public void testProxy() throws Exception {
        //TODO create "remote" device
//        SomeStupidTangoDevice device = TangoProxy.proxy("",SomeStupidTangoDevice.class);
//
//        String result = device.executeCommand(new int[]{1,2,3});

        TangoProxy proxy = TangoProxies.newDeviceProxyWrapper(new DeviceProxy("tango://hzgxenvtest:10000/development/status_server/test"));

        Path outAttr = Paths.get("target", "out.attr");
        Path outPipe = Paths.get("target", "out.pipe");
        Path outCmd = Paths.get("target", "out.cmd");

        Files.deleteIfExists(outAttr);
        Files.deleteIfExists(outPipe);
        Files.deleteIfExists(outCmd);

        for(int i = 0; i<1_000;i++){
            try {
                String[] result = proxy.readAttribute("data");
                Files.write(outAttr, Integer.toString(result.hashCode()).toString().concat("\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                DevicePipe pipe = proxy.toDeviceProxy().readPipe("status_server_pipe");
                Files.write(outPipe, Integer.toString(pipe.hashCode()).toString().concat("\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                String[] snapshot = proxy.executeCommand("getLatestSnapshot");
                Files.write(outCmd, Integer.toString(snapshot.hashCode()).toString().concat("\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (Throwable ignore){
                Thread.sleep(3000);
            }
        }
    }

    private static interface SomeStupidTangoDevice {
        int getIntAttr();

        String executeCommand(int[] args) throws IOException;

        //etc
    }
}
