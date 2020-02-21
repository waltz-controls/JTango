package org.tango.transport;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.net.http.HttpClient.Version.HTTP_2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.02.2020
 */
public class HttpTransport implements Transport {
    private HttpClient client;
    private URI uri;

    @Override
    public boolean isConnected() {
        return uri != null;
    }

    @Override
    public void connect(String endpoint) throws IOException {
        uri = URI.create(endpoint);
        client = HttpClient.newBuilder()
                // just to show off; HTTP/2 is the default
                .version(HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void disconnect(String endpoint) throws IOException {
    }

    @Override
    public byte[] send(byte[] data) throws IOException {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
        return new byte[0];
    }

    @Override
    public void close() throws IOException {

    }
}
