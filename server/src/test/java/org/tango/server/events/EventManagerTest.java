package org.tango.server.events;

import fr.esrf.Tango.DevVarLongStringArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10/25/18
 */
public class EventManagerTest {
    private EventManager instance;

    @Before
    public void before() {
        instance = EventManager.getInstance();
    }

    @After
    public void after(){
        instance.close();
    }

    @Test //requires TANGO_HOST
    @Ignore
    public void pubsub() throws Exception {
        DevVarLongStringArray connection = instance.subscribe("tango://hzgxenvtest:10000/development/test_server/0");

        for(String endpoint : connection.svalue){
            System.out.println(endpoint);
        }

    }
}