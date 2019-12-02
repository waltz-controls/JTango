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

import fr.esrf.Tango.DispLevel;
import org.tango.server.pipe.PipeValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Declare a pipe of a tango device.
 * </p>
 *
 * Example:
 *
 * <pre>
 * &#064;Pipe
 * private {@link PipeValue} myPipe;
 *
 * public PipeValue getMyPipe(){..};
 * </pre>
 *
 * @author ABEILLE
 *
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Pipe {

    /**
     * Default name is field name
     *
     * @return The attribute name.
     */
    String name() default "";

    /**
     * The command display level. see @link {@link DispLevel}
     *
     * @return display level
     */
    int displayLevel() default 0;// DispLevel.OPERATOR;

    String label() default "";

    String description() default "";

}
