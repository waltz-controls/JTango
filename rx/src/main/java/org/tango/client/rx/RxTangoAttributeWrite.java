package org.tango.client.rx;

import org.tango.client.ez.proxy.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;

/**
 * @author ingvord
 * @since 24.06.2020
 */
public class RxTangoAttributeWrite<T> extends RxTango<T> {
    private final T value;

    public RxTangoAttributeWrite(TangoProxy proxy, String name, T value) {
        super(proxy, name);
        this.value = value;
    }

    public RxTangoAttributeWrite(String device, String name, T value) throws Exception {
        this(TangoProxies.newDeviceProxyWrapper(device), name, value);
    }

    @Override
    protected Future<T> getFuture() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                proxy.writeAttribute(name, value);
                return proxy.readAttribute(name);
            } catch (WriteAttributeException | ReadAttributeException | NoSuchAttributeException e) {
                throw new CompletionException(e);
            }
        });
    }
}
