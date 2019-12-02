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
package org.tango.server.export;

import fr.esrf.Tango.DevFailed;
import org.tango.server.build.DeviceClassBuilder;
import org.tango.server.servant.DeviceImpl;

public interface IExporter {

    /**
     * Build all devices of all classes that are is this executable
     *
     * @throws DevFailed
     */
    void exportAll() throws DevFailed;

    /**
     * Export all devices except admin device
     *
     * @throws DevFailed
     */
    void exportDevices() throws DevFailed;

    /**
     * Unexport all except admin device
     *
     * @throws DevFailed
     */
    void unexportDevices() throws DevFailed;

    void unexportAll() throws DevFailed;

    DeviceImpl buildDevice(final String name, final DeviceClassBuilder classBuilder) throws DevFailed;

    DeviceImpl getDevice(final String className, final String deviceName) throws DevFailed;

    void unexportDevice(String deviceName) throws DevFailed;

    DeviceImpl buildDevice(String deviceName, Class<?> clazz) throws DevFailed;

}
