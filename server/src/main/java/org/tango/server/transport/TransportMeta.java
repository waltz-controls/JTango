package org.tango.server.transport;

import com.google.common.collect.Lists;
import fr.esrf.Tango.DevVarLongStringArray;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.02.2020
 */
public class TransportMeta {
    private final List<String> connectionPoints = Lists.newArrayList();

    public TransportMeta() {
    }


    public void addConnectionPoint(String connectionPoint) {
        connectionPoints.add(connectionPoint);
    }

    public DevVarLongStringArray toDevVarLongStringArray() {
        DevVarLongStringArray result = new DevVarLongStringArray(new int[0],
                connectionPoints.toArray(new String[0]));

        return result;
    }
}
