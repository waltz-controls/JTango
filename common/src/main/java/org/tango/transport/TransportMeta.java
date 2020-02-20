package org.tango.transport;

import com.google.common.collect.Lists;
import fr.esrf.Tango.DevVarLongStringArray;

import java.util.Arrays;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.02.2020
 */
public class TransportMeta {
    private List<String> endPoints = Lists.newArrayList();

    public TransportMeta() {
    }

    public static TransportMeta fromDevVarLongStringArray(DevVarLongStringArray array) {
        TransportMeta transportMeta = new TransportMeta();
        transportMeta.setEndPoints(Arrays.asList(array.svalue));
        return transportMeta;
    }

    public List<String> getEndPoints() {
        return endPoints;
    }

    public void setEndPoints(List<String> endPoints) {
        this.endPoints = endPoints;
    }

    public void addEndpoint(String connectionPoint) {
        endPoints.add(connectionPoint);
    }

    public DevVarLongStringArray toDevVarLongStringArray() {
        DevVarLongStringArray result = new DevVarLongStringArray(new int[0],
                endPoints.toArray(new String[0]));

        return result;
    }

    public String[] toStringArray() {
        return endPoints.toArray(new String[0]);
    }
}
