package org.tango.client.rx;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.07.2020
 */
public class TestClient {
    private static final int NUMBER_OF_CLIENTS = 64;
    private static final long FIFTEEN_SECONDS = 15_000;

    @Test
    @Disabled
    public void testMagix() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_CLIENTS);

        final AtomicLong counter = new AtomicLong(0L);
        final AtomicLong errors = new AtomicLong(0L);

        final AtomicBoolean finish = new AtomicBoolean(false);

        for (int i = 0; i < NUMBER_OF_CLIENTS; ++i) {
            executorService.submit(() -> {
                try {
                    TangoProxy proxy = TangoProxies.newDeviceProxyWrapper("tango://hzgxenvtest:10000/development/benchmark/0");

                    while (!finish.get()) {
                        Observable
                                .fromPublisher(new RxTangoAttributeWrite<>(proxy, "BenchmarkScalarAttribute", 3.14))
                                .subscribe(aDouble -> {
                                    counter.incrementAndGet();
                                }, throwable -> {
                                    errors.incrementAndGet();
                                });
                    }
                } catch (Exception e){
                    errors.incrementAndGet();
                }
            });
        }

        Thread.sleep(FIFTEEN_SECONDS);
        finish.set(true);

        executorService.shutdownNow();

        System.out.println(String.format("Total writes count: %d", counter.get()));
        System.out.println(String.format("Total errors count: %d", errors.get()));
    }

    @Test
    @Disabled
    public void testDirect() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_CLIENTS);

        final AtomicLong counter = new AtomicLong(0L);
        final AtomicLong errors = new AtomicLong(0L);

        final AtomicBoolean finish = new AtomicBoolean(false);

        TangoProxy proxy = TangoProxies.newDeviceProxyWrapper("tango://hzgxenvtest:10000/development/benchmark/0");

        for (int i = 0; i < NUMBER_OF_CLIENTS; ++i) {
            executorService.submit(() -> {
                while (!finish.get()) {
                    try {
                        proxy.writeAttribute("BenchmarkScalarAttribute", 3.14);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }

        Thread.sleep(FIFTEEN_SECONDS);
        finish.set(true);

        executorService.shutdownNow();

        System.out.println(String.format("Total writes count: %d", counter.get()));
        System.out.println(String.format("Total errors count: %d", errors.get()));
    }
}
