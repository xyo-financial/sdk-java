package financial.xyo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
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
                if (!(t instanceof IOException)) {
                    handlerException.set(t);
                }
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {
                }
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
    void testClientConfigValidation() {
        assertThrows(XyoException.class, () -> new XyoClient(null));
        assertThrows(IllegalArgumentException.class, () -> new ClientConfig.Builder((String) null).build());
        assertThrows(IllegalArgumentException.class, () -> new ClientConfig.Builder("").build());

        ClientConfig confNoUrl = new ClientConfig.Builder("key").apiBaseUrl("").build();
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
    void testClientConfigImmutability() {
        ClientConfig config = new ClientConfig.Builder("key")
                .apiBaseUrl("https://api.xyo.financial///")
                .build();

        new XyoClient(config);

        // Assert that the original config remains untouched
        assertEquals("https://api.xyo.financial///", config.getApiBaseUrl());

        // Verify toBuilder allows creating modified clone without affecting original
        ClientConfig copy = config.toBuilder().apiBaseUrl("https://other.xyo.financial").build();
        assertEquals("https://api.xyo.financial///", config.getApiBaseUrl());
        assertEquals("https://other.xyo.financial", copy.getApiBaseUrl());
    }

    @Test
    void testEnforceSecureHttp() {
        ClientConfig insecureConf = new ClientConfig.Builder("key")
                .apiBaseUrl("http://api.xyo.financial")
                .allowInsecureHttp(false)
                .build();

        XyoException exception = assertThrows(XyoException.class, () -> new XyoClient(insecureConf));
        assertEquals(ErrorCategory.VALIDATION, exception.getCategory());

        // Should allow if explicitly configured
        ClientConfig allowedConf = insecureConf.toBuilder().allowInsecureHttp(true).build();
        assertDoesNotThrow(() -> new XyoClient(allowedConf));
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
            exchange.getResponseHeaders().set("Retry-After", "60");
            exchange.getResponseHeaders().set("RateLimit-Limit", "1000");
            exchange.getResponseHeaders().set("RateLimit-Remaining", "0");
            exchange.getResponseHeaders().set("RateLimit-Reset", "1700000000");
            exchange.sendResponseHeaders(429, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        client = createTestClient();
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });

        assertEquals(ErrorCategory.RATE_LIMIT, exception.getCategory());
        assertEquals(429, exception.getHttpStatusCode());
        assertEquals(errorJson, exception.getResponseBody());
        assertEquals(60L, exception.getRetryAfter());
        assertEquals(1000L, exception.getRateLimitLimit());
        assertEquals(0L, exception.getRateLimitRemaining());
        assertEquals(1700000000L, exception.getRateLimitReset());
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
            assertTrue(exchange.getRequestHeaders().getFirst("Accept").contains("application/gzip"));

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

        // 1. Client downloading from same API server host includes Authorization
        ClientConfig config = new ClientConfig.Builder("secret-api-key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .build();
        client = new XyoClient(config);

        List<EnrichmentResponse> results = client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/archive.tar.gz");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Bearer secret-api-key", capturedAuthHeader.get());

        // 2. Untrusted rogue domain is rejected
        XyoException ex = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("https://evil-untrusted-domain.com/malicious.tar.gz");
        });
        assertEquals(ErrorCategory.VALIDATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("not permitted for secure archive downloads"));
    }

    @Test
    void testDynamicApiKeyRotation_Supplier() throws IOException {
        AtomicReference<String> currentSecret = new AtomicReference<>("initial-key-1");
        List<String> observedAuthHeaders = new ArrayList<>();

        startTestServer(exchange -> {
            observedAuthHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String response = "{\"merchant\":\"Starbucks\",\"description\":\"Coffee\",\"categories\":[\"Food\"],\"logo\":\"url\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });

        ClientConfig config = new ClientConfig.Builder(currentSecret::get)
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .build();

        try (XyoClient dynamicClient = new XyoClient(config)) {
            // First call with initial key
            dynamicClient.enrichTransaction(new EnrichmentRequest("Coffee purchase", "US"));
            
            // Rotate key dynamically in runtime
            currentSecret.set("rotated-secret-key-2");
            dynamicClient.enrichTransaction(new EnrichmentRequest("Second purchase", "US"));
        }

        assertEquals(2, observedAuthHeaders.size());
        assertEquals("Bearer initial-key-1", observedAuthHeaders.get(0));
        assertEquals("Bearer rotated-secret-key-2", observedAuthHeaders.get(1));
    }

    @Test
    void testDownloadEnrichmentCollection_UnsupportedScheme_SSRFRejection() {
        client = createTestClient();

        XyoException ex1 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("file:///etc/passwd");
        });
        assertEquals(ErrorCategory.VALIDATION, ex1.getCategory());
        assertTrue(ex1.getMessage().contains("Unsupported URI scheme: 'file'"));

        XyoException ex2 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("ftp://malicious.host/archive.tar.gz");
        });
        assertEquals(ErrorCategory.VALIDATION, ex2.getCategory());
        assertTrue(ex2.getMessage().contains("Unsupported URI scheme: 'ftp'"));
    }

    @Test
    void testDownloadEnrichmentCollection_UnexpectedContentType_WAFChallenge() throws IOException {
        String htmlChallenge = "<html><body><h1>Cloudflare / WAF Security Challenge</h1></body></html>";
        byte[] bytes = htmlChallenge.getBytes(StandardCharsets.UTF_8);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            } catch (IOException ignored) {
                // Client may abort connection upon receiving non-tar content-type header
            }
        });

        client = createTestClient();

        XyoException ex = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://127.0.0.1:" + testServerPort + "/v1/download/waf-challenge");
        });

        assertEquals(ErrorCategory.HTTP, ex.getCategory());
        assertTrue(ex.getMessage().contains("Unexpected Content-Type 'text/html; charset=UTF-8'"));
    }

    @Test
    @DisplayName("Submitting bulk enrichment collection with CRLF characters in apiUser throws VALIDATION exception (CWE-113)")
    void testEnrichTransactionCollection_ApiUserCrlfInjectionThrows() {
        client = createTestClient();
        List<EnrichmentRequest> requests = List.of(new EnrichmentRequest("STARBUCKS", "US"));

        XyoException ex = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollection(requests, "admin\r\nX-Injected-Header: evil");
        });

        assertEquals(ErrorCategory.VALIDATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("apiUser must not contain control characters"));
    }

    @Test
    @DisplayName("Checking collection status with CRLF characters in apiUser throws VALIDATION exception (CWE-113)")
    void testEnrichTransactionCollectionStatus_ApiUserCrlfInjectionThrows() {
        client = createTestClient();

        XyoException ex = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123", "admin\nInjected: evil");
        });

        assertEquals(ErrorCategory.VALIDATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("apiUser must not contain control characters"));
    }

    @Test
    @DisplayName("ClientConfig.Builder throws IllegalArgumentException when neither apiKey nor apiKeySupplier is provided")
    void testClientConfigBuilder_EmptyApiKeyAndNullSupplierThrows() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfig.Builder((String) null).build();
        });
        assertTrue(ex1.getMessage().contains("apiKey or apiKeySupplier must be provided"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfig.Builder("").build();
        });
        assertTrue(ex2.getMessage().contains("apiKey or apiKeySupplier must be provided"));
    }

    @Test
    @DisplayName("Tracing headers (X-Correlation-ID and traceparent) are correctly transmitted")
    void testTracingHeadersTransmitted() throws IOException {
        java.util.UUID correlationId = java.util.UUID.randomUUID();
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        AtomicReference<String> capturedCorrId = new AtomicReference<>();
        AtomicReference<String> capturedTraceparent = new AtomicReference<>();

        startTestServer(exchange -> {
            capturedCorrId.set(exchange.getRequestHeaders().getFirst("X-Correlation-ID"));
            capturedTraceparent.set(exchange.getRequestHeaders().getFirst("traceparent"));
            String jsonResponse = "{\"merchant\": \"Test Merchant\", \"description\": \"Desc\", \"categories\": [\"Cat\"]}";
            byte[] resBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        EnrichmentRequest request = new EnrichmentRequest("COSTA PICKUP", "GB");
        EnrichmentResponse response = client.enrichTransaction(request, correlationId, traceparent);

        assertNotNull(response);
        assertEquals(correlationId.toString(), capturedCorrId.get());
        assertEquals(traceparent, capturedTraceparent.get());
    }

    @Test
    @DisplayName("RequestOptions configures correlationId, traceparent, and apiUser")
    void testRequestOptionsUsage() throws IOException {
        java.util.UUID correlationId = java.util.UUID.randomUUID();
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        String apiUser = "user-123";

        AtomicReference<String> capturedCorrId = new AtomicReference<>();
        AtomicReference<String> capturedTraceparent = new AtomicReference<>();
        AtomicReference<String> capturedApiUser = new AtomicReference<>();

        startTestServer(exchange -> {
            capturedCorrId.set(exchange.getRequestHeaders().getFirst("X-Correlation-ID"));
            capturedTraceparent.set(exchange.getRequestHeaders().getFirst("traceparent"));
            capturedApiUser.set(exchange.getRequestHeaders().getFirst("x-api-user"));
            String jsonResponse = "{\"id\": \"batch-999\", \"link\": \"https://api.xyo.financial/v1/enrich/bulk/batch-999\"}";
            byte[] resBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resBytes);
            }
        });

        client = createTestClient();
        RequestOptions options = RequestOptions.builder()
                .correlationId(correlationId)
                .traceparent(traceparent)
                .apiUser(apiUser)
                .build();

        EnrichTransactionCollectionResponse response = client.enrichTransactionCollection(
                List.of(new EnrichmentRequest("TESCO", "GB")),
                options
        );

        assertNotNull(response);
        assertEquals("batch-999", response.getId());
        assertEquals(correlationId.toString(), capturedCorrId.get());
        assertEquals(traceparent, capturedTraceparent.get());
        assertEquals(apiUser, capturedApiUser.get());
    }

    @Test
    @DisplayName("Submitting bulk collection exceeding 50,000 items throws VALIDATION exception")
    void testEnrichTransactionCollection_ExceedsMaxBatchSize() {
        client = createTestClient();
        EnrichmentRequest dummyReq = new EnrichmentRequest("TEST", "US");
        List<EnrichmentRequest> oversizedList = Collections.nCopies(50001, dummyReq);

        XyoException ex = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollection(oversizedList);
        });

        assertEquals(ErrorCategory.VALIDATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("must not exceed 50,000 items"));
    }

    @Test
    @DisplayName("Traceparent header containing CRLF characters throws VALIDATION exception")
    void testTraceparentValidation_CrlfInjectionThrows() {
        client = createTestClient();
        String crlfTraceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01\r\nX-Injected: evil";
        EnrichmentRequest request = new EnrichmentRequest("STARBUCKS", "US");

        XyoException ex1 = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request, null, crlfTraceparent);
        });
        assertEquals(ErrorCategory.VALIDATION, ex1.getCategory());
        assertEquals("traceparent must strictly conform to W3C format", ex1.getMessage());

        XyoException ex2 = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollection(List.of(request), null, null, crlfTraceparent);
        });
        assertEquals(ErrorCategory.VALIDATION, ex2.getCategory());
        assertEquals("traceparent must strictly conform to W3C format", ex2.getMessage());

        XyoException ex3 = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123", null, null, crlfTraceparent);
        });
        assertEquals(ErrorCategory.VALIDATION, ex3.getCategory());
        assertEquals("traceparent must strictly conform to W3C format", ex3.getMessage());

        XyoException ex4 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("/v1/enrich/bulk/download/batch-123", null, crlfTraceparent);
        });
        assertEquals(ErrorCategory.VALIDATION, ex4.getCategory());
        assertEquals("traceparent must strictly conform to W3C format", ex4.getMessage());
    }

    @Test
    @DisplayName("Malformed traceparent header format throws VALIDATION exception")
    void testTraceparentValidation_MalformedTraceparentThrows() {
        client = createTestClient();
        EnrichmentRequest request = new EnrichmentRequest("STARBUCKS", "US");

        List<String> malformedTraceparents = List.of(
                "invalid",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7", // missing flags component
                "00-4BF92F3577B34DA6A3CE929D0E0E4736-00F067AA0BA902B7-01", // uppercase hex characters
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-extra", // extra trailing component
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-0g", // non-hex character 'g'
                "" // empty string
        );

        for (String invalidTp : malformedTraceparents) {
            XyoException ex = assertThrows(XyoException.class, () -> {
                client.enrichTransaction(request, null, invalidTp);
            }, "Expected validation error for traceparent: " + invalidTp);

            assertEquals(ErrorCategory.VALIDATION, ex.getCategory());
            assertEquals("traceparent must strictly conform to W3C format", ex.getMessage());
        }
    }

    @Test
    @DisplayName("Error body exceeding 1,000 characters is truncated in exception message to prevent log bloat")
    void testErrorBodyTruncation() throws IOException {
        String longErrorBody = "X".repeat(1500);

        startTestServer(exchange -> {
            byte[] resBytes = longErrorBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
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
        assertEquals(longErrorBody, exception.getResponseBody());
        assertTrue(exception.getMessage().contains("... [truncated]"));
        assertTrue(exception.getMessage().contains("X".repeat(1000)));
        assertFalse(exception.getMessage().contains("X".repeat(1001)));
    }

    @Test
    void testRequestOptions_EqualsHashCodeToString() {
        java.util.UUID uuid1 = java.util.UUID.randomUUID();
        java.util.UUID uuid2 = java.util.UUID.randomUUID();

        RequestOptions opt1 = RequestOptions.builder()
                .correlationId(uuid1)
                .traceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .apiUser("user1")
                .build();

        RequestOptions opt2 = RequestOptions.builder()
                .correlationId(uuid1)
                .traceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .apiUser("user1")
                .build();

        RequestOptions opt3 = RequestOptions.builder()
                .correlationId(uuid2)
                .traceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .apiUser("user1")
                .build();

        assertEquals(opt1, opt2);
        assertEquals(opt1.hashCode(), opt2.hashCode());
        assertNotEquals(opt1, opt3);
        assertNotEquals(opt1, null);
        assertNotEquals(opt1, "other object");
        assertEquals(opt1, opt1);

        String str = opt1.toString();
        assertTrue(str.contains("RequestOptions"));
        assertTrue(str.contains(uuid1.toString()));
        assertTrue(str.contains("user1"));
    }

    @Test
    void testRetryAfterHeader_Rfc1123DateParsing() throws IOException {
        java.time.ZonedDateTime futureDate = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(120);
        String rfc1123Str = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(futureDate);

        startTestServer(exchange -> {
            exchange.getResponseHeaders().set("Retry-After", rfc1123Str);
            exchange.sendResponseHeaders(429, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(new byte[0]);
            }
        });

        client = createTestClient();
        EnrichmentRequest request = new EnrichmentRequest("Costa", "GB");

        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request);
        });

        assertEquals(ErrorCategory.RATE_LIMIT, exception.getCategory());
        assertNotNull(exception.getRetryAfter());
        assertTrue(exception.getRetryAfter() > 0 && exception.getRetryAfter() <= 120,
                "retryAfter should be calculated from RFC 1123 header, expected ~120s but got " + exception.getRetryAfter());
    }

    @Test
    void testS3EndpointPattern_GlobalAndDualstack() throws Exception {
        client = createTestClient();

        XyoException ex1 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://s3.amazonaws.com/mybucket/file.tar.gz");
        });
        assertFalse(ex1.getMessage().contains("is not permitted for secure archive downloads"),
                "s3.amazonaws.com should be permitted by ALLOWED_S3_PATTERN");

        XyoException ex2 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://bucket.s3.amazonaws.com/file.tar.gz");
        });
        assertFalse(ex2.getMessage().contains("is not permitted for secure archive downloads"),
                "bucket.s3.amazonaws.com should be permitted by ALLOWED_S3_PATTERN");

        XyoException ex3 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://bucket.s3.dualstack.us-east-1.amazonaws.com/file.tar.gz");
        });
        assertFalse(ex3.getMessage().contains("is not permitted for secure archive downloads"),
                "bucket.s3.dualstack.us-east-1.amazonaws.com should be permitted by ALLOWED_S3_PATTERN");

        XyoException ex4 = assertThrows(XyoException.class, () -> {
            client.downloadEnrichmentCollection("http://unauthorized.domain.com/file.tar.gz");
        });
        assertTrue(ex4.getMessage().contains("Domain 'unauthorized.domain.com' is not permitted for secure archive downloads"));
    }

    @Test
    void testApiException_InterruptedException_ReassertsInterrupt() {
        financial.xyo.client.ApiException apiEx = new financial.xyo.client.ApiException(new InterruptedException("interrupted"));

        Thread.interrupted();

        try {
            java.lang.reflect.Method method = XyoClient.class.getDeclaredMethod("handleApiException", financial.xyo.client.ApiException.class);
            method.setAccessible(true);
            XyoClient clientObj = createTestClient();
            method.invoke(clientObj, apiEx);
        } catch (Exception ignored) {
        }

        assertTrue(Thread.currentThread().isInterrupted(), "Thread interrupt flag should be set");
        Thread.interrupted();
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

    @Test
    void testApiKeySupplierExceptionWrapped() {
        java.util.function.Supplier<String> failingSupplier = () -> {
            throw new RuntimeException("Vault connection timeout");
        };

        ClientConfig config = new ClientConfig.Builder(failingSupplier)
                .apiBaseUrl("https://api.xyo.financial")
                .build();

        XyoException exception = assertThrows(XyoException.class, () -> new XyoClient(config));
        assertEquals(ErrorCategory.VALIDATION, exception.getCategory());
        assertTrue(exception.getMessage().contains("Failed to retrieve initial API key from supplier"));
    }

    @Test
    void testClientCloseAutoCloseable() {
        ClientConfig config = new ClientConfig.Builder("test-key")
                .apiBaseUrl("https://api.xyo.financial")
                .build();

        assertDoesNotThrow(() -> {
            try (XyoClient xyoClient = new XyoClient(config)) {
                assertNotNull(xyoClient);
            }
        });
    }

    @Test
    void testEnrichmentRequestValidation_Alpha2AndTrimming() {
        // Valid 2-letter codes and automatic trimming / uppercase normalization
        EnrichmentRequest req1 = new EnrichmentRequest("  Starbucks  ", "  gb  ");
        assertEquals("Starbucks", req1.getContent());
        assertEquals("GB", req1.getCountryCode());

        EnrichmentRequest req2 = new EnrichmentRequest("Apple", "US");
        assertEquals("Apple", req2.getContent());
        assertEquals("US", req2.getCountryCode());

        EnrichmentRequest req3 = new EnrichmentRequest("Lidl", "de");
        assertEquals("Lidl", req3.getContent());
        assertEquals("DE", req3.getCountryCode());

        // Invalid codes: digits, punctuation, 3-letter, 1-letter (fail-fast on creation)
        assertThrows(XyoException.class, () -> new EnrichmentRequest("Test", "12"));
        assertThrows(XyoException.class, () -> new EnrichmentRequest("Test", "G1"));
        assertThrows(XyoException.class, () -> new EnrichmentRequest("Test", "1G"));
        assertThrows(XyoException.class, () -> new EnrichmentRequest("Test", "GBR"));
        assertThrows(XyoException.class, () -> new EnrichmentRequest("Test", "G!"));
        assertThrows(XyoException.class, () -> new EnrichmentRequest("Test", ""));
    }

    @Test
    void testToBuilder_AuthModeSwitching() {
        ClientConfig staticConfig = ClientConfig.builder("static-key").build();
        assertEquals("static-key", staticConfig.getApiKey());
        assertNull(staticConfig.getApiKeySupplier());

        // Switch to dynamic supplier via toBuilder()
        ClientConfig dynamicConfig = staticConfig.toBuilder().apiKeySupplier(() -> "dynamic-key").build();
        assertNotNull(dynamicConfig.getApiKeySupplier());
        assertEquals("dynamic-key", dynamicConfig.getApiKey());

        // Switch back to static key via toBuilder()
        ClientConfig backToStatic = dynamicConfig.toBuilder().apiKey("new-static-key").build();
        assertNull(backToStatic.getApiKeySupplier());
        assertEquals("new-static-key", backToStatic.getApiKey());
    }

    @Test
    void testStaticBuilderUniformity() {
        EnrichmentRequest req = EnrichmentRequest.builder().content("Tesco").countryCode("GB").build();
        assertEquals("Tesco", req.getContent());
        assertEquals("GB", req.getCountryCode());

        EnrichmentResponse res = EnrichmentResponse.builder().merchant("Tesco Extra").description("Groceries").build();
        assertEquals("Tesco Extra", res.getMerchant());
        assertEquals("Groceries", res.getDescription());

        EnrichTransactionCollectionResponse batch = EnrichTransactionCollectionResponse.builder().id("batch-456").link("https://status").build();
        assertEquals("batch-456", batch.getId());
        assertEquals("https://status", batch.getLink());
    }

    @Test
    void testJsonIgnoreUnknownProperties() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String responseJson = "{\"merchant\":\"Costa\",\"description\":\"Coffee\",\"future_metadata\":\"v2_field\"}";
        EnrichmentResponse res = mapper.readValue(responseJson, EnrichmentResponse.class);
        assertEquals("Costa", res.getMerchant());

        String batchJson = "{\"id\":\"batch-789\",\"link\":\"https://api/batches/789\",\"extra_routing\":\"aws-eu-west-1\"}";
        EnrichTransactionCollectionResponse batch = mapper.readValue(batchJson, EnrichTransactionCollectionResponse.class);
        assertEquals("batch-789", batch.getId());
    }

    @Test
    void testClientConfig_EqualsAndHashCode_ExcludesLambdasAndSecrets() {
        ClientConfig config1 = new ClientConfig.Builder("key1")
                .apiBaseUrl("https://api.xyo.financial")
                .connectTimeoutMs(5000)
                .build();

        ClientConfig config2 = new ClientConfig.Builder(() -> "key2")
                .apiBaseUrl("https://api.xyo.financial")
                .connectTimeoutMs(5000)
                .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testApiKey_ControlCharactersRejected() {
        ClientConfig configWithCrlf = new ClientConfig.Builder("key-with-\r\ninjection").build();
        XyoException exception = assertThrows(XyoException.class, () -> new XyoClient(configWithCrlf));
        assertEquals(ErrorCategory.VALIDATION, exception.getCategory());
        assertTrue(exception.getMessage().contains("control characters"));
    }

    @Test
    void testEnrichmentRequest_ToStringRedacted() {
        EnrichmentRequest request = new EnrichmentRequest("SECRET_CARD_NARRATIVE_12345", "GB");
        String str = request.toString();
        assertFalse(str.contains("SECRET_CARD_NARRATIVE_12345"));
        assertTrue(str.contains("[REDACTED]"));
        assertTrue(str.contains("GB"));
    }

    @Test
    void testS3HttpsEnforcement() {
        ClientConfig config = new ClientConfig.Builder("test-key")
                .apiBaseUrl("https://api.xyo.financial")
                .allowInsecureHttp(true)
                .build();
        XyoClient xyoClient = new XyoClient(config);

        XyoException exception = assertThrows(XyoException.class, () -> {
            xyoClient.downloadEnrichmentCollection("http://bucket.s3.amazonaws.com/batch.tar.gz");
        });
        assertEquals(ErrorCategory.VALIDATION, exception.getCategory());
        assertTrue(exception.getMessage().contains("External storage downloads (S3) must use HTTPS"));
    }

    @Test
    void testClientConfig_CustomHttpClientBuilder() {
        java.net.http.HttpClient.Builder customBuilder = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12));

        ClientConfig config = new ClientConfig.Builder("test-key")
                .apiBaseUrl("https://api.xyo.financial")
                .httpClientBuilder(customBuilder)
                .build();

        assertNotNull(config.getHttpClientBuilder());
        XyoClient customClient = new XyoClient(config);
        assertNotNull(customClient);
    }

    @Test
    void testClientConfig_DeprecatedHttpClientThrows() {
        java.net.http.HttpClient dummyClient = java.net.http.HttpClient.newHttpClient();
        assertThrows(UnsupportedOperationException.class, () -> {
            new ClientConfig.Builder("test-key").httpClient(dummyClient);
        });
    }
}
