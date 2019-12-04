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
// $Revision:  $
//
//-======================================================================


package fr.esrf.TangoApi.events;


import fr.esrf.Tango.DevError;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.Tango.ErrSeverity;
import fr.esrf.TangoApi.*;
import fr.esrf.TangoDs.Except;
import fr.esrf.TangoDs.TangoConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.utils.DevFailedUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * @author pascal_verdier
 */
public class ZmqEventConsumer implements IEventConsumer {
    private final Logger logger = LoggerFactory.getLogger(ZmqEventConsumer.class);

    //TODO concurrent access
    private int subscribe_event_id = 0;
    //TODO replace with ConcurrentMap
    //TODO not static
    private static final Hashtable<String, EventChannelStruct>   channel_map        = new Hashtable<>();
    private static final Hashtable<String, String>               device_channel_map = new Hashtable<>();
    private static final Hashtable<String, EventCallBackStruct>  event_callback_map = new Hashtable<>();
    private static final Hashtable<String, EventCallBackStruct>  failed_event_callback_map = new Hashtable<>();

    //  Alternate tango hosts
    //TODO concurrent access
    private static final List<String> possibleTangoHosts = new ArrayList<>();

    //TODO get rid of singleton
    private static ZmqEventConsumer instance = null;

    /**
     * Creates a new instance of EventConsumer
     *
     * @return an instance of EventConsumer object
     * @throws DevFailed in case of database connection failed.
     */
    public static ZmqEventConsumer getInstance() {
        if (instance == null) {
            instance = new ZmqEventConsumer();
        }
        return instance;
    }

    private ZmqEventConsumer(){

        //  Default constructor
        //  Start the KeepAliveThread loop
        KeepAliveThread.getInstance();
        //  Start ZMQ main thread
        //TODO use Executors
        ZmqMainThread zmqMainThread = new ZmqMainThread(ZMQutils.getContext());
        zmqMainThread.start();
        addShutdownHook();
    }

    static Hashtable<String, EventChannelStruct> getChannelMap() {
        return channel_map;
    }

    static Hashtable<String, EventCallBackStruct>  getEventCallbackMap() {
        return event_callback_map;
    }

    /**
     * Try to connect if it failed at subscribe
     */
    static void subscribeIfNotDone() {
        Enumeration<String > callbackKeys = failed_event_callback_map.keys();
        while (callbackKeys.hasMoreElements()) {
            String callbackKey = callbackKeys.nextElement();
            EventCallBackStruct eventCallBackStruct = failed_event_callback_map.get(callbackKey);
            if (eventCallBackStruct.consumer!=null) {
                try {
                    subscribeIfNotDone(eventCallBackStruct, callbackKey);
                } catch (DevFailed e) {
                    //	Send error to callback
                    sendErrorToCallback(eventCallBackStruct, callbackKey, e);
                }
            }
            else {
                //TODO invert and throw exception or send error callback
                if (EventConsumerUtil.isZmqLoadable()) {
                    try {
                        //  Try for zmq
                        eventCallBackStruct.consumer = ZmqEventConsumer.getInstance();
                        subscribeIfNotDone(eventCallBackStruct, callbackKey);
                        return;

                    }
                    catch (DevFailed e) {
                        if (e.errors[0].desc.equals(ZMQutils.SUBSCRIBE_COMMAND_NOT_FOUND)) {
                            	//TODO use DevFailedUtils#printDevFailed from JTangoCommons
                               System.err.println(e.errors[0].desc);
                                //  reset if both have failed
                                eventCallBackStruct.consumer = null;
                                //	Send error to callback
                                sendErrorToCallback(eventCallBackStruct, callbackKey, e);
                        }
                        else {
                            //	Send error to callback
                            eventCallBackStruct.consumer = null;
                            sendErrorToCallback(eventCallBackStruct, callbackKey, e);
                        }
                    }
                }
            }
        }
    }

    private static void sendErrorToCallback(EventCallBackStruct cs, String callbackKey, DevFailed e) {

        int source = EventData.ZMQ_EVENT;
        EventData eventData =
                new EventData(cs.device, callbackKey,
                        cs.event_name, source,
                        cs.event_type, null, null, null, null, null, e.errors);

        if (cs.use_ev_queue) {
            EventQueue ev_queue = cs.device.getEventQueue();
            ev_queue.insert_event(eventData);
        } else
            cs.callback.push_event(eventData);
    }

