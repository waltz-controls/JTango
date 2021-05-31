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
// $Revision: 30265 $
//
//-======================================================================


package fr.esrf.TangoApi;

import fr.esrf.Tango.AttrQuality;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoApi.events.ZmqUtils;
import fr.esrf.TangoDs.Except;
import fr.esrf.TangoDs.TangoConst;
import org.jacorb.orb.Delegate;
import org.omg.CORBA.Request;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class Description: This class manage a static vector of Database object. <Br>
 * <Br>
 * <Br>
 * <b> Usage example: </b> <Br>
 * <ul>
 * <i> Database dbase = ApiUtil.get_db_obj(); <Br>
 * </ul>
 * </i>
 *
 * @author verdier
 * @author ingvord
 */

public class ApiUtilDAODefaultImpl implements IApiUtilDAO {
    private final AtomicReference<Database> localDatabase = new AtomicReference<>();

    static private Hashtable<Integer, AsyncCallObject> async_request_table =
            new Hashtable<Integer, AsyncCallObject>();
    static private int async_request_cnt = 0;
    static private int async_cb_sub_model = ApiDefs.PULL_CALLBACK;
    static private boolean in_server_code = false;

    // ===================================================================

    /**
     * Return the database object created for specified host and port.
     *
     * @param tango_host
     *            host and port (hostname:portnumber) where database is running.
     */
    // ===================================================================
    public Database get_db_obj(final String tango_host) throws DevFailed {
        String[] parsed = tango_host.split(":");
        return get_db_obj(parsed[0], parsed[1]);
    }

    // ===================================================================

    /**
     * Return the database object created with TANGO_HOST environment variable .
     */
    // ===================================================================
    public Database get_default_db_obj() {
        try {
            return get_db_obj(System.getProperty("TANGO_HOST", "localhost:10000"));
        } catch (DevFailed devFailed) {
            throw new RuntimeException("Can not get default database due to " + devFailed.getMessage());
        }
    }
    // ===================================================================

    /**
     * Return tru if the database object has been created.
     */
    // ===================================================================
    public boolean default_db_obj_exists() {
        return false;
    }

    // ===================================================================

    /**
     * Return the database object created with TANGO_HOST environment variable .
     */
    // ===================================================================
    public Database get_db_obj() {
        return get_default_db_obj();
    }

    // ===================================================================
    /**
     * Return the database object created for specified host and port.
     *
     * @param host host where database is running.
     * @param port port for database connection.
     */
    // ===================================================================
    public Database get_db_obj(final String host, final String port) throws DevFailed {
        Database database = localDatabase.get();
        return Optional.ofNullable(database).orElseGet(() -> {
            return change_db_obj(host, port);
        });
    }

    // ===================================================================

    /**
     * Return the database object created for specified host and port, and set
     * this database object for all following uses..
     *
     * @param host host where database is running.
     * @param port port for database connection.
     */
    // ===================================================================
    public Database change_db_obj(final String host, final String port) {
        try {
            Database database = new Database(host, port, true);
            localDatabase.set(database);
            return database;
        } catch (DevFailed devFailed) {
            throw new RuntimeException(String.format("Failed to connect to Tango db %s:%s",host,port));
        }
    }

    /**
     * Return the database object created for specified host and port, and set
     * this database object for all following uses..
     *
     * @param host
     *            host where database is running.
     * @param port
     *            port for database connection.
     */
    // ===================================================================
    public Database set_db_obj(final String host, final String port) throws DevFailed {
        return change_db_obj(host, port);
    }

    // ===================================================================

    /**
     * Return the database object created for specified host and port.
     *
     * @param tango_host host and port (hostname:portnumber) where database is running.
     */
    // ===================================================================
    public Database set_db_obj(final String tango_host) {
        final int i = tango_host.indexOf(":");
        if (i <= 0) {
            throw new IllegalArgumentException(String.format("Invalid tango_host: %s", tango_host));
        }
        return change_db_obj(tango_host.substring(0, i), tango_host.substring(i + 1));
    }

    /**
     * Return the orb object
     */
    // ===================================================================
    public void set_in_server(final boolean val) {
	in_server_code = val;
    }

    // ===================================================================
    /**
     * Return true if in server code or false if in client code.
     */
    // ===================================================================
    public boolean in_server() {
	return in_server_code;
    }

