package com.xyo.financial;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

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
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("   ", "GB")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", null)));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", "")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", " ")));
        // ISO 3166-1 alpha-2 requires exactly 2 characters
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", "G")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", "GBR")));
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest("COSTA", "USA")));
        // Content exceeds max length of 128 characters
        String longContent = "A".repeat(129);
        assertThrows(XyoException.class, () -> client.enrichTransaction(new EnrichmentRequest(longContent, "GB")));
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

    @Test
    void testDownloadEnrichmentCollection_Success() throws IOException {
        String json1 = "{\n" +
                "  \"merchant\": \"Uber\",\n" +
                "  \"description\": \"UBER TRIP\",\n" +
                "  \"categories\": [\"Transport\", \"Taxi\"],\n" +
                "  \"logo\": \"https://example.com/uber.png\",\n" +
                "  \"location\": \"San Francisco\",\n" +
                "  \"address\": \"1455 Market St\"\n" +
                "}";

        String json2 = "{\n" +
                "  \"merchant\": \"Starbucks\",\n" +
                "  \"description\": \"STARBUCKS COFFEE\",\n" +
                "  \"categories\": [\"Food\", \"Beverage\"],\n" +
                "  \"logo\": \"https://example.com/sbux.png\",\n" +
                "  \"location\": \"Seattle\",\n" +
                "  \"address\": \"2401 Utah Ave S\"\n" +
                "}";

        Map<String, String> files = new LinkedHashMap<>();
        files.put("00000000000000000000000000000001.json", json1);
        files.put("00000000000000000000000000000002.json", json2);

        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("/v1/enrich/bulk/download/batch-123", exchange.getRequestURI().getPath());
            assertEquals("Bearer test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("application/gzip", exchange.getRequestHeaders().getFirst("Accept"));

            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        client = createTestClient();
        List<EnrichmentResponse> results = client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/enrich/bulk/download/batch-123");

        assertNotNull(results);
        assertEquals(2, results.size());

        EnrichmentResponse r1 = results.get(0);
        assertEquals("Uber", r1.getMerchant());
        assertEquals("UBER TRIP", r1.getDescription());
        assertEquals(List.of("Transport", "Taxi"), r1.getCategories());
        assertEquals("https://example.com/uber.png", r1.getLogo());
        assertEquals("San Francisco", r1.getLocation());
        assertEquals("1455 Market St", r1.getAddress());

        EnrichmentResponse r2 = results.get(1);
        assertEquals("Starbucks", r2.getMerchant());
        assertEquals("STARBUCKS COFFEE", r2.getDescription());
        assertEquals(List.of("Food", "Beverage"), r2.getCategories());
        assertEquals("https://example.com/sbux.png", r2.getLogo());
        assertEquals("Seattle", r2.getLocation());
        assertEquals("2401 Utah Ave S", r2.getAddress());
    }

    @Test
    void testDownloadEnrichmentCollection_RelativeUrl() throws IOException {
        String json = "{\n" +
                "  \"merchant\": \"Tesco\",\n" +
                "  \"description\": \"TESCO STORES\",\n" +
                "  \"categories\": [\"Groceries\"],\n" +
                "  \"logo\": \"https://example.com/tesco.png\",\n" +
                "  \"location\": \"London\",\n" +
                "  \"address\": \"Welwyn Garden City\"\n" +
                "}";

        Map<String, String> files = new LinkedHashMap<>();
        files.put("item.json", json);
        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("/v1/enrich/bulk/download/batch-rel", exchange.getRequestURI().getPath());

            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        client = createTestClient();
        List<EnrichmentResponse> results = client.downloadEnrichmentCollection("/v1/enrich/bulk/download/batch-rel");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Tesco", results.get(0).getMerchant());
    }

    @Test
    void testDownloadEnrichmentCollection_ValidationErrors() {
        ClientConfig config = new ClientConfig.Builder("key")
                .apiBaseUrl("https://api.xyo.financial")
                .allowInsecureHttp(false)
                .build();
        client = new XyoClient(config);

        // Null URL
        XyoException exNull = assertThrows(XyoException.class, () -> client.downloadEnrichmentCollection(null));
        assertEquals(ErrorCategory.VALIDATION, exNull.getCategory());

        // Empty URL
        XyoException exEmpty = assertThrows(XyoException.class, () -> client.downloadEnrichmentCollection(""));
        assertEquals(ErrorCategory.VALIDATION, exEmpty.getCategory());

        // Blank URL
        XyoException exBlank = assertThrows(XyoException.class, () -> client.downloadEnrichmentCollection("   "));
        assertEquals(ErrorCategory.VALIDATION, exBlank.getCategory());

        // Insecure HTTP disallowed
        XyoException exInsecure = assertThrows(XyoException.class, () -> client.downloadEnrichmentCollection("http://insecure.example.com/download.tar.gz"));
        assertEquals(ErrorCategory.VALIDATION, exInsecure.getCategory());
        assertTrue(exInsecure.getMessage().contains("Insecure HTTP connections are not allowed"));
    }

    @Test
    void testDownloadEnrichmentCollection_HttpError404() throws IOException {
        String errorBody = "{\"error\": \"Collection archive not found\"}";
        startTestServer(exchange -> {
            byte[] resBytes = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/notfound");
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(404, exception.getHttpStatusCode());
        assertEquals(errorBody, exception.getResponseBody());
    }

    @Test
    void testDownloadEnrichmentCollection_HttpError500() throws IOException {
        String errorBody = "Internal Server Error";
        startTestServer(exchange -> {
            byte[] resBytes = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/500");
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(500, exception.getHttpStatusCode());
        assertEquals(errorBody, exception.getResponseBody());
    }

    @Test
    void testDownloadEnrichmentCollection_CorruptGzip() throws IOException {
        startTestServer(exchange -> {
            byte[] corruptBytes = "This is plain text, not valid gzip stream".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, corruptBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(corruptBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/corrupt-gzip");
        });

        assertEquals(ErrorCategory.PARSING, exception.getCategory());
        assertTrue(exception.getMessage().contains("Failed to decompress gzip response"));
    }

    @Test
    void testDownloadEnrichmentCollection_CorruptTar() throws IOException {
        // Gzip compressed corrupt data (not valid tar 512-byte blocks)
        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzipBaos)) {
            // Write a truncated 100-byte block that doesn't form a valid 512-byte tar header
            byte[] badHeader = new byte[512];
            System.arraycopy("bad_entry.json".getBytes(StandardCharsets.UTF_8), 0, badHeader, 0, 14);
            // Put invalid octal size
            System.arraycopy("invalid_octal".getBytes(StandardCharsets.US_ASCII), 0, badHeader, 124, 12);
            badHeader[156] = '0';
            gzipOut.write(badHeader);
        }
        byte[] gzippedCorrupt = gzipBaos.toByteArray();

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, gzippedCorrupt.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(gzippedCorrupt);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/corrupt-tar");
        });

        assertEquals(ErrorCategory.PARSING, exception.getCategory());
    }

    @Test
    void testDownloadEnrichmentCollection_InvalidJsonInArchive() throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("invalid.json", "this is not valid json { ");
        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/invalid-json");
        });

        assertEquals(ErrorCategory.PARSING, exception.getCategory());
        assertTrue(exception.getMessage().contains("Failed to parse JSON entry"));
    }

    @Test
    void testDownloadEnrichmentCollection_EmptyArchive() throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        client = createTestClient();
        List<EnrichmentResponse> results = client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/empty");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testDownloadEnrichmentCollection_CompressedExceedsMaxResponseBytes() throws IOException {
        String json = "{\"merchant\":\"Costa\",\"description\":\"Coffee\",\"categories\":[\"Food\"],\"logo\":\"url\"}";
        Map<String, String> files = new LinkedHashMap<>();
        files.put("item.json", json);
        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        // Configure client with maxResponseBytes smaller than compressed archive
        ClientConfig config = new ClientConfig.Builder("test-key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .maxResponseBytes(50) // smaller than archiveBytes
                .build();
        client = new XyoClient(config);

        XyoException ex = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/bounded-test");
        });

        assertEquals(ErrorCategory.PARSING, ex.getCategory());
        assertTrue(ex.getMessage().contains("Payload exceeded maximum allowed size"));
    }

    @Test
    void testDownloadEnrichmentCollection_DecompressedZipBombExceedsMaxResponseBytes() throws IOException {
        // Create an entry that is small when compressed but expands beyond decompressed limit
        String largeJson = "{\"merchant\":\"" + "A".repeat(5000) + "\",\"description\":\"Coffee\",\"categories\":[\"Food\"],\"logo\":\"url\"}";
        Map<String, String> files = new LinkedHashMap<>();
        files.put("item.json", largeJson);
        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        // Set limit larger than compressed stream (~150B) but smaller than decompressed (>5KB)
        ClientConfig config = new ClientConfig.Builder("test-key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .maxResponseBytes(500)
                .build();
        client = new XyoClient(config);

        XyoException ex = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/zip-bomb");
        });

        assertEquals(ErrorCategory.PARSING, ex.getCategory());
        assertTrue(ex.getMessage().contains("Payload exceeded maximum allowed size"));
    }

    @Test
    void testDownloadEnrichmentCollection_ZipSlipPathTraversalIgnored() throws IOException {
        String validJson = "{\"merchant\":\"Tesco\",\"description\":\"Groceries\",\"categories\":[\"Food\"],\"logo\":\"url\"}";
        String maliciousJson = "{\"merchant\":\"Evil\",\"description\":\"Hacked\",\"categories\":[\"Malware\"],\"logo\":\"url\"}";

        Map<String, String> files = new LinkedHashMap<>();
        files.put("../../../../etc/passwd.json", maliciousJson);
        files.put("/absolute/path/attack.json", maliciousJson);
        files.put("valid_record.json", validJson);
        byte[] archiveBytes = createTarGzArchive(files);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        client = createTestClient();
        List<EnrichmentResponse> results = client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/zip-slip");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Tesco", results.get(0).getMerchant());
    }

    @Test
    void testDownloadEnrichmentCollection_AuthorizationHeaderNotLeakedToExternalHost() throws IOException {
        String json = "{\"merchant\":\"Tesco\",\"description\":\"Groceries\",\"categories\":[\"Food\"],\"logo\":\"url\"}";
        Map<String, String> files = new LinkedHashMap<>();
        files.put("record.json", json);
        byte[] archiveBytes = createTarGzArchive(files);

        AtomicReference<String> capturedAuthHeader = new AtomicReference<>();

        startTestServer(exchange -> {
            capturedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.sendResponseHeaders(200, archiveBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(archiveBytes);
            }
        });

        // Client configured with a different base URL (e.g. https://api.xyo.financial)
        ClientConfig config = new ClientConfig.Builder("secret-api-key")
                .apiBaseUrl("https://api.xyo.financial")
                .allowInsecureHttp(true)
                .build();
        client = new XyoClient(config);

        // Downloading from a third-party host (127.0.0.1)
        List<EnrichmentResponse> results = client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/external-cdn");

        assertNotNull(results);
        assertEquals(1, results.size());
        // Verify Authorization Bearer was stripped and NOT sent to external host
        assertNull(capturedAuthHeader.get(), "Authorization header must not be leaked to external hosts");
    }

    private static byte[] createTarGzArchive(Map<String, String> files) throws IOException {
        ByteArrayOutputStream tarBaos = new ByteArrayOutputStream();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String name = entry.getKey();
            byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);

            byte[] header = new byte[512];
            // Name: 0..99
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));

            // Mode: 100..107
            byte[] modeBytes = "0000644\0".getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(modeBytes, 0, header, 100, modeBytes.length);

            // UID & GID: 108..123
            byte[] uidBytes = "0000000\0".getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(uidBytes, 0, header, 108, uidBytes.length);
            System.arraycopy(uidBytes, 0, header, 116, uidBytes.length);

            // Size: 124..135 (octal ASCII, 11 digits + null)
            String sizeOctal = String.format("%011o", content.length) + "\0";
            byte[] sizeBytes = sizeOctal.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(sizeBytes, 0, header, 124, sizeBytes.length);

            // Mtime: 136..147
            byte[] mtimeBytes = "00000000000\0".getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(mtimeBytes, 0, header, 136, mtimeBytes.length);

            // Typeflag: 156 ('0')
            header[156] = '0';

            // Magic: 257..262 ("ustar\0")
            byte[] magicBytes = "ustar\0".getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(magicBytes, 0, header, 257, magicBytes.length);

            // Version: 263..264 ("00")
            header[263] = '0';
            header[264] = '0';

            // Checksum: 148..155 (fill with spaces, sum unsigned bytes, write octal)
            for (int i = 148; i < 156; i++) {
                header[i] = ' ';
            }
            long sum = 0;
            for (byte b : header) {
                sum += (b & 0xFF);
            }
            String chksumStr = String.format("%06o\0 ", sum);
            byte[] chksumBytes = chksumStr.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(chksumBytes, 0, header, 148, Math.min(chksumBytes.length, 8));

            tarBaos.write(header);
            tarBaos.write(content);

            int padding = (512 - (content.length % 512)) % 512;
            if (padding > 0) {
                tarBaos.write(new byte[padding]);
            }
        }

        // Two 512-byte zero blocks
        tarBaos.write(new byte[1024]);

        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzipBaos)) {
            gzipOut.write(tarBaos.toByteArray());
        }
        return gzipBaos.toByteArray();
    }
}
