package com.xyo.financial;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Standard implementation of {@link HttpTransport} wrapping {@link HttpClient}.
 */
public class DefaultHttpTransport implements HttpTransport {

    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(ClientConfig.DEFAULT_CONNECT_TIMEOUT_MS))
            .build();

    private final HttpClient client;
    private final long requestTimeoutMs;
    private final long maxResponseBytes;

    /**
     * Constructs a new DefaultHttpTransport with the given {@link ClientConfig}.
     * 
     * @param config the configuration properties
     */
    public DefaultHttpTransport(ClientConfig config) {
        this.requestTimeoutMs = config.getRequestTimeoutMs();
        this.maxResponseBytes = config.getMaxResponseBytes();

        if (config.getHttpClient() != null) {
            this.client = config.getHttpClient();
        } else if (config.getConnectTimeoutMs() == ClientConfig.DEFAULT_CONNECT_TIMEOUT_MS) {
            this.client = DEFAULT_HTTP_CLIENT;
        } else {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                    .build();
        }
    }

    @Override
    public com.xyo.financial.HttpResponse send(com.xyo.financial.HttpRequest request) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofMillis(requestTimeoutMs));

            if (request.getMethod().equalsIgnoreCase("POST")) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getBody()));
            } else if (request.getMethod().equalsIgnoreCase("GET")) {
                requestBuilder.GET();
            } else {
                throw new XyoException(ErrorCategory.VALIDATION, "Unsupported HTTP method: " + request.getMethod());
            }

            if (request.getHeaders() != null) {
                for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
                    for (String value : entry.getValue()) {
                        requestBuilder.header(entry.getKey(), value);
                    }
                }
            }

            HttpResponse<InputStream> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream is = new BoundedInputStream(response.body(), maxResponseBytes)) {
                String bodyString = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return new com.xyo.financial.HttpResponse(response.statusCode(), bodyString, response.headers().map());
            }

        } catch (IOException e) {
            throw new XyoException(ErrorCategory.TRANSPORT, "Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XyoException(ErrorCategory.TRANSPORT, "Request interrupted", e);
        }
    }

    /**
     * Bounded stream reader wrapping an {@link InputStream} that prevents reading 
     * more than the allowed limit to protect against OOM situations.
     */
    private static class BoundedInputStream extends InputStream {
        private final InputStream in;
        private final long maxBytes;
        private long bytesRead = 0;

        public BoundedInputStream(InputStream in, long maxBytes) {
            this.in = in;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            if (bytesRead >= maxBytes) {
                int b = in.read();
                if (b == -1) {
                    return -1;
                }
                throw new IOException("Response body exceeded maximum allowed size of " + maxBytes + " bytes");
            }
            int b = in.read();
            if (b != -1) {
                incrementBytes(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bytesRead >= maxBytes) {
                int check = in.read();
                if (check == -1) {
                    return -1;
                }
                throw new IOException("Response body exceeded maximum allowed size of " + maxBytes + " bytes");
            }
            long remaining = maxBytes - bytesRead;
            int maxToRead = (int) Math.min(len, remaining + 1);
            int read = in.read(b, off, maxToRead);
            if (read != -1) {
                incrementBytes(read);
            }
            return read;
        }

        private void incrementBytes(int count) throws IOException {
            bytesRead += count;
            if (bytesRead > maxBytes) {
                throw new IOException("Response body exceeded maximum allowed size of " + maxBytes + " bytes");
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
