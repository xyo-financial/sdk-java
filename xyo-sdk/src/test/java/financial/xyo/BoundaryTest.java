package financial.xyo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Boundary condition tests for maxResponseBytes parsing")
class BoundaryTest {
    private HttpServer testServer;

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    @Test
    @DisplayName("Exact boundary limit response triggers PARSING error on non-JSON response")
    void testExactBoundary() throws Exception {
        String exactResponseBody = "12345"; // 5 bytes non-JSON response
        testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        testServer.createContext("/", exchange -> {
            byte[] responseBytes = exactResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        testServer.start();
        int testServerPort = testServer.getAddress().getPort();

        ClientConfig config = new ClientConfig.Builder("key")
                .apiBaseUrl("http://127.0.0.1:" + testServerPort)
                .allowInsecureHttp(true)
                .maxResponseBytes(5)
                .build();
        
        XyoClient client = new XyoClient(config);
        
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransactionCollectionStatus("batch-123");
        });
        assertEquals(ErrorCategory.PARSING, exception.getCategory());
    }
}
