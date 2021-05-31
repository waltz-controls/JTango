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
// $Revision: 30280 $
//
//-======================================================================


package fr.esrf.TangoApi;

import fr.esrf.Tango.AttrQuality;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.Tango.factory.TangoFactory;
import fr.esrf.TangoDs.Except;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.utils.DevFailedUtils;

import java.io.FileOutputStream;
import java.util.*;
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

public class ApiUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiUtil.class);

    private static final IApiUtilDAO API_UTIL_DAO = TangoFactory.getSingleton().getApiUtilDAO();
    private static int  hwmValue = 0;

    // ===================================================================
    /**
     * ORB object reference for connection.
     */
    private static final AtomicReference<ORB> LOCAL_ORB = new AtomicReference<>();


    // ===================================================================

    static {
        try {
            LOCAL_ORB.set(create_orb());
        } catch (DevFailed devFailed) {
            DevFailedUtils.logDevFailed(devFailed, LOGGER);
            throw new RuntimeException(devFailed);
        }
    }

    /**
     * Create the orb object
     * @throws DevFailed if ORB creation failed
     */
    // ===================================================================
    private static ORB create_orb() throws DevFailed {
        try {
            // Modified properties fo ORB usage.
            // ---------------------------------------
            final Properties props = System.getProperties();
            props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
            props.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");

            // Set retry properties
            props.put("jacorb.retries", "0");
            props.put("jacorb.retry_interval", "100");

            // Initial timeout for establishing a connection.
            props.put("jacorb.connection.client.connect_timeout", "300");

            // Set the Largest transfer.
            final String str = checkORBgiopMaxMsgSize();
            props.put("jacorb.maxManagedBufSize", str);

            //	Check for max threads
            final String nbThreads = System.getProperty("max_receptor_threads");
            if (nbThreads!=null)
                props.put("jacorb.connection.client.max_receptor_threads", nbThreads);

            // Set jacorb verbosity at minimum value
            props.put("jacorb.config.log.verbosity", "0");
            props.put("jacorb.disableClientOrbPolicies", "off");

            //  Add code set to jacorb.properties
            props.put("jacorb.codeset", "true");

            //  Add directory to get jacorb.properties
            props.put("jacorb.config.dir", "fr/esrf/TangoApi/etc");
            System.setProperties(props);

            // Initialize ORB
            // -----------------------------
            final String[] argv = null;
            ORB orb = ORB.init(argv, null);

            // Get an instance of DevLockManager to initialize.
            DevLockManager.getInstance();
            return orb;
        } catch (final SystemException ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new ConnectionFailed(DevFailedUtils.newDevFailed(ex.toString(), "Initializing ORB failed !").errors);
        }
    }

    /**
     * Check if the checkORBgiopMaxMsgSize has been set. This environment
     * variable should be set in Mega bytes.
     * @return  the property string  to be set.
     */
    // ===================================================================
    private static String checkORBgiopMaxMsgSize() {
        /*
         * JacORB definition (see jacorb.properties file):
         *
         * This is NOT the maximum buffer size that can be used, but just the
         * largest size of buffers that will be kept and managed. This value
         * will be added to an internal constant of 5, so the real value in
         * bytes is 2**(5+maxManagedBufSize-1). You only need to increase this
         * value if you are dealing with LOTS of LARGE data structures. You may
         * decrease it to make the buffer manager release large buffers
         * immediately rather than keeping them for later reuse.
         */
        String str = "20"; // Set to 16 Mbytes

        // Check if environment ask for bigger size.
        String tmp = ApiUtil.getORBgiopMaxMsgSize();
        if (tmp != null) {
            if ((tmp = checkBufferSize(tmp)) != null) {
                str = tmp;
            }
        }
        return str;
    }

    // ===================================================================
    // ===================================================================
    private static String checkBufferSize(final String str) {
        // try to get value
        int nb_mega;
        try {
            nb_mega = Integer.parseInt(str);
        } catch (final NumberFormatException e) {
            return null;
        }

        // Compute the real size and the power of 2
        final long size = (long) nb_mega * 1024 * 1024;
        long l = size;
        int cnt;
        for (cnt = 0; l > 0; cnt++) {
            l >>= 1;
        }
        cnt--;

        // Check if number ob Mb is not power of 2
        if (Math.pow(2, cnt) < size) {
            cnt++;
        }
        System.out.println(nb_mega + " Mbytes  (2^" + cnt + ")");

        final int jacorb_size = cnt - 4;
        return Integer.toString(jacorb_size);
    }

    /**
     * Return the database object created for specified host and port.
     *
     * @param tango_host
     *            host and port (hostname:portnumber) where database is running.
     */
    // ===================================================================
    public static Database get_db_obj(final String tango_host) throws DevFailed {
	    return API_UTIL_DAO.get_db_obj(tango_host);
    }
    // ===================================================================
    // ===================================================================
    public void setApiUtilDAO(final IApiUtilDAO databaseDAO) {
    }
    // ===================================================================
    /**
     * @return true if use the default factory (new ObjectDAODefaultImpl or
     *         false if introspection factory (mainly used when from web)
     */
    // ===================================================================
    public static boolean useDefaultFactory() {
	    return TangoFactory.getSingleton().isDefaultFactory();
    }

    // ===================================================================

    /**
     * Return the database object created with TANGO_HOST environment variable .
     */
    // ===================================================================
    public static Database get_default_db_obj() throws DevFailed {
	    return API_UTIL_DAO.get_default_db_obj();
    }

    // ===================================================================

    /**
     * Return tru if the database object has been created.
     */
    // ===================================================================
    public static boolean default_db_obj_exists() throws DevFailed {
	    return API_UTIL_DAO.default_db_obj_exists();
    }
    // ===================================================================

    /**
     * Return the database object created with TANGO_HOST environment variable .
     */
    // ===================================================================
    public static synchronized Database get_db_obj() throws DevFailed {
	    return API_UTIL_DAO.get_db_obj();
    }

    // ===================================================================

    /**
     * Return the database object created for specified host and port.
     *
     * @param host host where database is running.
     * @param port port for database connection.
     */
    // ===================================================================
    public static Database get_db_obj(final String host, final String port) throws DevFailed {
	    return API_UTIL_DAO.get_db_obj(host, port);
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
    public static Database change_db_obj(final String host, final String port) throws DevFailed {
	    return API_UTIL_DAO.change_db_obj(host, port);
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
    public static Database set_db_obj(final String host, final String port) throws DevFailed {
	    return API_UTIL_DAO.set_db_obj(host, port);
    }
    // ===================================================================

    /**
     * Return the database object created for specified host and port.
     *
     * @param tango_host
     *            host and port (hostname:portnumber) where database is running.
     */
    // ===================================================================
    public static Database set_db_obj(final String tango_host) throws DevFailed {
	    return API_UTIL_DAO.set_db_obj(tango_host);
    }
    // ===================================================================

    /**
     * Return the orb object
     */
    // ===================================================================
    public static ORB get_orb() {
	    return LOCAL_ORB.get();
    }
    // ===================================================================
    // ===================================================================




    // ===================================================================

    /**
     * Return the orb object
     */
    // ===================================================================
    public static void set_in_server(final boolean val) {
    	API_UTIL_DAO.set_in_server(val);
    }
    // ===================================================================
    /**
     * Return the host address.
     */
    // ===================================================================
	public static String getHostAddress() throws DevFailed {
		return HostInfo.getAddress();
	}
    // ===================================================================
    /**
     * Return the host addresses.
     */
    // ===================================================================
	public static Vector<String> getHostAddresses() throws DevFailed {
		return HostInfo.getAddresses();
	}
     // ===================================================================
    /**
     * Return the host name.
     */
    // ===================================================================
	public static String getHostName() throws DevFailed {
		return HostInfo.getName();
	}
    // ===================================================================

    /**
     * Return true if in server code or false if in client code.
     */
    // ===================================================================
    public static boolean in_server() {
	    return API_UTIL_DAO.in_server();
    }

    /**
     * Add request in hash table and return id
     */
    // ==========================================================================
    public static int put_async_request(final AsyncCallObject aco) {
	    return API_UTIL_DAO.put_async_request(aco);
    }

    // ==========================================================================
    /*
     * Asynchronous request management
     */
    // ==========================================================================
    // ==========================================================================

    /**
     * Return the request in hash table for the id
     *
     * @throws DevFailed
     */
    // ==========================================================================
    public static Request get_async_request(final int id) throws DevFailed {
    	return API_UTIL_DAO.get_async_request(id);
    }

    // ==========================================================================

    /**
     * Return the Asynch Object in hash table for the id
     */
    // ==========================================================================
    public static AsyncCallObject get_async_object(final int id) {
    	return API_UTIL_DAO.get_async_object(id);
    }

    // ==========================================================================

    /**
     * Remove asynchronous call request and id from hashtable.
     */
    // ==========================================================================
    public static void remove_async_request(final int id) {
    	API_UTIL_DAO.remove_async_request(id);
    }

    // ==========================================================================

    /**
     * Set the reply_model in AsyncCallObject for the id key.
     */
    // ==========================================================================
    public static void set_async_reply_model(final int id, final int reply_model) {
    	API_UTIL_DAO.set_async_reply_model(id, reply_model);
    }

    // ==========================================================================

    /**
     * Set the Callback object in AsyncCallObject for the id key.
     */
    // ==========================================================================
    public static void set_async_reply_cb(final int id, final CallBack cb) {
	    API_UTIL_DAO.set_async_reply_cb(id, cb);
    }

    // ==========================================================================

    /**
     * return the still pending asynchronous call for a reply model.
     *
     * @param dev DeviceProxy object.
     * @param reply_model ApiDefs.ALL_ASYNCH, POLLING or CALLBACK.
     */
    // ==========================================================================
    public static int pending_asynch_call(final DeviceProxy dev, final int reply_model) {
	    return API_UTIL_DAO.pending_asynch_call(dev, reply_model);
    }

    // ==========================================================================

    /**
     * return the still pending asynchronous call for a reply model.
     *
     * @param reply_model ApiDefs.ALL_ASYNCH, POLLING or CALLBACK.
     */
    // ==========================================================================
    public static int pending_asynch_call(final int reply_model) {
    	return API_UTIL_DAO.pending_asynch_call(reply_model);
    }

    // ==========================================================================

    /**
     * Set the callback sub model used (ApiDefs.PUSH_CALLBACK or
     * ApiDefs.PULL_CALLBACK).
     */
    // ==========================================================================
    public static int get_asynch_cb_sub_model() {
	    return API_UTIL_DAO.get_asynch_cb_sub_model();
    }

    // ==========================================================================

    /**
     * Return the callback sub model used.
     * 
     * @param model ApiDefs.PUSH_CALLBACK or ApiDefs.PULL_CALLBACK.
     */
    // ==========================================================================
    public static void set_asynch_cb_sub_model(final int model) {
	    API_UTIL_DAO.set_asynch_cb_sub_model(model);
    }

    // ==========================================================================

    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public static void get_asynch_replies() {
	    API_UTIL_DAO.get_asynch_replies();
    }

    // ==========================================================================

    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public static void get_asynch_replies(final int timeout) {
	    API_UTIL_DAO.get_asynch_replies(timeout);
    }

    // ==========================================================================

    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public static void get_asynch_replies(final DeviceProxy dev) {
	    API_UTIL_DAO.get_asynch_replies(dev);
    }

    // ==========================================================================

    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public static void get_asynch_replies(final DeviceProxy dev, final int timeout) {
	    API_UTIL_DAO.get_asynch_replies(dev, timeout);
    }

    // ==========================================================================

    // ==========================================================================
    // ==========================================================================
    public static String stateName(final DevState state) {
	    return API_UTIL_DAO.stateName(state);
    }

    // ==========================================================================
    /*
     * Methods to convert data.
     */
    // ==========================================================================
    // ==========================================================================
    /**
     * Convert arguments to one String array
     */
    // ==========================================================================
    public static String[] toStringArray(final String objname, final String[] argin) {
        final String[] array = new String[1 + argin.length];
        array[0] = objname;
        System.arraycopy(argin, 0, array, 1, argin.length);
        return array;
    }

    // ==========================================================================
    /**
     * Convert arguments to one String array
     */
    // ==========================================================================
    public static String[] toStringArray(final String objname, final String argin) {
        final String[] array = new String[2];
        array[0] = objname;
        array[1] = argin;
        return array;
    }

    // ==========================================================================
    /**
     * Convert arguments to one String array
     */
    // ==========================================================================
    public static String[] toStringArray(final String argin) {
        final String[] array = new String[1];
        array[0] = argin;
        return array;
    }

    // ==========================================================================
    /**
     * Convert a DbAttribute class array to a StringArray.
     *
     * @param objname  object name (used in first index of output array)..
     * @param attributes DbAttribute array to be converted
     * @return the String array created from input argument.
     */
    // ==========================================================================
    public static String[] toStringArray(final String objname, final DbAttribute[] attributes, final int mode) {
        final int nb_attr = attributes.length;

        // Copy object name and nb attrib to String array
        final ArrayList<String> list = new ArrayList<String>();
        list.add(objname);
        list.add(Integer.toString(nb_attr));
        for (DbAttribute attribute : attributes) {
            // Copy Attrib name and nb prop to String array
            list.add(attribute.name);
            list.add("" + attribute.size());
            for (int j=0 ; j<attribute.size() ; j++) {
                // Copy data to String array
                list.add(attribute.get_property_name(j));
                final String[] values = attribute.get_value(j);
                if (mode != 1) {
                    list.add("" + values.length);
                }
                list.addAll(Arrays.asList(values));
            }
        }
        // alloacte a String array
        final String[] array = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    // ==========================================================================
    /**
     * Convert a DbPipe class array to a StringArray for DB command.
     *
     * @param deviceNme  device name
     * @param dbPipe DbPipe object to be converted
     * @return the String array created from input argument.
     */
    // ==========================================================================
    public static String[] toStringArray(String deviceNme, DbPipe dbPipe) {
        ArrayList<String>   list = new ArrayList<String>();
        list.add(deviceNme);
        list.add(Integer.toString(1));  //  one pipe
        list.add(dbPipe.getName());
        list.add(Integer.toString(dbPipe.size()));  //  property number
        //  fOR EACH PROPERTY
        for (DbDatum datum : dbPipe) {
            if (!datum.is_empty()) {
                String[] values = datum.extractStringArray();
                list.add(datum.name);
                list.add(Integer.toString(values.length));  // Property value number (array case)
                Collections.addAll(list, values);
            }
        }
        String[] array = new String[list.size()];
        for (int i=0 ; i<list.size() ; i++) {
            System.out.println(list.get(i));
            array[i] = list.get(i);
        }
        return array;
    }
    /*
    Argin description:
Str[0] = Device name
Str[1] = Pipe number
Str[2] = Pipe name
Str[3] = Property number
Str[4] = Property name
Str[5] = Property value number (array case)
Str[6] = Property value 1
Str[n] = Property value n (array case)
     */
    // ==========================================================================
    // ==========================================================================
    /**
     * Convert a StringArray to a DbAttribute class array
     * 
     * @param array String array to be converted
     * @param mode decode argout params mode (mode=2 added 26/10/04)
     * @return the DbAtribute class array created from input argument.
     */
    // ==========================================================================
    public static DbAttribute[] toDbAttributeArray(final String[] array, final int mode) throws DevFailed {
        if (mode < 1 && mode > 2) {
            Except.throw_non_supported_exception("API_NotSupportedMode", "Mode " + mode
                    + " to decode attribute properties is not supported",
                    "ApiUtil.toDbAttributeArray()");
        }

        int idx = 1;
        final int nb_attr = Integer.parseInt(array[idx++]);
        final DbAttribute[] attr = new DbAttribute[nb_attr];
        for (int i = 0; i < nb_attr; i++) {
            // Create DbAttribute with name and nb properties
            // ------------------------------------------------------
            attr[i] = new DbAttribute(array[idx++]);

            // Get nb properties
            // ------------------------------------------------------
            final int nb_prop = Integer.parseInt(array[idx++]);

            for (int j = 0; j < nb_prop; j++) {
                // And copy property name and value in
                // DbAttribute's DbDatum array
                // ------------------------------------------
                final String p_name = array[idx++];
                switch (mode) {
                    case 1:
                        // Value is just one element
                        attr[i].add(p_name, array[idx++]);
                        break;
                    case 2:
                        // value is an array
                        final int p_length = Integer.parseInt(array[idx++]);
                        final String[] val = new String[p_length];
                        for (int p = 0; p < p_length; p++) {
                            val[p] = array[idx++];
                        }
                        attr[i].add(p_name, val);
                        break;
                }
            }
        }
        return attr;
    }
    // ==========================================================================
    /**
     * Convert a StringArray to a DbPipe
     *
     * @param pipeName pip name
     * @param array String array to be converted
     * @return the DbPipe created from input argument.
     * @throws DevFailed if array is not coherent.
     */
    // ==========================================================================
    public static DbPipe toDbPipe(String pipeName, final String[] array) throws DevFailed {
        DbPipe dbPipe = new DbPipe(pipeName);
        try {
            int index = 3;
            final int nbProperties = Integer.parseInt(array[index++]);
            for (int i=0 ; i<nbProperties ; i++) {
                String  propertyName = array[index++];
                final int nbValues = Integer.parseInt(array[index++]);
                String[]    arrayValues = new String[nbValues];
                for (int v=0 ; v<nbValues ; v++) {
                    arrayValues[v] = array[index++];
                }
                dbPipe.add(new DbDatum(propertyName, arrayValues));
            }
        }
        catch (Exception e) {
            Except.throw_exception("TangoApi_SyntaxError",  "Cannot convert data to DbPipe: "+e);
        }
        return dbPipe;
    }

    // ==========================================================================
    // ==========================================================================
    public static String stateName(final short state_val) {
	    return API_UTIL_DAO.stateName(state_val);
    }

    // ==========================================================================
    // ==========================================================================
    public static String qualityName(final AttrQuality att_q) {
	    return API_UTIL_DAO.qualityName(att_q);
    }

    // ==========================================================================
    // ==========================================================================
    public static String qualityName(final short att_q_val) {
	    return API_UTIL_DAO.qualityName(att_q_val);
    }

    /**
     * Parse Tango host (check multi Tango_host)
     */
    // ===================================================================
    public static String[] parseTangoHost(final String tgh) throws DevFailed {
        String host = null;
        String strport = null;
        try {
            // Check if there is more than one Tango Host
            StringTokenizer stk;
            if (tgh.indexOf(",") > 0) {
                stk = new StringTokenizer(tgh, ",");
            } else {
                stk = new StringTokenizer(tgh);
            }

            final ArrayList<String> arrayList = new ArrayList<String>();
            while (stk.hasMoreTokens()) {
                // Get each Tango_host
                final String th = stk.nextToken();
                final StringTokenizer stk2 = new StringTokenizer(th, ":");
                arrayList.add(stk2.nextToken()); // Host Name
                arrayList.add(stk2.nextToken()); // Port Number
            }

            // Get the default one (first)
            host = arrayList.get(0);
            strport = arrayList.get(1);
            Integer.parseInt(strport);

            // Put second one if exists in a singleton map object
            final String def_tango_host = host + ":" + strport;
            final DbRedundancy dbr = DbRedundancy.get_instance();
            if (arrayList.size() > 3) {
                final String redun = arrayList.get(2) + ":"
                        + arrayList.get(3);
                dbr.put(def_tango_host, redun);
            }
        } catch (final Exception e) {
            Except.throw_exception("TangoApi_TANGO_HOST_NOT_SET", e.toString()
                            + " occurs when parsing " + "\"TANGO_HOST\" property " + tgh,
                    "TangoApi.ApiUtil.parseTangoHost()");
        }
        return new String[]{host, strport};
    }

    // ===================================================================

    // ===================================================================
    // ===================================================================
    public static ORB getOrb() {
    	return get_orb();
    }

    // ===================================================================
    // ===================================================================

    // ===================================================================
    // ===================================================================
    public static void setOrb(final ORB orb) {
	    ApiUtil.LOCAL_ORB.set(orb);
    }

    /**
     * Return the zmq version as a double like
     *         3.22 for "3.2.2" or 0.0 if zmq not available
     *
     * @return the TangORB version as a String
     * @deprecated use ZmqUtils.
     */
    //===================================================================
    @Deprecated
    public static double getZmqVersion() {
        return API_UTIL_DAO.getZmqVersion();
    }

    // ===================================================================
    // ===================================================================

    // ===================================================================
    /**
     * Returns the TANGO_HOST found in JVM, environment, tangorc file,....
     */
    // ===================================================================
    public static String getTangoHost() throws DevFailed {
	    return TangoEnv.getTangoHost();
    }

    // ===================================================================
    /**
     * Returns the SUPER_TANGO found in JVM, environment, tangorc file,....
     */
    // ===================================================================
    public static boolean isSuperTango() {
	    return TangoEnv.isSuperTango();
    }

    // ===================================================================
    /**
     * Returns the ACCESS_DEVNAME found in JVM, environment, tangorc file,....
     */
    // ===================================================================
    public static String getAccessDevname() {
	    return TangoEnv.getAccessDevname();
    }

    // ===================================================================
    /**
     * Returns the TANGO_TIMEOUT found in JVM, environment, tangorc file,....
     */
    // ===================================================================
    public static String getStrDefaultTimeout() {
	    return TangoEnv.getStrDefaultTimeout();
    }

    // ===================================================================
    /**
     * @return the ORBgiopMaxMsgSize found in JVM, environment, tangorb file,....
     */
    // ===================================================================
    public static String getORBgiopMaxMsgSize() {
	    return TangoEnv.getORBgiopMaxMsgSize();
    }
	//===============================================================
    /**
     * Set the ZMQ high water mark buffer size.
     * @param bufferSize the expected buffer size.
     */
	//===============================================================
    public static void setEventBufferHWM(int bufferSize) {
        hwmValue = bufferSize;
    }
	//===============================================================
    /**
     * Get the ZMQ high water mark buffer size.
     * @return the expected buffer size.
     */
	//===============================================================
    public static int getEventBufferHWM() {
        return hwmValue;
    }
	//===============================================================
	//===============================================================
    public static void printTrace(String str) {
        String  trace = System.getenv("ApiTrace");
        if (trace!=null) {
            if (trace.equals("true")) {
                System.out.println(str);
            }
        }

    }
    //===============================================================
    /**
     * Write a trace file (used to debug).
     * It is used to debug, it does not throw DevFailed
     * @param filename   file name to be generated
     */
    //===============================================================
    public static void writeStackTraceFile(String filename, String deviceName){
        //  Get the stack to write it in a file
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            System.out.println(element);
        }
        //  Get host name and pid
        String host = "";
        try {
            host = java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (final java.net.UnknownHostException e) { /* */  }
        int pid = DevLockManager.getInstance().getJvmPid();
        filename += "."+host+"."+pid;
        StringBuilder sb = new StringBuilder(deviceName+":\n");
        for (StackTraceElement stackTraceElement : stackTrace)
            sb.append(stackTraceElement).append('\n');
        writeFile(filename, sb.toString());
    }
    //===============================================================
    /**
     * Write a trace file (used to debug).
     * It is used to debug, it does not throw DevFailed
     * @param filename  file name to be generated
     * @param code      code to be generated in file.
     */
    //===============================================================
    public static void writeFile(String filename, String code){
        try {
            FileOutputStream fidout = new FileOutputStream(filename);
            fidout.write(code.getBytes());
            fidout.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    // ===================================================================
    // ===================================================================
    public IApiUtilDAO getApiUtilDAO() {
        return API_UTIL_DAO;
    }
    //===================================================================
    /**
     * Convert a signed int to a unsigned value in a long
     * @param intValue    signed integer value to convert
     * @return the unsigned value in a long
     */
    //===================================================================
    public static long toLongUnsigned(int intValue) {
        long mask = 0x7fffffff;
        mask += (long) 1 << 31;
        return (intValue & mask);
    }
    //===================================================================
    //===================================================================
}
