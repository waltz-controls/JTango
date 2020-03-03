package org.tango.server.events;

import fr.esrf.Tango.*;
import org.tango.server.attribute.AttributeImpl;
import org.tango.server.pipe.PipeImpl;
import org.tango.server.pipe.PipeValue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.03.2020
 */
public interface EventManager {
    boolean hasSubscriber(String deviceName);

    void close();

    DevVarLongStringArray getInfo();

    DevVarLongStringArray subscribe(String deviceName, PipeImpl pipe) throws DevFailed;

    DevVarLongStringArray subscribe(String deviceName, AttributeImpl attribute,
                                    EventType eventType, int idlVersion) throws DevFailed;

    DevVarLongStringArray subscribe(String deviceName) throws DevFailed;

    void pushAttributeErrorEvent(String deviceName, String attributeName, DevFailed devFailed)
            throws DevFailed;

    void pushAttributeValueEvent(String deviceName, String attributeName) throws DevFailed;

    void pushAttributeValueEvent(String deviceName, String attributeName, EventType eventType)
            throws DevFailed;

    void pushAttributeDataReadyEvent(String deviceName, String attributeName, int counter)
            throws DevFailed;

    void pushAttributeConfigEvent(String deviceName, String attributeName) throws DevFailed;

    void pushInterfaceChangedEvent(String deviceName, DevIntrChange deviceInterface)
            throws DevFailed;

    void pushPipeEvent(String deviceName, String pipeName, PipeValue blob) throws DevFailed;

    void pushPipeEvent(String deviceName, String pipeName, DevFailed devFailed)
            throws DevFailed;

    void pushAttributeValueIDL5Event(String deviceName, String attributeName, AttributeValue_5 value, EventType evtType) throws DevFailed;

    void pushAttributeConfigIDL5Event(String deviceName, String attributeName, AttributeConfig_5 config) throws DevFailed;
}
