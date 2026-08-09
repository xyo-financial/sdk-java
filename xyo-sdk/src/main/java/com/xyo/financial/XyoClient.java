package com.xyo.financial;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.api.EnrichmentApi;
import com.xyo.model.EnrichTransactionsRequestInner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Thread-safe client library used for interacting with the XYO.Financial Transaction Enrichment API.
 * <p>
 * This client provides support for enriching single transactions, running bulk asynchronous transaction
 * collections, checking collection statuses, and downloading enrichment result archives.
 */
public class XyoClient {

    private final String apiKey;
    private final String apiBaseUrl;
    private final boolean allowInsecureHttp;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
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
        this.allowInsecureHttp = config.isAllowInsecureHttp();

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
        if (!this.allowInsecureHttp && this.apiBaseUrl.toLowerCase().startsWith("http://")) {
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
            this.requestTimeout = Duration.ofMillis(config.getRequestTimeoutMs());
            apiClient.setReadTimeout(this.requestTimeout);
        } else {
            this.requestTimeout = null;
        }

        // Configure Authorization Bearer token header interceptor
        apiClient.setRequestInterceptor(builder -> builder.header("Authorization", "Bearer " + this.apiKey));

        this.httpClient = apiClient.getHttpClient();
        this.objectMapper = apiClient.getObjectMapper();
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

    /**
     * Downloads and decompresses the .tar.gz enrichment results archive produced by the bulk enrichment pipeline.
     *
     * @param downloadUrl the download link URL returned in the bulk collection response
     * @return the list of enriched transaction responses parsed from the archive
     * @throws XyoException if validation checks fail, the HTTP download fails, decompression fails, or parsing fails
     */
    public List<EnrichmentResponse> downloadEnrichmentCollection(String downloadUrl) throws XyoException {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "downloadUrl must not be null or empty");
        }

        URI uri;
        try {
            String targetUrl = downloadUrl.trim();
            if (!targetUrl.toLowerCase().startsWith("http://") && !targetUrl.toLowerCase().startsWith("https://")) {
                if (!targetUrl.startsWith("/")) {
                    targetUrl = "/" + targetUrl;
                }
                targetUrl = this.apiBaseUrl + targetUrl;
            }
            uri = URI.create(targetUrl);
        } catch (IllegalArgumentException e) {
            throw new XyoException(ErrorCategory.VALIDATION, "Invalid download URL: " + downloadUrl, e);
        }

        if (!this.allowInsecureHttp && "http".equalsIgnoreCase(uri.getScheme())) {
            throw new XyoException(ErrorCategory.VALIDATION, "Insecure HTTP connections are not allowed by default. Set allowInsecureHttp to true in ClientConfig if this is intentional.");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("Accept", "application/gzip");

        if (this.apiKey != null && !this.apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + this.apiKey);
        }

        if (this.requestTimeout != null) {
            requestBuilder.timeout(this.requestTimeout);
        }

        HttpResponse<InputStream> response;
        try {
            response = this.httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XyoException(ErrorCategory.TRANSPORT, "Download request was interrupted: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new XyoException(ErrorCategory.TRANSPORT, "HTTP download request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new XyoException(ErrorCategory.TRANSPORT, "Unexpected error during download request: " + e.getMessage(), e);
        }

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            String errorBody = "";
            try (InputStream is = response.body()) {
                if (is != null) {
                    errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
            }
            throw new XyoException(
                    ErrorCategory.HTTP,
                    "XYO API returned status code " + statusCode + (errorBody.isEmpty() ? "" : ": " + errorBody),
                    statusCode,
                    0,
                    errorBody
            );
        }

        byte[] tarBytes;
        try (InputStream responseStream = response.body()) {
            if (responseStream == null) {
                throw new XyoException(ErrorCategory.PARSING, "Response body is null");
            }
            try (GZIPInputStream gzipIn = new GZIPInputStream(responseStream)) {
                tarBytes = gzipIn.readAllBytes();
            }
        } catch (IOException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to decompress gzip response: " + e.getMessage(), e);
        }

        return parseTarArchive(tarBytes);
    }

    private List<EnrichmentResponse> parseTarArchive(byte[] tarBytes) {
        List<EnrichmentResponse> results = new ArrayList<>();
        if (tarBytes == null || tarBytes.length == 0) {
            return results;
        }

        int offset = 0;
        String nextLongName = null;

        while (offset + 512 <= tarBytes.length) {
            byte[] header = Arrays.copyOfRange(tarBytes, offset, offset + 512);

            // Check if header block is all zeros
            boolean allZeros = true;
            for (int i = 0; i < 512; i++) {
                if (header[i] != 0) {
                    allZeros = false;
                    break;
                }
            }
            if (allZeros) {
                break;
            }

            String entryName;
            if (nextLongName != null) {
                entryName = nextLongName;
                nextLongName = null;
            } else {
                int nameEnd = 0;
                while (nameEnd < 100 && header[nameEnd] != 0) {
                    nameEnd++;
                }
                entryName = new String(header, 0, nameEnd, StandardCharsets.UTF_8).trim();

                // Check ustar prefix
                int prefixEnd = 345;
                while (prefixEnd < 500 && header[prefixEnd] != 0) {
                    prefixEnd++;
                }
                if (prefixEnd > 345) {
                    String prefix = new String(header, 345, prefixEnd - 345, StandardCharsets.UTF_8).trim();
                    if (!prefix.isEmpty()) {
                        entryName = prefix + "/" + entryName;
                    }
                }
            }

            byte typeFlag = header[156];
            long size;
            try {
                size = parseOctal(header, 124, 12);
            } catch (IllegalArgumentException e) {
                throw new XyoException(ErrorCategory.PARSING, "Invalid tar entry size for entry: " + entryName, e);
            }

            offset += 512;

            if (offset + size > tarBytes.length) {
                throw new XyoException(ErrorCategory.PARSING, "Truncated tar archive: entry '" + entryName + "' extends beyond archive boundary");
            }

            if (typeFlag == 'L') {
                nextLongName = new String(tarBytes, offset, (int) size, StandardCharsets.UTF_8).replace("\0", "").trim();
            } else if (typeFlag == '0' || typeFlag == 0 || typeFlag == (byte) '0') {
                if (size > 0 && (entryName.endsWith(".json") || isJsonPayload(tarBytes, offset, (int) size))) {
                    byte[] contentBytes = Arrays.copyOfRange(tarBytes, offset, offset + (int) size);
                    try {
                        EnrichmentResponse enrichmentResponse = this.objectMapper.readValue(contentBytes, EnrichmentResponse.class);
                        if (enrichmentResponse != null) {
                            results.add(enrichmentResponse);
                        }
                    } catch (Exception e) {
                        throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON entry '" + entryName + "': " + e.getMessage(), e);
                    }
                }
            }

            // Advance past content blocks (rounded up to 512-byte boundary)
            long paddedSize = (size + 511) & ~511L;
            offset += (int) paddedSize;
        }

        return results;
    }

    private static boolean isJsonPayload(byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            byte b = bytes[i];
            if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                continue;
            }
            return b == '{';
        }
        return false;
    }

    private static long parseOctal(byte[] header, int offset, int length) {
        long result = 0;
        int end = offset + length;
        int start = offset;
        while (start < end && (header[start] == ' ' || header[start] == 0)) {
            start++;
        }
        for (int i = start; i < end; i++) {
            byte b = header[i];
            if (b == 0 || b == ' ') {
                break;
            }
            if (b < '0' || b > '7') {
                throw new IllegalArgumentException("Invalid octal character: " + (char) b);
            }
            result = (result << 3) + (b - '0');
        }
        return result;
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
