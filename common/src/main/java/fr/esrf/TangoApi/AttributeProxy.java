//+======================================================================
// $Source$
//
// Project:   Tango
//
// Description:  java source code for the TANGO client/server API.
//
// $Author: pascal_verdier $
//
// Copyright (C) :      2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,
//						European Synchrotron Radiation Facility
//                      BP 220, Grenoble 38043
//                      FRANCE
//
// This file is part of Tango.
//
// Tango is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// Tango is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License
// along with Tango.  If not, see <http://www.gnu.org/licenses/>.
//
// $Revision: 26328 $
//
//-======================================================================


package fr.esrf.TangoApi;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;

import java.util.Collections;

/**
 * Class Description:
 * This class manage device connection for Tango attribute access.
 *
 * @author verdier
 * @version $Revision: 26328 $
 * @deprecated use {@link DeviceProxy} directly instead
 */
@Deprecated
@SuppressWarnings("UnusedDeclaration")
public class AttributeProxy implements ApiDefs, java.io.Serializable {
    private final String deviceName;
    private final String fullAttributeName;
    private final String attributeName;
    private final DeviceProxy dev;
    private final int idl_version;

    //===================================================================

    /**
     * AttributeProxy constructor. It will import the device.
     *
     * @param    fullAttributeName    name of the attribute or its alias.
     * @param deviceProxy
     */
    //===================================================================
    public AttributeProxy(String fullAttributeName, DeviceProxy deviceProxy) {
        //	Extract device name
        this.deviceName =
                fullAttributeName.substring(0, fullAttributeName.lastIndexOf("/", fullAttributeName.length() - 1));

        //	Store full attribute name
        this.fullAttributeName = fullAttributeName;

        //	Store Tango attribute name
        this.attributeName =
                fullAttributeName.substring(fullAttributeName.lastIndexOf("/", fullAttributeName.length() - 1) + 1);

        //	And crate DeviceProxy Object
        dev = deviceProxy;
        int idl_version;
        try {
            idl_version = dev.get_idl_version();
        } catch (DevFailed devFailed) {
            idl_version = -1;
        }
        this.idl_version = idl_version;
    }

    //==========================================================================
    /**
     * Returns the alias name for this attribute
     * @return the alias name for this attribute
     * @throws DevFailed in case of database access failed
     */
    //==========================================================================
    public String get_alias() throws DevFailed {

        //	Then query database for an alias for deviceName.
        return dev.get_db_obj().get_alias_from_attribute(attributeName);
    }

    //==========================================================================
    //==========================================================================
    public void set_timeout_millis(int millis) throws DevFailed {
        dev.set_timeout_millis(millis);
    }

    //==========================================================================
    //==========================================================================
    public DeviceProxy getDeviceProxy() {
        return dev;
    }

    //==========================================================================
    //==========================================================================
    public int get_idl_version() throws DevFailed {
        return idl_version;
    }
    //==========================================================================
    /**
     * Return full attribute name
     */
    //==========================================================================
    public String fullName() {
        return fullAttributeName;
    }
    //==========================================================================
    /**
     * Return attribute name
     */
    //==========================================================================
    public String name() {
        return attributeName;
    }
    //==========================================================================
    /**
     * Ping the device proxy of this attribute.
     */
    //==========================================================================
    public long ping() throws DevFailed {
        return dev.ping();
    }
    //==========================================================================
    /**
     * Check state of the device proxy of this attribute.
     */
    //==========================================================================
    public DevState state() throws DevFailed {
        return dev.state();
    }
    //==========================================================================
    /**
     * Check status of the device proxy of this attribute.
     */
    //==========================================================================
    public String status() throws DevFailed {
        return dev.status();
    }
    //==========================================================================
    /**
     * Query the database for a device attribute
     * property for this device.
     *
     * @return property in DbAttribute object.
     */
    //==========================================================================
    public DbAttribute get_property()
            throws DevFailed {
        return dev.get_attribute_property(attributeName);
    }
    //==========================================================================
    /**
     * Insert or update an attribute properties for this device.
     * The property names and their values are specified by the DbAttribute array.
     *
     * @param property attribute  property (names and values).
     */
    //==========================================================================
    public void put_property(DbDatum property)
            throws DevFailed {
        DbAttribute db_att = new DbAttribute(attributeName);
        db_att.add(property);
        dev.put_attribute_property(db_att);
    }

