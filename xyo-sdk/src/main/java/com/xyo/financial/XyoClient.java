package com.xyo.financial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XyoClient {

    private final ClientConfig config;
    private final ObjectMapper objectMapper;

    public XyoClient(ClientConfig config) {
        this.config = config;
        if (this.config.getApiKey() == null || this.config.getApiKey().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "api_key must not be empty");
        }
        if (this.config.getApiBaseUrl() == null || this.config.getApiBaseUrl().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "api_base_url must not be empty");
        }
        
        String baseUrl = this.config.getApiBaseUrl();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.config.setApiBaseUrl(baseUrl);

        if (this.config.getHttpTransport() == null) {
            this.config.setHttpTransport(new DefaultHttpTransport(this.config));
        }

        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpResponse post(String path, String body) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("Accept", Collections.singletonList("application/json"));
        headers.put("Authorization", Collections.singletonList("Bearer " + config.getApiKey()));

        HttpRequest request = new HttpRequest("POST", config.getApiBaseUrl() + path, headers, body);
        HttpResponse response = config.getHttpTransport().send(request);

        if (response.getStatusCode() != 200) {
            throw new XyoException(ErrorCategory.HTTP, "XYO API returned status code " + response.getStatusCode(), response.getStatusCode(), 0);
        }

        return response;
    }

    public EnrichmentResponse enrichTransaction(EnrichmentRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            HttpResponse response = post("/v1/enrich", jsonBody);
            return objectMapper.readValue(response.getBody(), EnrichmentResponse.class);
        } catch (JsonProcessingException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    public EnrichTransactionCollectionResponse enrichTransactionCollection(List<EnrichmentRequest> requests) {
        try {
            String jsonBody = objectMapper.writeValueAsString(requests);
            HttpResponse response = post("/v1/enrich/bulk", jsonBody);
            return objectMapper.readValue(response.getBody(), EnrichTransactionCollectionResponse.class);
        } catch (JsonProcessingException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    public EnrichmentCollectionStatus enrichTransactionCollectionStatus(String id) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("id", id);
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpResponse response = post("/v1/enrich/bulk/status", jsonBody);
            Map<String, EnrichmentCollectionStatus> result = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, EnrichmentCollectionStatus>>() {});
            return result.get("status");
        } catch (JsonProcessingException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}
