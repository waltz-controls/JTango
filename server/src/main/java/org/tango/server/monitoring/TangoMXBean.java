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

import javax.management.MXBean;

@MXBean
public interface TangoMXBean {

    String getServerName();

    String getLastRequest();

    String getMaxRequest();

    long getRequestsPerSecond();

    long getMaxRequestsPerSecond();

    long getAverageRequestsPerSecond();

    long getMinRequestsPerSecond();

    long getLastRequestDuration();

    long getMaxRequestDuration();

    long getAverageRequestDuration();

    long getMinRequestDuration();

    long getErrorNr();

    void resetStats();

}
