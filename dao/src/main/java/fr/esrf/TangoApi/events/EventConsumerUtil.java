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

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.CallBack;
import fr.esrf.TangoApi.DeviceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to manage NotifdEventConsumer and ZmqEventConsumer instances
 *
 * @author pascal_verdier
 */
public class EventConsumerUtil {
    private final Logger logger = LoggerFactory.getLogger(EventConsumerUtil.class);

    private static final EventConsumerUtil  instance = new EventConsumerUtil(new ZmqEventConsumer());

    private final ZmqEventConsumer consumer;

    /**
     * Create a singleton if not already done
     * @return an instance on this singleton
     */
    public static EventConsumerUtil getInstance() {
        return instance;
    }

    /**
     * Default constructor
     * @param consumer
     */
    private EventConsumerUtil(ZmqEventConsumer consumer) {
        this.consumer = consumer;
        //  Start the KeepAliveThread loop
        new KeepAliveThread(consumer).start();
        //  Start ZMQ main thread
        new ZmqMainThread(ZmqUtils.getContext(), consumer).start();
    }

    /**
     *
     * @param deviceProxy device to be connected
     * @return consumer if already connected, otherwise return null
     */
    private ZmqEventConsumer isChannelAlreadyConnected(DeviceProxy deviceProxy) {
        try {
            String adminName = deviceProxy.adm_name();
            EventChannelStruct eventChannelStruct = consumer.getChannelMap().get(adminName);
            if (eventChannelStruct==null) {
                return null;
            }
            else {
                return eventChannelStruct.consumer;
            }
        }
        catch (DevFailed e) {
            return null;
        }
    }

    /**
     * Subscribe on specified event
     *
     * @param device    specified device
     * @param attribute specified attribute
     * @param event     specified event type
     * @param callback  callback class
     * @param filters   filters (not used with zmq)
     * @param stateless stateless subscription if true
     * @return  event ID.
     * @throws DevFailed if subscription failed
     */
    public int subscribe_event(DeviceProxy device,
                               String attribute,
                               int event,
                               CallBack callback,
                               String[] filters,
                               boolean stateless)
            throws DevFailed {
        return subscribe_event(device, attribute, event, callback, -1, filters, stateless);
    }

    //===============================================================
    /**
     * Subscribe on specified event
     *
     * @param device    specified device
     * @param attribute specified attribute
     * @param event     specified event type
     * @param max_size  maximum size for event queue
     * @param filters   filters (not used with zmq)
     * @param stateless stateless subscription if true
     * @return  event ID.
     * @throws DevFailed if subscription failed
     */
    //===============================================================
    public int subscribe_event(DeviceProxy device,
                               String attribute,
                               int event,
                               int max_size,
                               String[] filters,
                               boolean stateless)
            throws DevFailed {
        return subscribe_event(device, attribute, event, null, max_size, filters, stateless);
    }

    //===============================================================
    /**
     * Subscribe on specified event
     *
     * @param device    specified device
     * @param attribute specified attribute
     * @param event     specified event type
     * @param callback  callback class
     * @param max_size  maximum size for event queue
     * @param filters   filters (not used with zmq)
     * @param stateless stateless subscription if true
     * @return  event ID.
     * @throws DevFailed if subscription failed
     */
    //===============================================================
    public int subscribe_event(DeviceProxy device,
                               String attribute,
                               int event,
                               CallBack callback,
                               int max_size,
                               String[] filters,
                               boolean stateless) throws DevFailed {
        ApiUtil.printTrace("trying to subscribe_event to " + device.name() + "/" + attribute);
        //  If already connected, subscribe directly on same channel
        ZmqEventConsumer consumer = isChannelAlreadyConnected(device);
        if (consumer!=null) {
            return consumer.subscribe_event(device,
                    attribute, event, callback, max_size, filters, stateless);
        }
        else {
            return this.consumer.subscribe_event(device,
                    attribute, event, callback, max_size, filters, stateless);
        }
    }
    //===============================================================
    /**
     * Subscribe on specified event
     *
     * @param device    specified device
     * @param event     specified event type
     * @param callback  callback class
     * @param max_size  maximum size for event queue
     * @param stateless stateless subscription if true
     * @return  event ID.
     * @throws DevFailed if subscription failed
     */
    //===============================================================
    public int subscribe_event(DeviceProxy device,
                               int event,
                               CallBack callback,
                               int max_size,
                               boolean stateless) throws DevFailed {
        ApiUtil.printTrace("INTERFACE_CHANGE: trying to subscribe_event to " + device.name());
        int id;
        //  If already connected, subscribe directly on same channel
        ZmqEventConsumer consumer = isChannelAlreadyConnected(device);
        if (consumer!=null) {
            return consumer.subscribe_event(device, event, callback, max_size, stateless);
        }

        //  If ZMQ jni library can be loaded, try to connect on ZMQ event system
        id = this.consumer.subscribe_event(
                device, event, callback, max_size, stateless);

        return id;
    }
    //===============================================================
    /**
     * Un subscribe event
     * @param event_id  specified event ID
     * @throws DevFailed if un subscribe failed or event not found
     */
    //===============================================================
    public void unsubscribe_event(int event_id) throws DevFailed {
        consumer.unsubscribe_event(event_id);
    }
    //===============================================================
    //===============================================================
}
