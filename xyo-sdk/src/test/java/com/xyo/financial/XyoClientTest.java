package com.xyo.financial;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class XyoClientTest {

    private HttpServer testServer;
    private int testServerPort;
    private XyoClient client;
    private final AtomicReference<Throwable> handlerException = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        handlerException.set(null);
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
        Throwable backgroundException = handlerException.get();
        if (backgroundException != null) {
            fail("HttpServer handler threw exception: " + backgroundException.getMessage());
        }
    }

    private void startTestServer(HttpHandler handler) throws IOException {
        testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        testServer.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } catch (Throwable t) {
                handlerException.set(t);
                exchange.sendResponseHeaders(500, -1);
            }
        });
        testServer.start();
        testServerPort = testServer.getAddress().getPort();
    }

    private XyoClient createTestClient() {
        ClientConfig config = new ClientConfig.Builder("test-api-key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .build();
        return new XyoClient(config);
    }

    @Test
    void testEnrichTransaction_Success() throws IOException {
        String jsonResponse = "{\n" +
                "  \"merchant\": \"Test Merchant\",\n" +
                "  \"description\": \"Test Description\",\n" +
                "  \"categories\": [\"Food\", \"Coffee\"],\n" +
                "  \"logo\": \"https://example.com/logo.png\",\n" +
                "  \"location\": \"London\",\n" +
                "  \"address\": \"123 Baker St\"\n" +
                "}";

        startTestServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/v1/ai/finance/enrichment/transaction", exchange.getRequestURI().getPath());
            assertEquals("Bearer test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(requestBody.contains("\"content\":\"COSTA PICKUP\""));
            assertTrue(requestBody.contains("\"countryCode\":\"GB\""));

            byte[] resBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        EnrichmentRequest request = new EnrichmentRequest("COSTA PICKUP", "GB");
        EnrichmentResponse response = client.enrichTransaction(request);

        assertNotNull(response);
        assertEquals("Test Merchant", response.getMerchant());
        assertEquals("Test Description", response.getDescription());
        assertEquals(2, response.getCategories().size());
        assertEquals("London", response.getLocation());
    }

    @Test
    void testEnrichTransaction_ApiError() throws IOException {
        String errorJson = "{\"error\": \"Bad request\"}";
        startTestServer(exchange -> {
            byte[] resBytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        EnrichmentRequest request = new EnrichmentRequest("Test", "GB");

        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request);
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(errorJson, exception.getResponseBody());
    }

    @Test
    void testEnrichTransaction_ServerError500() throws IOException {
        String errorBody = "Internal Server Error";
        startTestServer(exchange -> {
            byte[] resBytes = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        EnrichmentRequest request = new EnrichmentRequest("Costa", "GB");

        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request);
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(500, exception.getHttpStatusCode());
        assertEquals("Internal Server Error", exception.getResponseBody());
    }

    @Test
    void testEnrichTransactionCollection_Success() throws IOException {
        String jsonResponse = "{\n" +
                "  \"id\": \"batch-123\",\n" +
                "  \"link\": \"https://api.xyo.financial/v1/enrich/bulk/batch-123\"\n" +
                "}";

        startTestServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/v1/ai/finance/enrichment/transactions", exchange.getRequestURI().getPath());
            assertEquals("Bearer test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));

            byte[] resBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        EnrichmentRequest req1 = new EnrichmentRequest("COSTA", "GB");
        EnrichmentRequest req2 = new EnrichmentRequest("TESCO", "GB");
        EnrichTransactionCollectionResponse response = client.enrichTransactionCollection(List.of(req1, req2));

        assertNotNull(response);
        assertEquals("batch-123", response.getId());
        assertEquals("https://api.xyo.financial/v1/enrich/bulk/batch-123", response.getLink());
    }

    @Test
    void testEnrichTransactionCollectionStatus_Ready() throws IOException {
        String jsonResponse = "{\"status\": \"READY\"}";
        startTestServer(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("/v1/ai/finance/enrichment/status/batch-123", exchange.getRequestURI().getPath());
            assertEquals("Bearer test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));

            byte[] resBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        EnrichmentCollectionStatus status = client.enrichTransactionCollectionStatus("batch-123");

        assertEquals(EnrichmentCollectionStatus.READY, status);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testClientConfigValidation() {
        assertThrows(XyoException.class, () -> new XyoClient(null));
        assertThrows(XyoException.class, () -> new XyoClient(new ClientConfig(null)));
        assertThrows(XyoException.class, () -> new XyoClient(new ClientConfig("")));

        ClientConfig confNoUrl = new ClientConfig("key");
        confNoUrl.setApiBaseUrl("");
        assertThrows(XyoException.class, () -> new XyoClient(confNoUrl));
    }

    @Test
    void testClientConfigBuilder() {
        ClientConfig buildConfig = new ClientConfig.Builder("key-from-builder")
                .apiBaseUrl("https://api2.xyo.financial")
                .connectTimeoutMs(8000)
                .requestTimeoutMs(45000)
                .maxResponseBytes(500000)
                .allowInsecureHttp(true)
                .build();

        assertEquals("key-from-builder", buildConfig.getApiKey());
        assertEquals("https://api2.xyo.financial", buildConfig.getApiBaseUrl());
        assertEquals(8000, buildConfig.getConnectTimeoutMs());
        assertEquals(45000, buildConfig.getRequestTimeoutMs());
        assertEquals(500000, buildConfig.getMaxResponseBytes());
        assertTrue(buildConfig.isAllowInsecureHttp());

        XyoClient clientFromBuilder = new XyoClient(buildConfig);
        assertNotNull(clientFromBuilder);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testClientConfigDoesNotMutate() {
        ClientConfig mutableConfig = new ClientConfig("key");
        mutableConfig.setApiBaseUrl("https://api.xyo.financial///");

        new XyoClient(mutableConfig);

        // Assert that the original config remains untouched
        assertEquals("https://api.xyo.financial///", mutableConfig.getApiBaseUrl());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEnforceSecureHttp() {
        ClientConfig insecureConf = new ClientConfig("key");
        insecureConf.setApiBaseUrl("http://api.xyo.financial");
        insecureConf.setAllowInsecureHttp(false);

        XyoException exception = assertThrows(XyoException.class, () -> new XyoClient(insecureConf));
        assertEquals(ErrorCategory.VALIDATION, exception.getCategory());

        // Should allow if explicitly configured
        insecureConf.setAllowInsecureHttp(true);
        assertDoesNotThrow(() -> new XyoClient(insecureConf));
    }

    @Test
    void testEnrichmentRequestValidation() {
        ClientConfig dummyConfig = new ClientConfig.Builder("key").apiBaseUrl("https://api.xyo.financial").build();
        client = new XyoClient(dummyConfig);

        assertThrows(XyoException.class, () -> client.enrichTransaction(null));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest(null, "GB")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("", "GB")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", null)));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", "")));
    }

    @Test
    void testEnrichmentCollectionValidation() {
        ClientConfig dummyConfig = new ClientConfig.Builder("key").apiBaseUrl("https://api.xyo.financial").build();
        client = new XyoClient(dummyConfig);

        assertThrows(XyoException.class, () -> client.enrichTransactionCollection(null));
        assertThrows(XyoException.class, () -> client.enrichTransactionCollection(Collections.emptyList()));
        assertThrows(XyoException.class, () -> client.enrichTransactionCollection(List.of(new EnrichmentRequest(null, "GB"))));

        // List containing a null request item
        java.util.ArrayList<EnrichmentRequest> listWithNull = new java.util.ArrayList<>();
        listWithNull.add(new EnrichmentRequest("TESCO", "GB"));
        listWithNull.add(null);
        assertThrows(XyoException.class, () -> client.enrichTransactionCollection(listWithNull));
    }

    @Test
    void testMissingStatusKey() throws IOException {
        startTestServer(exchange -> {
            byte[] resBytes = "{\"wrong_key\": \"READY\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });
        assertEquals(ErrorCategory.PARSING, exception.getCategory());
        assertTrue(exception.getMessage().contains("Response is missing the required 'status' key"));
    }

    @Test
    void testEnrichmentCollectionStatus_NullValueThrows() throws IOException {
        startTestServer(exchange -> {
            byte[] resBytes = "{\"status\": null}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });
    }

    @Test
    void testHttpErrorPreserved() throws IOException {
        String errorJson = "{\"error_code\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}";
        startTestServer(exchange -> {
            byte[] responseBytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(429, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(429, exception.getHttpStatusCode());
        assertEquals(errorJson, exception.getResponseBody());
    }
}