    //==========================================================================
    /**
     * Insert or update an attribute properties for this device.
     * The property names and their values are specified by the DbAttribute array.
     *
     * @param properties attribute  properties (names and values).
     */
    //==========================================================================
    public void put_property(DbDatum[] properties)
            throws DevFailed {
        DbAttribute db_att = new DbAttribute(attributeName);
        Collections.addAll(db_att, properties);
        dev.put_attribute_property(db_att);
    }

    //==========================================================================
    /**
     * Delete a property for this object.
     *
     * @param propname Property name.
     */
    //==========================================================================
    public void delete_property(String propname)
            throws DevFailed {
        dev.delete_attribute_property(attributeName, propname);
    }
    //==========================================================================
    /**
     * Delete a list of properties for this object.
     *
     * @param propnames Property names.
     */
    //==========================================================================
    public void delete_property(String[] propnames)
            throws DevFailed {
        dev.delete_attribute_property(attributeName, propnames);
    }
    //==========================================================================
    /**
     * Get the attribute info.
     *
     * @return the attributes configuration.
     */
    //==========================================================================
    public AttributeInfo get_info() throws DevFailed {
        return dev.get_attribute_info(attributeName);
    }
    //==========================================================================
    /**
     * Get the attribute extended info.
     *
     * @return the attributes configuration.
     */
    //==========================================================================
    public AttributeInfoEx get_info_ex() throws DevFailed {
        return dev.get_attribute_info_ex(attributeName);
    }
    //==========================================================================
    /**
     * Update the attributes info for the specified device.
     *
     * @param attr the attributes info.
     */
    //==========================================================================
    public void set_info(AttributeInfo[] attr) throws DevFailed {
        dev.set_attribute_info(attr);
    }
    //==========================================================================
    /**
     * Update the attributes extended info for the specified device.
     *
     * @param attr the attributes info.
     */
    //==========================================================================
    public void set_info(AttributeInfoEx[] attr) throws DevFailed {
        dev.set_attribute_info(attr);
    }
    //==========================================================================
    /**
     * Read the attribute value for the specified device.
     *
     * @return the attribute value.
     */
    //==========================================================================
    public DeviceAttribute read() throws DevFailed {
        return dev.read_attribute(attributeName);
    }
    //==========================================================================
    /**
     * Write the attribute value for the specified device.
     *
     * @param devattr attribute name and value.
     */
    //==========================================================================
    public void write(DeviceAttribute devattr) throws DevFailed {
        dev.write_attribute(devattr);
    }
    // ==========================================================================
    /**
     * Write and then read the attribute values, for the specified device.
     *
     * @param devattr attribute names and values.
     */
    // ==========================================================================
    public DeviceAttribute write_read_attribute(DeviceAttribute devattr) throws DevFailed {
        return dev.write_read_attribute(devattr);
    }
    // ==========================================================================
    /**
     * Write and then read the attribute values, for the specified device.
     *
     * @param devattr attribute names and values.
     */
    // ==========================================================================
    public DeviceAttribute[] write_read_attribute(DeviceAttribute[] devattr) throws DevFailed {
        return dev.write_read_attribute(devattr);
    }

