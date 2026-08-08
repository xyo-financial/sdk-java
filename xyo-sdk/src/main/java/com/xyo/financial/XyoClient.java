package com.xyo.financial;

import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.api.EnrichmentApi;
import com.xyo.model.EnrichTransactionsRequestInner;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe client library used for interacting with the XYO.Financial Transaction Enrichment API.
 * <p>
 * This client provides support for enriching single transactions, running bulk asynchronous transaction
 * collections, and checking collection statuses.
 */
public class XyoClient {

    private final String apiKey;
    private final String apiBaseUrl;
    private final EnrichmentApi enrichmentApi;

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

        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(this.apiBaseUrl);

        if (config.getHttpClient() != null) {
            apiClient.setHttpClientBuilder(config.getHttpClient().newBuilder());
        }

        if (config.getConnectTimeoutMs() > 0) {
            apiClient.setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()));
        }
        if (config.getRequestTimeoutMs() > 0) {
            apiClient.setReadTimeout(Duration.ofMillis(config.getRequestTimeoutMs()));
        }

        // Configure Authorization Bearer token header interceptor
        apiClient.setRequestInterceptor(builder -> builder.header("Authorization", "Bearer " + this.apiKey));

        this.enrichmentApi = new EnrichmentApi(apiClient);
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

        com.xyo.model.EnrichmentRequest apiReq = new com.xyo.model.EnrichmentRequest();
        apiReq.setContent(request.getContent());
        apiReq.setCountryCode(request.getCountryCode());

        try {
            com.xyo.model.EnrichmentResponse apiRes = enrichmentApi.enrichTransaction(apiReq);
            if (apiRes == null) {
                throw new XyoException(ErrorCategory.PARSING, "Enrichment API returned null response");
            }
            return new EnrichmentResponse(
                apiRes.getMerchant(),
                apiRes.getDescription(),
                apiRes.getCategories(),
                apiRes.getLogo(),
                apiRes.getLocation(),
                apiRes.getAddress()
            );
        } catch (ApiException e) {
            throw handleApiException(e);
        } catch (Exception e) {
            if (e instanceof XyoException) {
                throw (XyoException) e;
            }
            throw new XyoException(ErrorCategory.TRANSPORT, e.getMessage(), e);
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
        return enrichTransactionCollection(requests, null);
    }

    /**
     * Submits a collection of transaction requests for bulk asynchronous processing with an optional x-api-user header.
     * 
     * @param requests the list of transactions to enrich
     * @param apiUser optional user header value
     * @return a bulk collection status tracking descriptor including the batch id and link
     * @throws XyoException if validation checks fail or the server returns an error response
     */
    public EnrichTransactionCollectionResponse enrichTransactionCollection(List<EnrichmentRequest> requests, String apiUser) {
        if (requests == null || requests.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "requests list must not be null or empty");
        }
        List<EnrichTransactionsRequestInner> apiReqList = new ArrayList<>(requests.size());
        for (EnrichmentRequest request : requests) {
            if (request == null) {
                throw new XyoException(ErrorCategory.VALIDATION, "request inside collection must not be null");
            }
            request.validate();
            EnrichTransactionsRequestInner inner = new EnrichTransactionsRequestInner();
            inner.setContent(request.getContent());
            inner.setCountryCode(request.getCountryCode());
            apiReqList.add(inner);
        }

        try {
            com.xyo.model.EnrichTransactionCollectionResponse apiRes = enrichmentApi.enrichTransactions(apiUser, apiReqList);
            if (apiRes == null) {
                throw new XyoException(ErrorCategory.PARSING, "Enrichment API returned null response");
            }
            return new EnrichTransactionCollectionResponse(apiRes.getId(), apiRes.getLink());
        } catch (ApiException e) {
            throw handleApiException(e);
        } catch (Exception e) {
            if (e instanceof XyoException) {
                throw (XyoException) e;
            }
            throw new XyoException(ErrorCategory.TRANSPORT, e.getMessage(), e);
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
        return enrichTransactionCollectionStatus(id, null);
    }

    /**
     * Checks the processing status of a previously submitted asynchronous bulk enrichment collection with optional x-api-user header.
     * 
     * @param id the unique batch tracking ID
     * @param apiUser optional user header value
     * @return the collection processing status (READY, PENDING, or FAILED)
     * @throws XyoException if parsing fails, trace status lookup returns bad code, or required elements are missing
     */
    public EnrichmentCollectionStatus enrichTransactionCollectionStatus(String id, String apiUser) {
        if (id == null || id.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "id must not be null or empty");
        }
        try {
            com.xyo.model.EnrichmentCollectionStatusResponse apiRes = enrichmentApi.getEnrichmentStatus(id, apiUser);
            if (apiRes == null || apiRes.getStatus() == null) {
                throw new XyoException(ErrorCategory.PARSING, "Response is missing the required 'status' key");
            }
            String rawStatus = apiRes.getStatus().getValue();
            if (rawStatus == null) {
                throw new XyoException(ErrorCategory.PARSING, "Status value is null in response");
            }
            return EnrichmentCollectionStatus.fromValue(rawStatus);
        } catch (ApiException e) {
            throw handleApiException(e);
        } catch (IllegalArgumentException e) {
            throw new XyoException(ErrorCategory.PARSING, e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof XyoException) {
                throw (XyoException) e;
            }
            throw new XyoException(ErrorCategory.TRANSPORT, e.getMessage(), e);
        }
    }

    private XyoException handleApiException(ApiException e) {
        if (e.getCode() > 0) {
            return new XyoException(
                ErrorCategory.HTTP,
                "XYO API returned status code " + e.getCode() + ": " + e.getResponseBody(),
                e.getCode(),
                0,
                e.getResponseBody()
            );
        }
        if (e.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException
                || (e.getMessage() != null && e.getMessage().contains("JSON"))) {
            return new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
        return new XyoException(ErrorCategory.TRANSPORT, e.getMessage(), e);
    }
}
