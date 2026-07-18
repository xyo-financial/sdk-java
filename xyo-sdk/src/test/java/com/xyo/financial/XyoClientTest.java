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

    private ClientConfig config;
    private MockHttpTransport mockTransport;
    private XyoClient client;
    private HttpServer testServer;
    private int testServerPort;
    
    // Captures assertions or other failures thrown inside the HttpServer worker threads
    private final AtomicReference<Throwable> handlerException = new AtomicReference<>();

    // A simple manual mock for HttpTransport to avoid adding Mockito dependency
    static class MockHttpTransport implements HttpTransport {
        volatile HttpRequest lastRequest;
        volatile HttpResponse responseToReturn;

        @Override
        public HttpResponse send(HttpRequest request) {
            this.lastRequest = request;
            if (responseToReturn == null) {
                throw new XyoException(ErrorCategory.TRANSPORT, "No mock response configured");
            }
            return responseToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        handlerException.set(null);
        config = new ClientConfig("test-api-key");
        mockTransport = new MockHttpTransport();
        config.setHttpTransport(mockTransport);
        client = new XyoClient(config);
    }

    @AfterEach
    void tearDown() throws Throwable {
        if (testServer != null) {
            testServer.stop(0);
        }
        
        // Propagate assertion failures thrown on background HttpServer handler threads back to JUnit test runner thread
        Throwable backgroundException = handlerException.get();
        if (backgroundException != null) {
            throw backgroundException;
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

    @Test
    void testEnrichTransaction_Success() {
        String jsonResponse = "{\n" +
                "  \"merchant\": \"Test Merchant\",\n" +
                "  \"description\": \"Test Description\",\n" +
                "  \"categories\": [\"Food\", \"Coffee\"],\n" +
                "  \"logo\": \"https://example.com/logo.png\",\n" +
                "  \"location\": \"London\",\n" +
                "  \"address\": \"123 Baker St\"\n" +
                "}";
        mockTransport.responseToReturn = new HttpResponse(200, jsonResponse);

        EnrichmentRequest request = new EnrichmentRequest("COSTA PICKUP", "GB");
        EnrichmentResponse response = client.enrichTransaction(request);

        assertNotNull(mockTransport.lastRequest);
        assertEquals("POST", mockTransport.lastRequest.getMethod());
        assertEquals("https://api.xyo.financial/v1/enrich", mockTransport.lastRequest.getUrl());
        assertEquals("Bearer test-api-key", mockTransport.lastRequest.getHeaders().get("Authorization").get(0));
        assertTrue(mockTransport.lastRequest.getBody().contains("\"content\":\"COSTA PICKUP\""));
        assertTrue(mockTransport.lastRequest.getBody().contains("\"countryCode\":\"GB\""));

        assertNotNull(response);
        assertEquals("Test Merchant", response.getMerchant());
        assertEquals("Test Description", response.getDescription());
        assertEquals(2, response.getCategories().size());
        assertEquals("London", response.getLocation());
    }

    @Test
    void testEnrichTransaction_ApiError() {
        mockTransport.responseToReturn = new HttpResponse(400, "{\"error\": \"Bad request\"}");

        EnrichmentRequest request = new EnrichmentRequest("Test", "GB");

        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request);
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals("{\"error\": \"Bad request\"}", exception.getResponseBody());
    }

    @Test
    void testEnrichTransaction_ServerError500() {
        mockTransport.responseToReturn = new HttpResponse(500, "Internal Server Error");

        EnrichmentRequest request = new EnrichmentRequest("Costa", "GB");

        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request);
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(500, exception.getHttpStatusCode());
        assertEquals("Internal Server Error", exception.getResponseBody());
    }

    @Test
    void testEnrichTransactionCollection_Success() {
        String jsonResponse = "{\n" +
                "  \"id\": \"batch-123\",\n" +
                "  \"link\": \"https://api.xyo.financial/v1/enrich/bulk/batch-123\"\n" +
                "}";
        mockTransport.responseToReturn = new HttpResponse(200, jsonResponse);

        EnrichmentRequest req1 = new EnrichmentRequest("COSTA", "GB");
        EnrichmentRequest req2 = new EnrichmentRequest("TESCO", "GB");
        EnrichTransactionCollectionResponse response = client.enrichTransactionCollection(List.of(req1, req2));

        assertNotNull(response);
        assertEquals("batch-123", response.getId());
        assertEquals("https://api.xyo.financial/v1/enrich/bulk/batch-123", response.getLink());
    }

    @Test
    void testEnrichTransactionCollectionStatus_Ready() {
        String jsonResponse = "{\"status\": \"READY\"}";
        mockTransport.responseToReturn = new HttpResponse(200, jsonResponse);

        EnrichmentCollectionStatus status = client.enrichTransactionCollectionStatus("batch-123");

        assertEquals(EnrichmentCollectionStatus.READY, status);
        assertTrue(mockTransport.lastRequest.getBody().contains("\"id\":\"batch-123\""));
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
        assertThrows(XyoException.class, () -> client.enrichTransaction(null));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest(null, "GB")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("", "GB")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", null)));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", "")));
    }

    @Test
    void testEnrichmentCollectionValidation() {
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
    void testMissingStatusKey() {
        mockTransport.responseToReturn = new HttpResponse(200, "{\"wrong_key\": \"READY\"}");

        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });
        assertEquals(ErrorCategory.PARSING, exception.getCategory());
        assertTrue(exception.getMessage().contains("Response is missing the required 'status' key"));
    }

    @Test
    void testEnrichmentCollectionStatus_NullValueThrows() {
        // Mock response returning JSON null for status
        mockTransport.responseToReturn = new HttpResponse(200, "{\"status\": null}");

        assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });
    }

    @Test
    void testDefaultHttpTransport_RealHttpServer() throws IOException {
        String mockResponseBody = "{\"status\": \"READY\"}";
        startTestServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/v1/enrich/bulk/status", exchange.getRequestURI().getPath());
            assertEquals("Bearer integration-key", exchange.getRequestHeaders().getFirst("Authorization"));

            byte[] responseBytes = mockResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        ClientConfig realConfig = new ClientConfig.Builder("integration-key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true) // Need true since server is http localhost
                .build();

        XyoClient realClient = new XyoClient(realConfig);
        EnrichmentCollectionStatus status = realClient.enrichTransactionCollectionStatus("batch-123");
        assertEquals(EnrichmentCollectionStatus.READY, status);
    }

    @Test
    void testDefaultHttpTransport_MaxResponseBytesEnforced() throws IOException {
        String longResponseBody = "This is a very long response body that will exceed the configured limit.";
        startTestServer(exchange -> {
            byte[] responseBytes = longResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        ClientConfig limitConfig = new ClientConfig.Builder("key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .maxResponseBytes(10) // set very low limit
                .build();

        XyoClient limitClient = new XyoClient(limitConfig);

        XyoException exception = assertThrows(XyoException.class, () -> {
            limitClient.enrichTransactionCollectionStatus("batch-123");
        });

        assertEquals(ErrorCategory.TRANSPORT, exception.getCategory());
        assertTrue(exception.getMessage().contains("Response body exceeded maximum allowed size"));
    }

    @Test
    void testDefaultHttpTransport_HttpErrorPreserved() throws IOException {
        String errorJson = "{\"error_code\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}";
        startTestServer(exchange -> {
            byte[] responseBytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        ClientConfig errConfig = new ClientConfig.Builder("key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .build();

        XyoClient errClient = new XyoClient(errConfig);

        XyoException exception = assertThrows(XyoException.class, () -> {
            errClient.enrichTransactionCollectionStatus("batch-123");
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(429, exception.getHttpStatusCode());
        assertEquals(errorJson, exception.getResponseBody());
    }
}
