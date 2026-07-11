package com.xyo.financial;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DefaultHttpTransport implements HttpTransport {

    private final HttpClient client;
    private final ClientConfig config;

    public DefaultHttpTransport(ClientConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public com.xyo.financial.HttpResponse send(com.xyo.financial.HttpRequest request) throws XyoException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofMillis(config.getRequestTimeoutMs()));

            if (request.getMethod().equalsIgnoreCase("POST")) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getBody()));
            } else if (request.getMethod().equalsIgnoreCase("GET")) {
                requestBuilder.GET();
            }

            if (request.getHeaders() != null) {
                for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
                    for (String value : entry.getValue()) {
                        requestBuilder.header(entry.getKey(), value);
                    }
                }
            }

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return new com.xyo.financial.HttpResponse(response.statusCode(), response.body());

        } catch (IOException e) {
            throw new XyoException(ErrorCategory.TRANSPORT, "Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XyoException(ErrorCategory.TRANSPORT, "Request interrupted", e);
        }
    }
}
