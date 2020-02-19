package org.tango.server.transport;

import org.tango.transport.Message;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public interface ZmqMessageProcessor {
    Message process();
}
