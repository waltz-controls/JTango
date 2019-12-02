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
package org.tango.server.annotation;

import fr.esrf.Tango.DevVarDoubleStringArray;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.Tango.DispLevel;
import org.tango.DeviceState;
import org.tango.server.command.CommandConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Declare a command of a tango device.
 * </p>
 * Declares a command myCommand with parameter of type int and returns double
 *
 * <pre>
 * &#064;Command
 * public double myCommand(int value){..};
 * </pre>
 *
 * <p>
 * Possible parameter types and return types are:
 * </p>
 * boolean, boolean[], short, short[], long, long[], float, float[], double, double[], String, String[], int, int[],
 * {@link DeviceState}, {@link DeviceState}[], byte, byte[], DevEncoded, {@link DevVarLongStringArray},
 * {@link DevVarDoubleStringArray}
 *
 * @author ABEILLE
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    /**
     * Default name is method name
     *
     * @return The command name.
     */
    String name() default "";

    String inTypeDesc() default CommandConfiguration.UNINITIALISED;

    /**
     * The command output documentation
     *
     * @return output doc
     */
    String outTypeDesc() default CommandConfiguration.UNINITIALISED;

    /**
     * The command display level. see @link {@link DispLevel}
     *
     * @return the display level
     */
    int displayLevel() default 0;

    /**
     * define if attribute is polling. period must be configured. see {@link Attribute#pollingPeriod()}
     *
     * @return is polled
     */
    boolean isPolled() default false;

    /**
     * Configure polling period in ms. use only is {@link Attribute#isPolled()} is true
     *
     * @return polling period
     */
    int pollingPeriod() default 0;

}