    // ==========================================================================
    // ==========================================================================
    public static String getUser()
    {
        return System.getProperty("user.name");
    }
    // ==========================================================================
    // ==========================================================================



    // ==========================================================================
    /*
     * Asynchronous request management
     */
    // ==========================================================================
    // ==========================================================================
    /**
     * Add request in hash table and return id
     */
    // ==========================================================================
    public synchronized int put_async_request(final AsyncCallObject aco) {

        async_request_cnt++;
        aco.id = async_request_cnt;
        async_request_table.put(async_request_cnt, aco);
        return async_request_cnt;
    }

    // ==========================================================================
    /**
     * Return the request in hash table for the id
     *
     * @throws DevFailed
     */
    // ==========================================================================
    public Request get_async_request(final int id) throws DevFailed {

	if (!async_request_table.containsKey(id)) {
	    Except.throw_exception("ASYNC_API_ERROR", "request for id " + id + " does not exist",
		    this.getClass().getCanonicalName() + ".get_async_request");
	}
	final AsyncCallObject aco = async_request_table.get(id);
	return aco.request;
    }

    // ==========================================================================
    /**
     * Return the Asynch Object in hash table for the id
     */
    // ==========================================================================
    public AsyncCallObject get_async_object(final int id) {
	return async_request_table.get(id);
    }

    // ==========================================================================
    /**
     * Remove asynchronous call request and id from hashtable.
     */
    // ==========================================================================
    // GA: add synchronized
    public synchronized void remove_async_request(final int id) {

        // Try to destroye Request object (added by PV 7/9/06)
        final AsyncCallObject aco =  async_request_table.get(id);
        if (aco != null) {
            removePendingRepliesOfRequest(aco.request);
            ((org.jacorb.orb.ORB) ApiUtil.getOrb()).removeRequest(aco.request);
            async_request_table.remove(id);
        }
    }


    @SuppressWarnings("UnusedParameters")
    private static void removePendingReplies(final Delegate delegate) {
        // try to solve a memory leak. pending_replies is still growing when
        // server is in timeout
        /*****
        Removed for JacORB-3
        if (!delegate.get_pending_replies().isEmpty()) {
            delegate.get_pending_replies().clear();
        }
        *****/
    }
    public static void removePendingRepliesOfRequest(final Request request) {
        final org.jacorb.orb.Delegate delegate = (org.jacorb.orb.Delegate) ((org.omg.CORBA.portable.ObjectImpl) request
            .target())._get_delegate();
        removePendingReplies(delegate);
    }

    public static void removePendingRepliesOfDevice(final Connection connection) {
        final org.jacorb.orb.Delegate delegate;
        if (connection.device_4 != null) {
            delegate = (org.jacorb.orb.Delegate) ((org.omg.CORBA.portable.ObjectImpl) connection.device_4)
                ._get_delegate();
        } else if (connection.device_3 != null) {
            delegate = (org.jacorb.orb.Delegate) ((org.omg.CORBA.portable.ObjectImpl) connection.device_3)
                ._get_delegate();
        } else if (connection.device_2 != null) {
            delegate = (org.jacorb.orb.Delegate) ((org.omg.CORBA.portable.ObjectImpl) connection.device_2)
                ._get_delegate();
        } else if (connection.device != null) {
            delegate = (org.jacorb.orb.Delegate) ((org.omg.CORBA.portable.ObjectImpl) connection.device)
                ._get_delegate();
        }
        else {
            return;
        }
        removePendingReplies(delegate);
    }

    // ==========================================================================
    /**
     * Set the reply_model in AsyncCallObject for the id key.
     */
    // ==========================================================================
    public void set_async_reply_model(final int id, final int reply_model) {
        final AsyncCallObject aco = async_request_table.get(id);
        if (aco != null) {
            aco.reply_model = reply_model;
        }
    }

    // ==========================================================================
    /**
     * Set the Callback object in AsyncCallObject for the id key.
     */
    // ==========================================================================
    public void set_async_reply_cb(final int id, final CallBack cb) {
        final AsyncCallObject aco = async_request_table.get(id);
        if (aco != null) {
            aco.cb = cb;
        }
    }

