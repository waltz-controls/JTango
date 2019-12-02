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
import fr.soleil.tango.clientapi.TangoAttribute;
import org.junit.Test;
import org.tango.utils.DevFailedUtils;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class TimeStampTest extends NoDBDeviceManager {

    @Test
    public void test() throws DevFailed {
        final TangoAttribute att = new TangoAttribute(deviceName + "/booleanScalar");

        att.read();
        assertThat(att.getTimestamp(), equalTo(123456L));
    }

    @Test
    public void testReadTimestampDouble() throws DevFailed {
        try {
            final TangoAttribute att = new TangoAttribute(deviceName + "/doubleScalar");

            att.read();
            final long time1 = att.getTimestamp();
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
            }
            att.read();
            final long time2 = att.getTimestamp();
            assertThat(time1, not(time2));
        } catch (final DevFailed e) {
            DevFailedUtils.printDevFailed(e);
            throw e;
        }
    }
}