    private static void subscribeIfNotDone(EventCallBackStruct eventCallBackStruct,
                                         String callbackKey) throws DevFailed{

       eventCallBackStruct.consumer.subscribe_event(
               eventCallBackStruct.device,
               eventCallBackStruct.attr_name,
               eventCallBackStruct.event_type,
               eventCallBackStruct.callback,
               eventCallBackStruct.max_size,
               eventCallBackStruct.filters,
               false);
       failed_event_callback_map.remove(callbackKey);
    }

    static EventCallBackStruct getCallBackStruct(Hashtable map, int id) {
        Enumeration keys = map.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            EventCallBackStruct callback_struct =
                    (EventCallBackStruct) map.get(name);
            if (callback_struct.id == id)
                return callback_struct;
        }
        return null;
    }

    private void addShutdownHook(){
        //	Create a thread and start it
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                public void run() {
                    logger.info("======== Shutting down ZMQ event system ==========");
                    KeepAliveThread.getInstance().stopThread();
                }
            }
        );
    }

    /**
     * Subscribe event on device (Interface Change Event)
     * @param device device to subscribe
     * @param event event to subscribe
     * @param callback method to be called when receive an event
     * @param max_size  queue maximum size if use queue
     * @param stateless subscription stateless if true
     * @return the event ID
     * @throws DevFailed if subscription failed.
     */
    public int subscribe_event(DeviceProxy device,
                               int event,
                               CallBack callback,
                               int max_size,
                               boolean stateless)
            throws DevFailed {
        //	Set the event name;
        String event_name = TangoConst.eventNames[event];
        logger.debug("=============> subscribing for {}.{}",  device.name(), event_name);

        //	if no callback (null), create EventQueue
        if (callback == null && max_size >= 0) {
            //	Check if already created (in case of reconnection stateless mode)
            if (device.getEventQueue() == null)
                if (max_size > 0)
                    device.setEventQueue(new EventQueue(max_size));
                else
                    device.setEventQueue(new EventQueue());
        }

        String deviceName = device.fullName();
        String callback_key = deviceName.toLowerCase();

        //  Not added for Interface change event (Special case)
        /*
        if (device.get_idl_version()>=5)
            callback_key += ".idl" + device.get_idl_version()+ "_" + event_name;
        else
        */
            callback_key += "." + event_name;
        try {
            //	Inform server that we want to subscribe and try to connect
            logger.trace("calling callEventSubscriptionAndConnect() method");
            callEventSubscriptionAndConnect(device, event_name);
            logger.trace("call callEventSubscriptionAndConnect() method done");
        } catch (DevFailed e) {
            //  re throw if not stateless
            if (!stateless || e.errors[0].desc.equals(ZMQutils.SUBSCRIBE_COMMAND_NOT_FOUND)) {
                throw e;
            }
            else {
                //	Build Event CallBack Structure and add it to map
                subscribe_event_id++;
                EventCallBackStruct new_event_callback_struct =
                        new EventCallBackStruct(device,
                                event_name,
                                "",
                                callback,
                                max_size,
                                subscribe_event_id,
                                event,
                                false);
                failed_event_callback_map.put(callback_key, new_event_callback_struct);
                return subscribe_event_id;
            }
        }

        //	Prepare filters for heartbeat events on channelName
        String channelName = device_channel_map.get(deviceName);
        if (channelName==null) {
            //  If from notifd, tango host not used.
            int start = deviceName.indexOf('/', "tango:// ".length());
            deviceName = deviceName.substring(start+1);
            channelName = device_channel_map.get(deviceName);
        }
        EventChannelStruct event_channel_struct = channel_map.get(channelName);
        event_channel_struct.last_subscribed = System.currentTimeMillis();

        //	Check if a new event or a re-trying one
        int eventId;
        EventCallBackStruct failed_struct = failed_event_callback_map.get(callback_key);
        if (failed_struct == null) {
            //	It is a new one
            subscribe_event_id++;
            eventId = subscribe_event_id;
        } else
            eventId = failed_struct.id;

        //	Build Event CallBack Structure if any
        EventCallBackStruct new_event_callback_struct =
                new EventCallBackStruct(device,
                        event_name,
                        channelName,
                        callback,
                        max_size,
                        eventId,
                        event,
                        true);
        new_event_callback_struct.consumer  = this;
        event_callback_map.put(callback_key, new_event_callback_struct);

        return eventId;
    }

    private void callEventSubscriptionAndConnect(DeviceProxy device, String eventType)
            throws DevFailed {
        //  Done for IDL>=5 and not for notifd event system (no attribute name)
        String device_name = device.name();
        String[] info = new String[] {
                device_name,
                "",
                "subscribe",
                eventType,
                Integer.toString(device.get_idl_version())
        };
        DeviceData argIn = new DeviceData();
        argIn.insert(info);
        String cmdName = ZMQutils.SUBSCRIBE_COMMAND;
        logger.trace("{}.command_inout({}) for {}.{}", device.get_adm_dev().name(), cmdName, device_name, eventType);
        DeviceData argOut =
                device.get_adm_dev().command_inout(cmdName, argIn);
        logger.trace("    command_inout done.");

        //	And then connect to device
        checkDeviceConnection(device, null, argOut, eventType);
    }

    private void setAdditionalInfoToEventCallBackStruct(EventCallBackStruct callback_struct,
                                                        String device_name, String attribute, String event_name, String[] filters, EventChannelStruct channel_struct) {
        // Nothing
        logger.debug("-------------> Set as ZmqEventConsumer for {}", device_name);
        callback_struct.consumer  = this;
    }

    private void connect(DeviceProxy deviceProxy, String attributeName,
                         String eventName, DeviceData deviceData) throws DevFailed {
        String deviceName = deviceProxy.fullName();
        int tangoVersion = deviceData.extractLongStringArray().lvalue[0];
        try {
            String adminName = deviceProxy.adm_name();  //.toLowerCase();
            //  Since Tango 8.1, heartbeat is sent in lower case.
            //tangoVersion = new DeviceProxy(adm_name).getTangoVersion();
            if (tangoVersion>=810)
                adminName = adminName.toLowerCase();

            // If no connection exists to this channel, create it
            Database database = null;
            if (!channel_map.containsKey(adminName)) {
                if (deviceProxy.use_db())
                    database = deviceProxy.get_db_obj();
                ConnectionStructure connectionStructure =
                        new ConnectionStructure(deviceProxy.get_tango_host(),
                                adminName, deviceName, attributeName,
                                eventName, database, deviceData, false);
                connect_event_channel(connectionStructure);
            } else if (deviceProxy.use_db()) {
                database = deviceProxy.get_db_obj();
                ZMQutils.connectEvent(deviceProxy.get_tango_host(), deviceName,
                        attributeName, deviceData.extractLongStringArray(), eventName,false);
            }
            EventChannelStruct eventChannelStruct = channel_map.get(adminName);
            eventChannelStruct.adm_device_proxy =  new DeviceProxy(adminName);
            eventChannelStruct.use_db = deviceProxy.use_db();
            eventChannelStruct.dbase = database;
            eventChannelStruct.setTangoRelease(tangoVersion);

            device_channel_map.put(deviceName, adminName);
        }
        catch (DevFailed e) {
            Except.throw_event_system_failed("API_BadConfigurationProperty",
                    "Can't subscribe to event for device " + deviceName
                            + "\n Check that device server is running...",
                    "ZmqEventConsumer.connect");
        }
    }

    /**
     *  Due to a problem when there is more than one network card,
     *  The address returned by the command ZmqEventSubscriptionChange
     *  is different than the getHostAddress() call !!!
     *  In this case the address from getHostAddress()
     *  replace the address in device data.
     */
    private DeviceData checkWithHostAddress(DeviceData deviceData, DeviceProxy deviceProxy) throws DevFailed {
// ToDo
        DevVarLongStringArray lsa = deviceData.extractLongStringArray();
        try {
            java.net.InetAddress iadd =
                    java.net.InetAddress.getByName(deviceProxy.get_host_name());
            String hostAddress = iadd.getHostAddress();
            logger.debug("Host address is {}", hostAddress);
            logger.debug("Server returns  {}", lsa.svalue[0]);
            if (! lsa.svalue[0].startsWith("tcp://"+hostAddress)) { //  Addresses are different
                 String  wrongAdd = lsa.svalue[0];
                 int idx = lsa.svalue[0].lastIndexOf(':');   //  get port
                 if (idx>0) {
                     lsa.svalue[0] = "tcp://" + hostAddress + lsa.svalue[0].substring(idx);
                     lsa.svalue[1] = "tcp://" + hostAddress + lsa.svalue[1].substring(idx);
                     logger.debug("{} ---> {}", wrongAdd, lsa.svalue[0]);
                     deviceData = new DeviceData();
                     deviceData.insert(lsa);
                     isEndpointAvailable(lsa.svalue[0]);
                 }
            }
        } catch (UnknownHostException e) {
            Except.throw_exception("UnknownHostException",
                    e.toString(), "ZmqEventConsumer.checkZmqAddress()");
        }
        logger.debug("---> Connect on {}", deviceData.extractLongStringArray().svalue[0]);
        return deviceData;
    }

    /**
     * In case of several endpoints, check which one is connected.
     * @param deviceData    data from ZmqEventSubscriptionChange command
     * @param deviceProxy   the admin device
     * @return the endpoints after checked
     * @throws DevFailed in case of connection problem
     */
    private DeviceData checkZmqAddress(DeviceData deviceData, DeviceProxy deviceProxy) throws DevFailed{
        logger.trace("Inside checkZmqAddress()");
        DevVarLongStringArray lsa = deviceData.extractLongStringArray();
        for (int i=0 ; i<lsa.svalue.length ; i+=2) {
            String endpoint = lsa.svalue[i];
            if (isEndpointAvailable(endpoint)) {
                lsa.svalue[0] = lsa.svalue[i];
                lsa.svalue[1] = lsa.svalue[i+1];
                logger.trace("return {} - {}", lsa.svalue[i], lsa.svalue[i+1]);
                deviceData = new DeviceData();
                deviceData.insert(lsa);
                return deviceData;
            }
        }
        //  Not found check with host address
        return checkWithHostAddress(deviceData, deviceProxy);
    }

    private boolean isEndpointAvailable(String endpoint) {
        logger.debug("Check endpoint: {}", endpoint);
        //  Split address and port
        int start = endpoint.indexOf("//");
        if (start<0)  {
            logger.warn("Bad endpoint: {} - no protocol specified ", endpoint);
            return false;
        }
        int end = endpoint.indexOf(":", start);
        if (end<0)  {
            logger.warn("Bad endpoint: {} - no port specified", endpoint);
            return false;
        }
        String address = endpoint.substring(start+2, end);
        int    port    = Integer.parseInt(endpoint.substring(end+1));

        //  Try to connect
        InetSocketAddress ip = new InetSocketAddress(address, port);
        Socket socket = new Socket();
        try {
            socket.connect(ip, 10);
            socket.close();
            return true;
        } catch (IOException e) {
            logger.warn("Failed to connect to " + ip, e);
            return false;
        }
    }

    private void checkDeviceConnection(DeviceProxy deviceProxy,
                                       String attribute, DeviceData deviceData, String event_name) throws DevFailed {

        //  Check if address is coherent (??)
        deviceData = checkZmqAddress(deviceData, deviceProxy);

        String deviceName = deviceProxy.fullName();
        logger.debug("checkDeviceConnection for {}", deviceName);
        if (!device_channel_map.containsKey(deviceName)) {
            logger.debug("device_channel_map has no entity for {}", deviceName);
            connect(deviceProxy, attribute, event_name, deviceData);
            if (!device_channel_map.containsKey(deviceName)) {
                Except.throw_event_system_failed("API_NotificationServiceFailed",
                        "Failed to connect to event channel for device",
                        "EventConsumer.subscribe_event()");
            }
        }
        else {
            logger.debug("{} already connected.", deviceName);
            ZMQutils.connectEvent(deviceProxy.get_tango_host(), deviceName,
                        attribute, deviceData.extractLongStringArray(), event_name,false);
        }
    }

    private synchronized void connect_event_channel(ConnectionStructure cs) throws DevFailed {
        //	Get a reference to an EventChannel for
        //  this device server from the tango database
        DeviceProxy adminDevice = new DeviceProxy(cs.channelName);
        cs.channelName = adminDevice.fullName().toLowerCase();    //  Update name with tango host

        DevVarLongStringArray   lsa = cs.deviceData.extractLongStringArray();
        logger.debug("connect_event_channel for {}", cs.channelName);

        //  Build the buffer to connect heartbeat and send it
        ZMQutils.connectHeartbeat(adminDevice.get_tango_host(), adminDevice.name(), lsa, false);

        //  Build the buffer to connect event and send it
        ZMQutils.connectEvent(cs.tangoHost, cs.deviceName, cs.attributeName,
                lsa, cs.eventName, false);
        if (cs.reconnect) {
            EventChannelStruct eventChannelStruct = channel_map.get(cs.channelName);
           // eventChannelStruct.eventChannel = eventChannel;
            eventChannelStruct.last_heartbeat = System.currentTimeMillis();
            eventChannelStruct.heartbeat_skipped = false;
            eventChannelStruct.has_notifd_closed_the_connection = 0;
            eventChannelStruct.setTangoRelease(lsa.lvalue[0]);
            eventChannelStruct.setIdlVersion(lsa.lvalue[1]);
        } else {
            //  Crate new one
            EventChannelStruct newEventChannelStruct = new EventChannelStruct();
            //newEventChannelStruct.eventChannel = eventChannel;
            newEventChannelStruct.last_heartbeat = System.currentTimeMillis();
            newEventChannelStruct.heartbeat_skipped = false;
            newEventChannelStruct.adm_device_proxy = adminDevice;
            newEventChannelStruct.has_notifd_closed_the_connection = 0;
            newEventChannelStruct.consumer = this;
            newEventChannelStruct.zmqEndpoint = lsa.svalue[0];
            newEventChannelStruct.setTangoRelease(lsa.lvalue[0]);
            newEventChannelStruct.setIdlVersion(lsa.lvalue[1]);
            channel_map.put(cs.channelName, newEventChannelStruct);
            ApiUtil.printTrace("Adding " + cs.channelName + " to channel_map");

            //  Get possible TangoHosts and add it to list if not already in.
            String[]    tangoHosts = adminDevice.get_db_obj().getPossibleTangoHosts();
            for (String tangoHost : tangoHosts) {
                tangoHost = "tango://" + tangoHost;
                for (String possibleTangoHost : possibleTangoHosts) {
                    if (possibleTangoHost.equals(tangoHost))
                        possibleTangoHosts.add(tangoHost);
                }
            }
        }
    }

    private boolean reSubscribe(EventChannelStruct channelStruct, EventCallBackStruct eventCallBackStruct) {
        //  ToDo
        boolean done = false;
        try {
            logger.debug("====================================================\n" +
                                "   Try to resubscribe {}", eventCallBackStruct.channel_name);
            DeviceData argOut = ZMQutils.getEventSubscriptionInfoFromAdmDevice(
                        channelStruct.adm_device_proxy,
                        eventCallBackStruct.device.name(),
                        eventCallBackStruct.attr_name, eventCallBackStruct.event_name);
            DevVarLongStringArray lsa = checkZmqAddress(argOut, eventCallBackStruct.device).extractLongStringArray();

            //  Update the heartbeat time
            String  admDeviceName = channelStruct.adm_device_proxy.name();  //.toLowerCase();
            //  Since Tango 8.1, heartbeat is sent in lower case.
            if (channelStruct.getTangoRelease()>=810)
                admDeviceName = admDeviceName.toLowerCase();
            push_structured_event_heartbeat(admDeviceName);
            channelStruct.heartbeat_skipped = false;
            channelStruct.last_subscribed = System.currentTimeMillis();
            channelStruct.setTangoRelease(lsa.lvalue[0]);
            channelStruct.setIdlVersion(lsa.lvalue[1]);
            eventCallBackStruct.last_subscribed = channelStruct.last_subscribed;
            done = true;
        }
        catch(DevFailed e) {
            logger.warn(DevFailedUtils.toString(e));
        }
        return done;
    }

    void checkIfHeartbeatSkipped(String name, EventChannelStruct channelStruct) {
            // Check if heartbeat have been skipped, can happen if
            // 1- the server is dead
            // 2- The network was down;
            // 3- The server has been restarted on another host.

        if (KeepAliveThread.heartbeatHasBeenSkipped(channelStruct)) {
            DevError    dev_error = null;
            try{
                String  admDeviceName = channelStruct.adm_device_proxy.fullName();  //.toLowerCase();
                //  Since Tango 8.1, heartbeat is sent in lower case.
                if (channelStruct.getTangoRelease()>=810)
                    admDeviceName = admDeviceName.toLowerCase();
                channelStruct.adm_device_proxy = new DeviceProxy(admDeviceName);
                channelStruct.adm_device_proxy.set_timeout_millis(300);
                channelStruct.adm_device_proxy.ping();
                reconnectToChannel(name);
            }
            catch (DevFailed e) {
                dev_error = e.errors[0];
            }

            Enumeration callbackStructs = ZmqEventConsumer.getEventCallbackMap().elements();
            while (callbackStructs.hasMoreElements()) {
                EventCallBackStruct callbackStruct = (EventCallBackStruct) callbackStructs.nextElement();
                if (callbackStruct.channel_name.equals(name)) {
                    //	Push exception
                    if (dev_error != null)
                        pushReceivedException(channelStruct, callbackStruct, dev_error);
                    else {
                        if (!reconnectToEvent(channelStruct, callbackStruct)) {
                            dev_error = new DevError("API_NoHeartbeat",
                                    ErrSeverity.ERR, "No heartbeat from " +
                                    channelStruct.adm_device_proxy.get_name(),
                                    "ZmqEventConsumer.checkIfHeartbeatSkipped()");
                            pushReceivedException(channelStruct, callbackStruct, dev_error);
                        }
                    }
                }
            }
        }
    }

    private void unsubscribeTheEvent(EventCallBackStruct callbackStruct) throws DevFailed {
        ZMQutils.disConnectEvent(callbackStruct.device.get_tango_host(),
                callbackStruct.device.name(),
                callbackStruct.attr_name,
                callbackStruct.device.get_idl_version(),
                callbackStruct.event_name);
    }

    /**
     * Reconnect to event
     *
     * @return true if reconnection done
     */
    private boolean reconnectToEvent(EventChannelStruct channelStruct, EventCallBackStruct callBackStruct) {
        boolean reConnected;
        try {
            DeviceData argOut = ZMQutils.getEventSubscriptionInfoFromAdmDevice(
                        channelStruct.adm_device_proxy,
                        callBackStruct.device.name(),
                        callBackStruct.attr_name,
                        callBackStruct.event_name);
            DevVarLongStringArray   lsa = checkZmqAddress(argOut, callBackStruct.device).extractLongStringArray();

            //  Build the buffer to connect event and send it
            ZMQutils.connectEvent(callBackStruct.device.get_tango_host(),
                    callBackStruct.device.name(),
                    callBackStruct.attr_name, lsa,
                    callBackStruct.event_name, true);
            reConnected = true;
        }
        catch (DevFailed e) {
            logger.warn(DevFailedUtils.toString(e));
            reConnected = false;
        }
        return reConnected;
    }

    /**
     * Reconnect to channel
     *
     * @param name channel name
     * @return true if reconnection done
     */
    private boolean reconnectToChannel(String name) {
        boolean reConnected = false;
        Enumeration callbackStructs = event_callback_map.elements();
        while (callbackStructs.hasMoreElements()) {
            EventCallBackStruct eventCallBackStruct = (EventCallBackStruct) callbackStructs.nextElement();
            if (eventCallBackStruct.channel_name.equals(name) && (eventCallBackStruct.callback != null)) {
                try {
                    EventChannelStruct channelStruct = channel_map.get(name);
                    DeviceData argOut = ZMQutils.getEventSubscriptionInfoFromAdmDevice(
                            channelStruct.adm_device_proxy, eventCallBackStruct.device.name(),
                            eventCallBackStruct.attr_name, eventCallBackStruct.event_name);
                    DevVarLongStringArray lsa = checkZmqAddress(
                            argOut, eventCallBackStruct.device).extractLongStringArray();

                    //  Re Connect heartbeat
                    ZMQutils.connectHeartbeat(channelStruct.adm_device_proxy.get_tango_host(),
                                channelStruct.adm_device_proxy.name(), lsa, true);
                    reConnected = true;
                } catch (DevFailed e1) {
                    //Except.print_exception(e1);
                    reConnected = false;
                }
                break;
            }
        }
        return reConnected;
    }

    private EventChannelStruct getEventChannelStruct(String channelName) {
        if (channel_map.containsKey(channelName)) {
            return channel_map.get(channelName);
        }
        //  Check with other TangoHosts using possibleTangoHosts as header
        int index = channelName.indexOf("//");
        if (index>0) {
            index = channelName.indexOf('/', index+2); //  "//".length()
            for (String possibleTangoHost : possibleTangoHosts) {
                String key = possibleTangoHost + channelName.substring(index);
                if (channel_map.containsKey(key))
                    return channel_map.get(key);
            }
        }
        return null;
    }

    /*private*/ void push_structured_event_heartbeat(String channelName) {
        //  ToDo
        try {
            //  If full name try to get structure from channel_map with different tango hosts
            if (channelName.startsWith("tango://")) {
                EventChannelStruct  eventChannelStruct = getEventChannelStruct(channelName);
                if (eventChannelStruct!=null) {
                    eventChannelStruct.last_heartbeat = System.currentTimeMillis();
                    return;
                }
            }

            //  Not Found !
            //	In case of (use_db==false)
            //	domain name is only device name
            //	but key is full name (//host:port/a/b/c....)
            Enumeration keys = channel_map.keys();
            boolean found = false;
            while (keys.hasMoreElements() && !found) {
                String name = (String) keys.nextElement();
                EventChannelStruct eventChannelStruct = channel_map.get(name);
                //  Check with device name for adm device
                String  admDeviceName = eventChannelStruct.adm_device_proxy.name();
                if (admDeviceName.equalsIgnoreCase(channelName)) {
                    eventChannelStruct.last_heartbeat = System.currentTimeMillis();
                    found = true;
                }
            }
            if (!found)
                logger.warn(channelName + " Not found");
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void callEventSubscriptionAndConnect(DeviceProxy device,
                                                 String attribute,
                                                 String eventType) throws DevFailed {

        String device_name = device.name();
        if (attribute==null) // Interface change event on device.
            attribute = "";
        String[] info = new String[] {
                device_name,
                attribute,
                "subscribe",
                eventType,
                Integer.toString(device.get_idl_version())
        };
        DeviceData argIn = new DeviceData();
        argIn.insert(info);
        String cmdName = ZMQutils.SUBSCRIBE_COMMAND;
        logger.debug("{}.command_inout({}) for {}/{}.{}", device.get_adm_dev().name(), cmdName, device_name, attribute, eventType);
        DeviceData argOut = device.get_adm_dev().command_inout(cmdName, argIn);
        logger.trace("    command_inout done.");

        //	And then connect to device
        checkDeviceConnection(device, attribute, argOut, eventType);
    }

    public int subscribe_event(DeviceProxy device,
                               String attribute,
                               int event,
                               CallBack callback,
                               String[] filters,
                               boolean stateless)
            throws DevFailed {
        return subscribe_event(device, attribute, event, callback, -1, filters, stateless);
    }

    public int subscribe_event(DeviceProxy device,
                               String attribute,
                               int event,
                               int max_size,
                               String[] filters,
                               boolean stateless)
            throws DevFailed {
        return subscribe_event(device, attribute, event, null, max_size, filters, stateless);
    }

    /**
     *
     * @param device
     * @param attribute
     * @param event
     * @param callback
     * @param max_size
     * @param filters
     * @param stateless -- throw DevFailed if subscription has failed
     * @return
     * @throws DevFailed
     */
    public int subscribe_event(DeviceProxy device,
                               String attribute,
                               int event,
                               CallBack callback,
                               int max_size,
                               String[] filters,
                               boolean stateless)
            throws DevFailed {
        //	Set the event name;
        String event_name = TangoConst.eventNames[event];
        logger.debug("=============> subscribing for {}{}.{}", device.name(),((attribute==null)? "" : "/" + attribute), event_name);

        //	if no callback (null), create EventQueue
        if (callback == null && max_size >= 0) {
            //	Check if already created (in case of reconnection stateless mode)
            if (device.getEventQueue() == null)
                if (max_size > 0)
                    device.setEventQueue(new EventQueue(max_size));
                else
                    device.setEventQueue(new EventQueue());
        }

        String deviceName = device.fullName();
        String callback_key = deviceName.toLowerCase();
        try {
            //  Check idl version
            if (device.get_idl_version()>=5) {
                switch (event_name) {
                    //TODO enum
                    case "intr_change":
                        //  No IDL for interface change
                        callback_key += "." + event_name;
                        break;
                    case "pipe":
                        //    No IDL for pipe
                        callback_key += "/" + attribute + "." + event_name;
                        break;
                    default:
                        callback_key += "/" + attribute + ".idl" + device.get_idl_version() + "_" + event_name;
                        break;
                }
            }
            else
                callback_key += "/" + attribute + "." + event_name;

            //	Inform server that we want to subscribe and try to connect
            logger.trace("calling callEventSubscriptionAndConnect() method");
            String  att = (attribute==null)? null : attribute.toLowerCase();
            callEventSubscriptionAndConnect(device, att, event_name);
            logger.trace("call callEventSubscriptionAndConnect() method done");
        } catch (DevFailed e) {
            //  re throw if not stateless
            if (!stateless || e.errors[0].desc.equals(ZMQutils.SUBSCRIBE_COMMAND_NOT_FOUND)) {
                throw e;
            }
            else {
                //	Build Event CallBack Structure and add it to map
                subscribe_event_id++;
                EventCallBackStruct new_event_callback_struct =
                        new EventCallBackStruct(device,
                                attribute,
                                event_name,
                                "",
                                callback,
                                max_size,
                                subscribe_event_id,
                                event,
//                                "",
//                                -1,
                                filters,
                                false);
                failed_event_callback_map.put(callback_key, new_event_callback_struct);
                return subscribe_event_id;
            }
        }

        //	Prepare filters for heartbeat events on channelName
        String channelName = device_channel_map.get(deviceName);
        if (channelName==null) {
            //  If from notifd, tango host not used.
            int start = deviceName.indexOf('/', "tango:// ".length());
            deviceName = deviceName.substring(start+1);
            channelName = device_channel_map.get(deviceName);
        }
        EventChannelStruct event_channel_struct = channel_map.get(channelName);
        event_channel_struct.last_subscribed = System.currentTimeMillis();

        //	Check if a new event or a re-trying one
        int evnt_id;
        EventCallBackStruct failed_struct = failed_event_callback_map.get(callback_key);
        if (failed_struct == null) {
            //	It is a new one
            subscribe_event_id++;
            evnt_id = subscribe_event_id;
        } else
            evnt_id = failed_struct.id;

        //	Build Event CallBack Structure if any
        EventCallBackStruct new_event_callback_struct =
                new EventCallBackStruct(device,
                        attribute,
                        event_name,
                        channelName,
                        callback,
                        max_size,
                        evnt_id,
                        event,
//                        constraint_expr,
//                        filter_id,
                        filters,
                        true);
        setAdditionalInfoToEventCallBackStruct(new_event_callback_struct,
                deviceName, attribute, event_name, filters, event_channel_struct);
        event_callback_map.put(callback_key, new_event_callback_struct);

        return evnt_id;
    }

    private void removeCallBackStruct(Hashtable map, EventCallBackStruct cb_struct) {
        String callback_key = cb_struct.device.name().toLowerCase() +
                "/" + cb_struct.attr_name + "." + cb_struct.event_name;
        map.remove(callback_key);
    }

    public void unsubscribe_event(int event_id) throws DevFailed {
        //	Get callback struct for event ID
        EventCallBackStruct callbackStruct =
                getCallBackStruct(event_callback_map, event_id);
        if (callbackStruct != null) {
            removeCallBackStruct(event_callback_map, callbackStruct);
            unsubscribeTheEvent(callbackStruct);
        }
        else {
            //	If not found check if in failed map
            callbackStruct =
                    getCallBackStruct(failed_event_callback_map, event_id);
            if (callbackStruct != null)
                removeCallBackStruct(failed_event_callback_map, callbackStruct);
            else
                Except.throw_event_system_failed("API_EventNotFound",
                        "Failed to unsubscribe event, the event id (" + event_id +
                                ") specified does not correspond with any known one",
                        "EventConsumer.unsubscribe_event()");
        }
    }

    /*
     * Re subscribe event selected by name
     */
     void reSubscribeByName(EventChannelStruct event_channel_struct, String name) {
        Enumeration callback_structs = event_callback_map.elements();
        while (callback_structs.hasMoreElements()) {
            EventCallBackStruct callback_struct = (EventCallBackStruct) callback_structs.nextElement();
            if (callback_struct.channel_name.equals(name)) {
                reSubscribe(event_channel_struct, callback_struct);
            }
        }
    }

    /*
     * Push event containing exception
     */
    private void pushReceivedException(EventChannelStruct event_channel_struct, EventCallBackStruct callback_struct, DevError error) {
        int eventSource = EventData.ZMQ_EVENT;
        DevError[] errors = {error};
        String domain_name = callback_struct.device.name();
        if (callback_struct.attr_name!=null)
            domain_name += "/" + callback_struct.attr_name.toLowerCase();
        EventData event_data =
                new EventData(event_channel_struct.adm_device_proxy,
                        domain_name, callback_struct.event_name, callback_struct.event_type,
                        eventSource, null, null, null, null, null, errors);

        CallBack callback = callback_struct.callback;
        event_data.device = callback_struct.device;
        event_data.name = callback_struct.device.name();
        event_data.event = callback_struct.event_name;

        if (callback_struct.use_ev_queue) {
            EventQueue ev_queue = callback_struct.device.getEventQueue();
            ev_queue.insert_event(event_data);
        } else
            callback.push_event(event_data);
    }

    protected static class ConnectionStructure {
        String      tangoHost;
        String      channelName;
        String      attributeName;
        String      deviceName;
        String      eventName;
        Database    database;
        DeviceData  deviceData = null;
        boolean     reconnect = false;
        //===========================================================
        ConnectionStructure(String tangoHost,
                            String channelName,
                            String deviceName,
                            String attributeName,
                            String eventName,
                            Database database,
                            DeviceData deviceData,
                            boolean reconnect) {
            this.tangoHost      = tangoHost;
            this.channelName    = channelName;
            this.deviceName     = deviceName;
            this.attributeName  = attributeName;
            this.eventName      = eventName;
            this.database          = database;
            this.deviceData     = deviceData;
            this.reconnect      = reconnect;
        }
        //===========================================================
        ConnectionStructure(String tangoHost, String name, Database dbase, boolean reconnect) {
            this(tangoHost, name, null, null, null, dbase, null, reconnect);
        }
        //===========================================================
        public String toString() {
            return "channel name: " + channelName +
                 "\ndatabase:     " + database +
                 "\nreconnect:    " + reconnect;
        }
        //===========================================================
    }

    public static List<String> getPossibleTangoHosts() {
        return possibleTangoHosts;
    }
}
