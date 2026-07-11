package com.xyo.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XyoClientTest {

    private ClientConfig config;
    private MockHttpTransport mockTransport;
    private XyoClient client;

    // A simple manual mock for HttpTransport to avoid adding Mockito dependency
    static class MockHttpTransport implements HttpTransport {
        HttpRequest lastRequest;
        HttpResponse responseToReturn;

        @Override
        public HttpResponse send(HttpRequest request) throws XyoException {
            this.lastRequest = request;
            if (responseToReturn == null) {
                throw new XyoException(ErrorCategory.TRANSPORT, "No mock response configured");
            }
            return responseToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        config = new ClientConfig("test-api-key");
        mockTransport = new MockHttpTransport();
        config.setHttpTransport(mockTransport);
        client = new XyoClient(config);
    }

    @Test
    void testEnrichTransaction_Success() {
        // Prepare mock response
        String jsonResponse = "{\n" +
                "  \"merchant\": \"Test Merchant\",\n" +
                "  \"description\": \"Test Description\",\n" +
                "  \"categories\": [\"Food\", \"Coffee\"],\n" +
                "  \"logo\": \"https://example.com/logo.png\",\n" +
                "  \"location\": \"London\",\n" +
                "  \"address\": \"123 Baker St\"\n" +
                "}";
        mockTransport.responseToReturn = new HttpResponse(200, jsonResponse);

        // Make the call
        EnrichmentRequest request = new EnrichmentRequest("COSTA PICKUP", "GB");
        EnrichmentResponse response = client.enrichTransaction(request);

        // Verify request was sent correctly
        assertNotNull(mockTransport.lastRequest);
        assertEquals("POST", mockTransport.lastRequest.getMethod());
        assertEquals("https://api.xyo.financial/v1/enrich", mockTransport.lastRequest.getUrl());
        assertEquals("Bearer test-api-key", mockTransport.lastRequest.getHeaders().get("Authorization").get(0));
        assertTrue(mockTransport.lastRequest.getBody().contains("\"content\":\"COSTA PICKUP\""));
        assertTrue(mockTransport.lastRequest.getBody().contains("\"countryCode\":\"GB\""));

        // Verify response parsing
        assertNotNull(response);
        assertEquals("Test Merchant", response.getMerchant());
        assertEquals("Test Description", response.getDescription());
        assertEquals(2, response.getCategories().size());
        assertEquals("London", response.getLocation());
    }

    @Test
    void testEnrichTransaction_ApiError() {
        // Return a 400 Bad Request
        mockTransport.responseToReturn = new HttpResponse(400, "{\"error\": \"Bad request\"}");

        EnrichmentRequest request = new EnrichmentRequest("", "GB");

        // The client should throw an exception for non-200 responses
        XyoException exception = assertThrows(XyoException.class, () -> {
            client.enrichTransaction(request);
        });

        assertEquals(ErrorCategory.HTTP, exception.getCategory());
        assertEquals(400, exception.getHttpStatusCode());
    }

    @Test
    void testEnrichTransactionCollection_Success() {
        // Prepare mock response
        String jsonResponse = "{\n" +
                "  \"id\": \"batch-123\",\n" +
                "  \"link\": \"https://api.xyo.financial/v1/enrich/bulk/batch-123\"\n" +
                "}";
        mockTransport.responseToReturn = new HttpResponse(200, jsonResponse);

        // Make the call
        EnrichmentRequest req1 = new EnrichmentRequest("COSTA", "GB");
        EnrichmentRequest req2 = new EnrichmentRequest("TESCO", "GB");
        EnrichTransactionCollectionResponse response = client.enrichTransactionCollection(List.of(req1, req2));

        // Verify parsing
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
}