    //==========================================================================
    /**
     * Return the history for attribute polled.
     *
     * @param    nb        nb data to read.
     */
    //==========================================================================
    public DeviceDataHistory[] history(int nb) throws DevFailed {
        return dev.attribute_history(attributeName, nb);
    }
    //==========================================================================
    /**
     * Return the full history for attribute polled.
     */
    //==========================================================================
    public DeviceDataHistory[] history() throws DevFailed {
        return dev.attribute_history(attributeName);
    }
    //==========================================================================
    /**
     * Add a attribute to be polled for the device.
     * If already polled, update its polling period.
     *
     * @param    period    polling period.
     */
    //==========================================================================
    public void poll(int period) throws DevFailed {
        dev.poll_attribute(attributeName, period);
    }
    //==========================================================================
    /**
     * Returns the polling period (in ms) for specified attribute.
     */
    //==========================================================================
    public int get_polling_period() throws DevFailed {
        return dev.get_attribute_polling_period(attributeName);
    }
    //==========================================================================
    /**
     * Remove attribute of polled object list
     */
    //==========================================================================
    public void stop_poll() throws DevFailed {
        dev.stop_poll_attribute(attributeName);
    }
    //==========================================================================
    /**
     * Asynchronous read_attribute.
     */
    //==========================================================================
    public int read_asynch() throws DevFailed {
        return dev.read_attribute_asynch(attributeName);
    }
    //==========================================================================
    /**
     * Asynchronous read_attribute using callback for reply.
     *
     * @param    cb        a CallBack object instance.
     */
    //==========================================================================
    public void read_asynch(CallBack cb) throws DevFailed {
        dev.read_attribute_asynch(attributeName, cb);
    }
    //==========================================================================
    /**
     * Read Asynchronous read_attribute reply.
     *
     * @param    id    asynchronous call id (returned by read_attribute_asynch).
     * @param    timeout    number of milliseconds to wait reply before throw an excption.
     */
    //==========================================================================
    public DeviceAttribute[] read_reply(int id, int timeout) throws DevFailed {
        return dev.read_attribute_reply(id, timeout);
    }
    //==========================================================================

    /**
     * Read Asynchronous read_attribute reply.
     *
     * @param    id    asynchronous call id (returned by read_attribute_asynch).
     */
    //==========================================================================
    public DeviceAttribute[] read_reply(int id) throws DevFailed {
        return dev.read_attribute_reply(id);
    }
    //==========================================================================
    /**
     * Asynchronous write_attribute.
     *
     * @param    attr    Attribute value (name, writing value...)
     */
    //==========================================================================
    public int write_asynch(DeviceAttribute attr) throws DevFailed {
        return dev.write_attribute_asynch(attr);
    }
    //==========================================================================
    /**
     * Asynchronous write_attribute.
     *
     * @param    attr    Attribute value (name, writing value...)
     * @param    forget    forget the response if true
     */
    //==========================================================================
    public int write_asynch(DeviceAttribute attr, boolean forget) throws DevFailed {
        return dev.write_attribute_asynch(attr, forget);
    }
    //==========================================================================
    /**
     * Asynchronous write_attribute using callback for reply.
     *
     * @param    attr    Attribute values (name, writing value...)
     * @param    cb        a CallBack object instance.
     */
    //==========================================================================
    public void write_asynch(DeviceAttribute attr, CallBack cb)  throws DevFailed {
        dev.write_attribute_asynch(attr, cb);
    }
    //==========================================================================
    /**
     * check for Asynchronous write_attribute reply.
     *
     * @param    id    asynchronous call id (returned by read_attribute_asynch).
     */
    //==========================================================================
    public void write_reply(int id) throws DevFailed {
        dev.write_attribute_reply(id);
    }
    //==========================================================================
    /**
     * check for Asynchronous write_attribute reply.
     *
     * @param    id    asynchronous call id (returned by write_attribute_asynch).
     * @param    timeout    number of milliseconds to wait reply before throw an excption.
     */
    //==========================================================================
    public void write_reply(int id, int timeout) throws DevFailed {
        dev.write_attribute_reply(id, timeout);
    }
    //==========================================================================
    /**
     * Subscribe to an event.
     *
     * @param callback event callback.
     * @param    event event name.
     */
    //==========================================================================
    public int subscribe_event(int event, CallBack callback, String[] filters) throws DevFailed {
        return dev.subscribe_event(attributeName, event, callback, filters);
    }
    //==========================================================================
    //==========================================================================
}
