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
package org.tango.server.monitoring;

import javax.management.*;
import java.lang.management.ManagementFactory;

public class MonitoringService {

    private static final String FR_SOLEIL_MANAGEMENT_TYPE_TANGO_PARSER_STATS = "org.tango.server:type=TangoServerStats";
    private final TangoStats tangoStats;
    private ObjectName objectName;

    public MonitoringService(final String serverName) {
        tangoStats = TangoStats.getInstance();
        tangoStats.setServerName(serverName);
    }

    public void start() {
        // Register MBean in Platform MBeanServer
        final MBeanServer mbServer = ManagementFactory.getPlatformMBeanServer();
        try {
            objectName = new ObjectName(FR_SOLEIL_MANAGEMENT_TYPE_TANGO_PARSER_STATS);
            mbServer.registerMBean(tangoStats, objectName);
        } catch (final MalformedObjectNameException e) {
            // ignore
        } catch (final InstanceAlreadyExistsException e) {
            // ignore
        } catch (final MBeanRegistrationException e) {
            // ignore
        } catch (final NotCompliantMBeanException e) {
            // ignore
        }
    }

    public void stop() {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
        } catch (final MBeanRegistrationException e) {
            // ignore
        } catch (final InstanceNotFoundException e) {
            // ignore
        }
    }

}
