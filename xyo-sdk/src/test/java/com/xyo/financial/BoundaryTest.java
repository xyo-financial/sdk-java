package com.xyo.financial;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class BoundaryTest {
    private HttpServer testServer;
    private int testServerPort;

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    @Test
    public void testExactBoundary() throws Exception {
        String exactResponseBody = "12345"; // 5 bytes
        testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        testServer.createContext("/", exchange -> {
            byte[] responseBytes = exactResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        testServer.start();
        testServerPort = testServer.getAddress().getPort();

        ClientConfig config = new ClientConfig.Builder("key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .maxResponseBytes(5) // Exactly the length of the response
                .build();
        
        XyoClient client = new XyoClient(config);
        
        // This should not throw an exception!
        client.enrichTransactionCollectionStatus("batch-123");
    }
}