    // ==========================================================================
    /**
     * return the still pending asynchronous call for a reply model.
     *
     * @param dev   DeviceProxy object.
     * @param reply_model
     *            ApiDefs.ALL_ASYNCH, POLLING or CALLBACK.
     */
    // ==========================================================================
    public int pending_asynch_call(final DeviceProxy dev, final int reply_model) {
        int cnt = 0;
        final Enumeration _enum = async_request_table.keys();
        while (_enum.hasMoreElements()) {
            int n = (Integer)_enum.nextElement();
            final AsyncCallObject aco = async_request_table.get(n);
            if (aco.dev == dev
                && (reply_model == ApiDefs.ALL_ASYNCH || aco.reply_model == reply_model)) {
            cnt++;
            }
        }
        return cnt;
    }

    // ==========================================================================
    /**
     * return the still pending asynchronous call for a reply model.
     *
     * @param reply_model
     *            ApiDefs.ALL_ASYNCH, POLLING or CALLBACK.
     */
    // ==========================================================================
    public int pending_asynch_call(final int reply_model) {
        int cnt = 0;
        final Enumeration _enum = async_request_table.keys();
        while (_enum.hasMoreElements()) {
            int n = (Integer)_enum.nextElement();
            final AsyncCallObject aco = async_request_table.get(n);
            if (reply_model == ApiDefs.ALL_ASYNCH || aco.reply_model == reply_model) {
                cnt++;
            }
        }
        return cnt;
    }

    // ==========================================================================
    /**
     * Return the callback sub model used.
     *
     * @param model
     *            ApiDefs.PUSH_CALLBACK or ApiDefs.PULL_CALLBACK.
     */
    // ==========================================================================
    public void set_asynch_cb_sub_model(final int model) {
    	async_cb_sub_model = model;
    }

    // ==========================================================================
    /**
     * Set the callback sub model used (ApiDefs.PUSH_CALLBACK or
     * ApiDefs.PULL_CALLBACK).
     */
    // ==========================================================================
    public int get_asynch_cb_sub_model() {
    	return async_cb_sub_model;
    }

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies() {
        final Enumeration _enum = async_request_table.keys();
        while (_enum.hasMoreElements()) {
            int n = (Integer)_enum.nextElement();
            final AsyncCallObject aco = async_request_table.get(n);
            aco.manage_reply(ApiDefs.NO_TIMEOUT);
        }
    }

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies(final int timeout) {
        final Enumeration _enum = async_request_table.keys();
        while (_enum.hasMoreElements()) {
            int n = (Integer)_enum.nextElement();
            final AsyncCallObject aco = async_request_table.get(n);
            aco.manage_reply(timeout);
        }
    }

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies(final DeviceProxy dev) {
        final Enumeration _enum = async_request_table.keys();
        while (_enum.hasMoreElements()) {
            int n = (Integer)_enum.nextElement();
                final AsyncCallObject aco = async_request_table.get(n);
                if (aco.dev == dev) {
                    aco.manage_reply(ApiDefs.NO_TIMEOUT);
            }
        }
    }

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies(final DeviceProxy dev, final int timeout) {
        final Enumeration _enum = async_request_table.keys();
        while (_enum.hasMoreElements()) {
            int n = (Integer)_enum.nextElement();
                final AsyncCallObject aco = async_request_table.get(n);
                if (aco.dev == dev) {
                    aco.manage_reply(timeout);
            }
        }
    }

    // ==========================================================================
    /*
     * Methods to convert data.
     */
    // ==========================================================================


    // ==========================================================================
    // ==========================================================================
    public String stateName(final DevState state) {
	    return TangoConst.Tango_DevStateName[state.value()];
    }

    // ==========================================================================
    // ==========================================================================
    public String stateName(final short state_val) {
    	return TangoConst.Tango_DevStateName[state_val];
    }

    // ==========================================================================
    // ==========================================================================
    public String qualityName(final AttrQuality att_q) {
	    return TangoConst.Tango_QualityName[att_q.value()];
    }

    // ==========================================================================
    // ==========================================================================
    public String qualityName(final short att_q_val) {
	    return TangoConst.Tango_QualityName[att_q_val];
    }

    // ===================================================================
    // ===================================================================
    public double getZmqVersion() {
        return ZmqUtils.getZmqVersion();
    }
    // ===================================================================
    // ===================================================================
}

