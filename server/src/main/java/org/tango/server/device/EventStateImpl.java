package org.tango.server.device;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.03.2020
 */
public class EventStateImpl {
    private final StateImpl inner;

    public EventStateImpl(EventSystem eventSystem, StateImpl inner) {
        this.inner = inner;
    }


}
