package com.xyo.financial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe client library used for interacting with the XYO.Financial Transaction Enrichment API.
 * <p>
 * This client provides support for enriching single transactions, running bulk asynchronous transaction
 * collections, and checking collection statuses.
 */
public class XyoClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String apiKey;
    private final String apiBaseUrl;
    private final HttpTransport httpTransport;

    /**
     * Constructs a new instance of XyoClient using the given configuration properties.
     * All settings from config are defensively copied to maintain client immutability.
     * 
     * @param config the client configuration
     * @throws XyoException if validation checks on API key or base URL fail
     */
    public XyoClient(ClientConfig config) {
        if (config == null) {
            throw new XyoException(ErrorCategory.VALIDATION, "ClientConfig must not be null");
        }
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "api_key must not be empty");
        }
        if (config.getApiBaseUrl() == null || config.getApiBaseUrl().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "api_base_url must not be empty");
        }

        this.apiKey = config.getApiKey();
        boolean allowInsecureHttp = config.isAllowInsecureHttp();

        String baseUrl = config.getApiBaseUrl();
        int end = baseUrl.length();
        while (end > 0 && baseUrl.charAt(end - 1) == '/') {
            end--;
        }
        this.apiBaseUrl = baseUrl.substring(0, end);

        // Fail-fast URL parsing check
        try {
            URI.create(this.apiBaseUrl);
        } catch (IllegalArgumentException e) {
            throw new XyoException(ErrorCategory.VALIDATION, "Invalid API base URL: " + this.apiBaseUrl, e);
        }

        // Validate insecure connection setting
        if (!allowInsecureHttp && this.apiBaseUrl.toLowerCase().startsWith("http://")) {
            throw new XyoException(ErrorCategory.VALIDATION, "Insecure HTTP connections are not allowed by default. Set allowInsecureHttp to true in ClientConfig if this is intentional.");
        }

        if (config.getHttpTransport() != null) {
            this.httpTransport = config.getHttpTransport();
        } else {
            this.httpTransport = new DefaultHttpTransport(config);
        }
    }

    private HttpResponse post(String path, String body) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("Accept", Collections.singletonList("application/json"));
        headers.put("Authorization", Collections.singletonList("Bearer " + apiKey));

        HttpRequest request = new HttpRequest("POST", apiBaseUrl + path, headers, body);
        HttpResponse response = httpTransport.send(request);

        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new XyoException(
                    ErrorCategory.HTTP,
                    "XYO API returned status code " + response.getStatusCode() + ": " + response.getBody(),
                    response.getStatusCode(),
                    0,
                    response.getBody()
            );
        }

        return response;
    }

    /**
     * Enriches a single transaction description with merchant, category, logo and location details.
     * 
     * @param request the enrichment parameters (content and countryCode)
     * @return the transaction enrichment response
     * @throws XyoException if input validation fails, parsing errors happen, or API returns a non-2xx response
     */
    public EnrichmentResponse enrichTransaction(EnrichmentRequest request) {
        if (request == null) {
            throw new XyoException(ErrorCategory.VALIDATION, "request must not be null");
        }
        request.validate();
        try {
            String jsonBody = OBJECT_MAPPER.writeValueAsString(request);
            HttpResponse response = post("/v1/enrich", jsonBody);
            return OBJECT_MAPPER.readValue(response.getBody(), EnrichmentResponse.class);
        } catch (JsonProcessingException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Submits a collection of transaction requests for bulk asynchronous processing.
     * 
     * @param requests the list of transactions to enrich
     * @return a bulk collection status tracking descriptor including the batch id and link
     * @throws XyoException if validation checks fail or the server returns an error response
     */
    public EnrichTransactionCollectionResponse enrichTransactionCollection(List<EnrichmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "requests list must not be null or empty");
        }
        for (EnrichmentRequest request : requests) {
            if (request == null) {
                throw new XyoException(ErrorCategory.VALIDATION, "request inside collection must not be null");
            }
            request.validate();
        }
        try {
            String jsonBody = OBJECT_MAPPER.writeValueAsString(requests);
            HttpResponse response = post("/v1/enrich/bulk", jsonBody);
            return OBJECT_MAPPER.readValue(response.getBody(), EnrichTransactionCollectionResponse.class);
        } catch (JsonProcessingException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Checks the processing status of a previously submitted asynchronous bulk enrichment collection.
     * 
     * @param id the unique batch tracking ID
     * @return the collection processing status (READY, PENDING, or FAILED)
     * @throws XyoException if parsing fails, trace status lookup returns bad code, or required elements are missing
     */
    public EnrichmentCollectionStatus enrichTransactionCollectionStatus(String id) {
        if (id == null || id.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "id must not be null or empty");
        }
        try {
            // non-standard: upstream requires POST for status
            String jsonBody = OBJECT_MAPPER.writeValueAsString(Collections.singletonMap("id", id));
            HttpResponse response = post("/v1/enrich/bulk/status", jsonBody);
            Map<String, EnrichmentCollectionStatus> result = OBJECT_MAPPER.readValue(
                    response.getBody(),
                    new TypeReference<Map<String, EnrichmentCollectionStatus>>() {}
                );
            if (result == null || !result.containsKey("status")) {
                throw new XyoException(ErrorCategory.PARSING, "Response is missing the required 'status' key: " + response.getBody());
            }
            EnrichmentCollectionStatus status = result.get("status");
            if (status == null) {
                throw new XyoException(ErrorCategory.PARSING, "Status value is null in response: " + response.getBody());
            }
            return status;
        } catch (JsonProcessingException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}
