package financial.xyo;

import com.fasterxml.jackson.databind.ObjectMapper;
import financial.xyo.client.ApiClient;
import financial.xyo.client.ApiException;
import financial.xyo.api.EnrichmentApi;
import financial.xyo.model.EnrichTransactionsRequestInner;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLParameters;
import java.util.function.Supplier;

/**
 * Thread-safe client library used for interacting with the XYO.Financial Transaction Enrichment API.
 * <p>
 * This client provides support for enriching single transactions, running bulk asynchronous transaction
 * collections, checking collection statuses, and downloading enrichment result archives.
 */
public class XyoClient implements AutoCloseable {

    private static final Pattern TRACEPARENT_PATTERN =
            Pattern.compile("^[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");

    private static final Pattern ALLOWED_S3_PATTERN =
            Pattern.compile("^([a-z0-9][a-z0-9\\-]{1,61}[a-z0-9]\\.)?s3([.-]([a-z0-9-]+|dualstack[.-][a-z0-9-]+|accelerate|fips[.-][a-z0-9-]+))?\\.amazonaws\\.com$");

    private final Supplier<String> apiKeySupplier;
    private final String apiBaseUrl;
    private final boolean allowInsecureHttp;
    private final long maxResponseBytes;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final EnrichmentApi enrichmentApi;

    private static void validateTraceparent(String traceparent) {
        if (traceparent != null && !TRACEPARENT_PATTERN.matcher(traceparent).matches()) {
            throw new XyoException(ErrorCategory.VALIDATION, "traceparent must strictly conform to W3C format");
        }
    }

    private static void validateApiUser(String apiUser) {
        if (apiUser != null && apiUser.chars().anyMatch(c -> (c < 0x20 && c != '\t') || c == 0x7F)) {
            throw new XyoException(ErrorCategory.VALIDATION, "apiUser must not contain control characters");
        }
    }

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
        
        if (config.getApiKeySupplier() != null) {
            this.apiKeySupplier = config.getApiKeySupplier();
        } else if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            String key = config.getApiKey();
            this.apiKeySupplier = () -> key;
        } else {
            throw new XyoException(ErrorCategory.VALIDATION, "api_key must not be empty");
        }

        String initialKey;
        try {
            initialKey = this.apiKeySupplier.get();
        } catch (Exception e) {
            throw new XyoException(ErrorCategory.VALIDATION, "Failed to retrieve initial API key from supplier: " + e.getMessage(), e);
        }
        if (initialKey == null || initialKey.trim().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "api_key must not be empty");
        }

        if (config.getApiBaseUrl() == null || config.getApiBaseUrl().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "api_base_url must not be empty");
        }

        this.allowInsecureHttp = config.isAllowInsecureHttp();
        this.maxResponseBytes = config.getMaxResponseBytes();

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
        } else {
            // Enforce minimum TLS 1.2+ version (PCI-DSS 4.0 §4.2.1 compliance)
            SSLParameters sslParams = new SSLParameters();
            sslParams.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            apiClient.setHttpClientBuilder(HttpClient.newBuilder().sslParameters(sslParams));
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

        // Configure dynamic Authorization Bearer token header interceptor (NIST SP 800-57 key rotation support)
        apiClient.setRequestInterceptor(builder -> {
            String key;
            try {
                key = this.apiKeySupplier.get();
            } catch (Exception e) {
                throw new XyoException(ErrorCategory.VALIDATION, "Failed to retrieve API key from supplier: " + e.getMessage(), e);
            }
            if (key == null || key.trim().isEmpty()) {
                throw new XyoException(ErrorCategory.VALIDATION, "API key supplier returned null or empty key; cannot authenticate request");
            }
            builder.header("Authorization", "Bearer " + key.trim());
        });

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
        return enrichTransaction(request, (RequestOptions) null);
    }

    /**
     * Enriches a single transaction description with request options (tracing headers).
     * 
     * @param request the enrichment parameters
     * @param options per-request options containing correlation ID, traceparent, etc.
     * @return the transaction enrichment response
     * @throws XyoException if input validation fails, parsing errors happen, or API returns a non-2xx response
     */
    public EnrichmentResponse enrichTransaction(EnrichmentRequest request, RequestOptions options) {
        UUID correlationId = options != null ? options.getCorrelationId() : null;
        String traceparent = options != null ? options.getTraceparent() : null;
        return enrichTransaction(request, correlationId, traceparent);
    }

    /**
     * Enriches a single transaction description with distributed tracing headers.
     * 
     * @param request the enrichment parameters
     * @param correlationId optional correlation UUID for distributed tracing
     * @param traceparent optional W3C traceparent header string for APM tracing
     * @return the transaction enrichment response
     * @throws XyoException if input validation fails, parsing errors happen, or API returns a non-2xx response
     */
    public EnrichmentResponse enrichTransaction(EnrichmentRequest request, UUID correlationId, String traceparent) {
        if (request == null) {
            throw new XyoException(ErrorCategory.VALIDATION, "request must not be null");
        }
        validateTraceparent(traceparent);
        request.validate();

        financial.xyo.model.EnrichmentRequest apiReq = new financial.xyo.model.EnrichmentRequest();
        apiReq.setContent(request.getContent().trim());
        apiReq.setCountryCode(request.getCountryCode().trim().toUpperCase(Locale.ROOT));

        try {
            financial.xyo.model.EnrichmentResponse apiRes = enrichmentApi.enrichTransaction(apiReq, correlationId, traceparent);
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
            throw wrapUnexpected(e);
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
        return enrichTransactionCollection(requests, (RequestOptions) null);
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
        return enrichTransactionCollection(requests, RequestOptions.builder().apiUser(apiUser).build());
    }

    /**
     * Submits a collection of transaction requests for bulk asynchronous processing with distributed tracing headers.
     * 
     * @param requests the list of transactions to enrich
     * @param correlationId optional correlation UUID for distributed tracing
     * @param traceparent optional W3C traceparent header string for APM tracing
     * @return a bulk collection status tracking descriptor including the batch id and link
     * @throws XyoException if validation checks fail or the server returns an error response
     */
    public EnrichTransactionCollectionResponse enrichTransactionCollection(List<EnrichmentRequest> requests, UUID correlationId, String traceparent) {
        return enrichTransactionCollection(requests, RequestOptions.builder().correlationId(correlationId).traceparent(traceparent).build());
    }

    /**
     * Submits a collection of transaction requests for bulk asynchronous processing with request options.
     * 
     * @param requests the list of transactions to enrich
     * @param options per-request options containing correlation ID, traceparent, apiUser, etc.
     * @return a bulk collection status tracking descriptor including the batch id and link
     * @throws XyoException if validation checks fail or the server returns an error response
     */
    public EnrichTransactionCollectionResponse enrichTransactionCollection(List<EnrichmentRequest> requests, RequestOptions options) {
        String apiUser = options != null ? options.getApiUser() : null;
        UUID correlationId = options != null ? options.getCorrelationId() : null;
        String traceparent = options != null ? options.getTraceparent() : null;
        return enrichTransactionCollection(requests, apiUser, correlationId, traceparent);
    }

    /**
     * Submits a collection of transaction requests for bulk asynchronous processing with optional headers and distributed tracing headers.
     * 
     * @param requests the list of transactions to enrich (1 to 50,000 items)
     * @param apiUser optional user header value
     * @param correlationId optional correlation UUID for distributed tracing
     * @param traceparent optional W3C traceparent header string for APM tracing
     * @return a bulk collection status tracking descriptor including the batch id and link
     * @throws XyoException if validation checks fail or the server returns an error response
     */
    public EnrichTransactionCollectionResponse enrichTransactionCollection(List<EnrichmentRequest> requests, String apiUser, UUID correlationId, String traceparent) {
        validateApiUser(apiUser);
        validateTraceparent(traceparent);
        if (requests == null || requests.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "requests list must not be null or empty");
        }
        if (requests.size() > 50000) {
            throw new XyoException(ErrorCategory.VALIDATION, "requests list size must not exceed 50,000 items");
        }
        List<EnrichTransactionsRequestInner> apiReqList = new ArrayList<>(requests.size());
        for (EnrichmentRequest request : requests) {
            if (request == null) {
                throw new XyoException(ErrorCategory.VALIDATION, "request inside collection must not be null");
            }
            request.validate();
            EnrichTransactionsRequestInner inner = new EnrichTransactionsRequestInner();
            inner.setContent(request.getContent().trim());
            inner.setCountryCode(request.getCountryCode().trim().toUpperCase(Locale.ROOT));
            apiReqList.add(inner);
        }

        try {
            financial.xyo.model.EnrichTransactionCollectionResponse apiRes = enrichmentApi.enrichTransactions(apiReqList, apiUser, correlationId, traceparent);
            if (apiRes == null) {
                throw new XyoException(ErrorCategory.PARSING, "Enrichment API returned null response");
            }
            return new EnrichTransactionCollectionResponse(apiRes.getId(), apiRes.getLink());
        } catch (ApiException e) {
            throw handleApiException(e);
        } catch (Exception e) {
            throw wrapUnexpected(e);
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
        return enrichTransactionCollectionStatus(id, (RequestOptions) null);
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
        return enrichTransactionCollectionStatus(id, RequestOptions.builder().apiUser(apiUser).build());
    }

    /**
     * Checks the processing status of a previously submitted asynchronous bulk enrichment collection with distributed tracing headers.
     * 
     * @param id the unique batch tracking ID
     * @param correlationId optional correlation UUID for distributed tracing
     * @param traceparent optional W3C traceparent header string for APM tracing
     * @return the collection processing status (READY, PENDING, or FAILED)
     * @throws XyoException if parsing fails, trace status lookup returns bad code, or required elements are missing
     */
    public EnrichmentCollectionStatus enrichTransactionCollectionStatus(String id, UUID correlationId, String traceparent) {
        return enrichTransactionCollectionStatus(id, RequestOptions.builder().correlationId(correlationId).traceparent(traceparent).build());
    }

    /**
     * Checks the processing status of a previously submitted asynchronous bulk enrichment collection with request options.
     * 
     * @param id the unique batch tracking ID
     * @param options per-request options containing correlation ID, traceparent, apiUser, etc.
     * @return the collection processing status (READY, PENDING, or FAILED)
     * @throws XyoException if parsing fails, trace status lookup returns bad code, or required elements are missing
     */
    public EnrichmentCollectionStatus enrichTransactionCollectionStatus(String id, RequestOptions options) {
        String apiUser = options != null ? options.getApiUser() : null;
        UUID correlationId = options != null ? options.getCorrelationId() : null;
        String traceparent = options != null ? options.getTraceparent() : null;
        return enrichTransactionCollectionStatus(id, apiUser, correlationId, traceparent);
    }

    /**
     * Checks the processing status of a previously submitted asynchronous bulk enrichment collection with optional headers and tracing parameters.
     * 
     * @param id the unique batch tracking ID
     * @param apiUser optional user header value
     * @param correlationId optional correlation UUID for distributed tracing
     * @param traceparent optional W3C traceparent header string for APM tracing
     * @return the collection processing status (READY, PENDING, or FAILED)
     * @throws XyoException if parsing fails, trace status lookup returns bad code, or required elements are missing
     */
    public EnrichmentCollectionStatus enrichTransactionCollectionStatus(String id, String apiUser, UUID correlationId, String traceparent) {
        validateApiUser(apiUser);
        validateTraceparent(traceparent);
        if (id == null || id.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "id must not be null or empty");
        }
        try {
            financial.xyo.model.EnrichmentCollectionStatusResponse apiRes = enrichmentApi.getEnrichmentStatus(id, apiUser, correlationId, traceparent);
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
            throw wrapUnexpected(e);
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
        return downloadEnrichmentCollection(downloadUrl, (RequestOptions) null);
    }

    /**
     * Downloads and decompresses the .tar.gz enrichment results archive with request options.
     *
     * @param downloadUrl the download link URL returned in the bulk collection response
     * @param options per-request options containing correlation ID, traceparent, etc.
     * @return the list of enriched transaction responses parsed from the archive
     * @throws XyoException if validation checks fail, the HTTP download fails, decompression fails, or parsing fails
     */
    public List<EnrichmentResponse> downloadEnrichmentCollection(String downloadUrl, RequestOptions options) throws XyoException {
        UUID correlationId = options != null ? options.getCorrelationId() : null;
        String traceparent = options != null ? options.getTraceparent() : null;
        return downloadEnrichmentCollection(downloadUrl, correlationId, traceparent);
    }

    /**
     * Downloads and decompresses the .tar.gz enrichment results archive produced by the bulk enrichment pipeline with distributed tracing headers.
     *
     * @param downloadUrl the download link URL returned in the bulk collection response
     * @param correlationId optional correlation UUID for distributed tracing
     * @param traceparent optional W3C traceparent header string for APM tracing
     * @return the list of enriched transaction responses parsed from the archive
     * @throws XyoException if validation checks fail, the HTTP download fails, decompression fails, or parsing fails
     */
    public List<EnrichmentResponse> downloadEnrichmentCollection(String downloadUrl, UUID correlationId, String traceparent) throws XyoException {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "downloadUrl must not be null or empty");
        }
        validateTraceparent(traceparent);

        URI uri;
        try {
            String targetUrl = downloadUrl.trim();
            URI candidate = URI.create(targetUrl);
            if (candidate.isAbsolute()) {
                uri = candidate;
            } else {
                if (!targetUrl.startsWith("/")) {
                    targetUrl = "/" + targetUrl;
                }
                uri = URI.create(this.apiBaseUrl + targetUrl);
            }
        } catch (IllegalArgumentException e) {
            throw new XyoException(ErrorCategory.VALIDATION, "Invalid download URL: " + downloadUrl, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
            throw new XyoException(ErrorCategory.VALIDATION, "Unsupported URI scheme: '" + scheme + "'. Only HTTPS (and HTTP if explicitly allowed) is permitted.");
        }

        if (!this.allowInsecureHttp && "http".equalsIgnoreCase(scheme)) {
            throw new XyoException(ErrorCategory.VALIDATION, "Insecure HTTP connections are not allowed by default. Set allowInsecureHttp to true in ClientConfig if this is intentional.");
        }

        URI baseUri = URI.create(this.apiBaseUrl);
        boolean isApiHost = (baseUri.getHost() != null && baseUri.getHost().equalsIgnoreCase(uri.getHost()));
        boolean isS3 = (uri.getHost() != null && ALLOWED_S3_PATTERN.matcher(uri.getHost().toLowerCase()).matches());

        if (!isApiHost && !isS3) {
            throw new XyoException(ErrorCategory.VALIDATION, "Domain '" + uri.getHost() + "' is not permitted for secure archive downloads");
        }

        if (isS3 && !"https".equalsIgnoreCase(scheme)) {
            throw new XyoException(ErrorCategory.VALIDATION, "External storage downloads (S3) must use HTTPS");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("Accept", "application/gzip, application/x-tar, application/octet-stream;q=0.9, */*;q=0.8");

        if (correlationId != null) {
            requestBuilder.header("X-Correlation-ID", correlationId.toString());
        }
        if (traceparent != null) {
            requestBuilder.header("traceparent", traceparent);
        }

        String currentKey;
        try {
            currentKey = this.apiKeySupplier.get();
        } catch (Exception e) {
            throw new XyoException(ErrorCategory.VALIDATION, "Failed to retrieve API key from supplier: " + e.getMessage(), e);
        }
        if (currentKey != null && !currentKey.isEmpty()) {
            // Only attach Authorization header if target host matches configured API base URL host (prevents token leakage)
            if (isApiHost) {
                requestBuilder.header("Authorization", "Bearer " + currentKey);
            }
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
            throw wrapUnexpected(e);
        }

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            String errorBody = "";
            try (InputStream is = response.body()) {
                if (is != null) {
                    byte[] bytes = is.readNBytes(64 * 1024); // 64 KB cap on error bodies
                    errorBody = new String(bytes, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
            }
            throw createHttpException(statusCode, errorBody, response.headers(), null);
        }

        // Validate Content-Type header to diagnose intermediate proxy/WAF challenge pages
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.isEmpty()) {
            String ct = contentType.toLowerCase();
            if (!ct.contains("gzip") && !ct.contains("tar") && !ct.contains("octet-stream") && !ct.contains("binary")) {
                throw new XyoException(
                        ErrorCategory.HTTP,
                        "Unexpected Content-Type '" + contentType + "' received when expecting binary archive",
                        statusCode,
                        0,
                        ""
                );
            }
        }

        List<EnrichmentResponse> results = new ArrayList<>();
        try (InputStream responseStream = response.body()) {
            if (responseStream == null) {
                throw new XyoException(ErrorCategory.PARSING, "Response body is null");
            }

            InputStream boundedRawStream = (this.maxResponseBytes > 0)
                    ? new BoundedInputStream(responseStream, this.maxResponseBytes, "compressed HTTP stream")
                    : responseStream;

            try (GZIPInputStream gzipIn = new GZIPInputStream(boundedRawStream)) {
                InputStream boundedDecompressedStream = (this.maxResponseBytes > 0)
                        ? new BoundedInputStream(gzipIn, this.maxResponseBytes, "decompressed archive stream")
                        : gzipIn;

                try (TarArchiveInputStream tarIn = new TarArchiveInputStream(boundedDecompressedStream)) {
                    TarArchiveEntry entry;
                    int entryCount = 0;
                    while ((entry = tarIn.getNextTarEntry()) != null) {
                        entryCount++;
                        if (entryCount > ClientConfig.DEFAULT_MAX_TAR_ENTRIES) {
                            throw new PayloadTooLargeException("Archive contains too many entries (exceeded maximum limit of " + ClientConfig.DEFAULT_MAX_TAR_ENTRIES + " entries)");
                        }
                        if (entry.isDirectory()) {
                            continue;
                        }
                        String entryName = entry.getName();
                        // Zip-Slip and path traversal mitigation
                        if (entryName == null || entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                            continue;
                        }
                        if (entryName.endsWith(".json")) {
                            try {
                                EnrichmentResponse enrichmentResponse = this.objectMapper.readValue(
                                        new NonClosingInputStream(tarIn),
                                        EnrichmentResponse.class
                                );
                                if (enrichmentResponse != null) {
                                    results.add(enrichmentResponse);
                                }
                            } catch (Exception e) {
                                throw new XyoException(ErrorCategory.PARSING, "Failed to parse JSON entry '" + sanitizeEntryName(entryName) + "': " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        } catch (PayloadTooLargeException e) {
            throw new XyoException(ErrorCategory.PARSING, e.getMessage(), e);
        } catch (IOException e) {
            throw new XyoException(ErrorCategory.PARSING, "Failed to decompress gzip response: " + e.getMessage(), e);
        }

        return results;
    }

    private static class PayloadTooLargeException extends IOException {
        PayloadTooLargeException(String message) {
            super(message);
        }
    }

    private static class BoundedInputStream extends FilterInputStream {
        private final long maxBytes;
        private final String description;
        private long bytesRead = 0;

        BoundedInputStream(InputStream in, long maxBytes, String description) {
            super(in);
            this.maxBytes = maxBytes;
            this.description = description;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                checkCount(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = super.read(b, off, len);
            if (count > 0) {
                checkCount(count);
            }
            return count;
        }

        private void checkCount(long count) throws IOException {
            bytesRead += count;
            if (maxBytes > 0 && bytesRead > maxBytes) {
                throw new PayloadTooLargeException("Payload exceeded maximum allowed size of " + maxBytes + " bytes (" + description + ")");
            }
        }
    }

    private static class NonClosingInputStream extends FilterInputStream {
        NonClosingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // No-op to prevent downstream parsers (like Jackson) from closing the underlying tar archive stream
        }
    }

    private static String sanitizeEntryName(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[\\r\\n\\p{C}]", "_");
    }

    /**
     * Closes this client and releases any underlying resources.
     */
    @Override
    public void close() {
        if (this.httpClient instanceof AutoCloseable) {
            try {
                ((AutoCloseable) this.httpClient).close();
            } catch (Exception ignored) {
            }
        }
    }

    private XyoException createHttpException(int statusCode, String errorBody, HttpHeaders headers, Throwable cause) {
        ErrorCategory category = (statusCode == 429) ? ErrorCategory.RATE_LIMIT : ErrorCategory.HTTP;
        Long retryAfter = parseRetryAfterHeader(headers);
        Long rateLimitLimit = parseLongHeader(headers, "RateLimit-Limit", "X-RateLimit-Limit");
        Long rateLimitRemaining = parseLongHeader(headers, "RateLimit-Remaining", "X-RateLimit-Remaining");
        Long rateLimitReset = parseLongHeader(headers, "RateLimit-Reset", "X-RateLimit-Reset");

        String displayBody = errorBody;
        if (displayBody != null && displayBody.length() > 1000) {
            displayBody = displayBody.substring(0, 1000) + "... [truncated]";
        }

        String message = "XYO API returned status code " + statusCode
                + ((displayBody == null || displayBody.isEmpty()) ? "" : ": " + displayBody);

        return new XyoException(
                category,
                message,
                cause,
                statusCode,
                0,
                errorBody,
                retryAfter,
                rateLimitLimit,
                rateLimitRemaining,
                rateLimitReset
        );
    }

    private XyoException wrapUnexpected(Exception e) {
        if (e instanceof XyoException) {
            return (XyoException) e;
        }
        return new XyoException(ErrorCategory.TRANSPORT, e.getMessage(), e);
    }

    private XyoException handleApiException(ApiException e) {
        if (e.getCause() instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        if (e.getCode() > 0) {
            return createHttpException(e.getCode(), e.getResponseBody(), e.getResponseHeaders(), e.getCause());
        }
        if (e.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException
                || (e.getMessage() != null && e.getMessage().contains("JSON"))) {
            return new XyoException(ErrorCategory.PARSING, "Failed to parse JSON: " + e.getMessage(), e);
        }
        return new XyoException(ErrorCategory.TRANSPORT, e.getMessage(), e);
    }

    private Long parseRetryAfterHeader(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        java.util.Optional<String> val = headers.firstValue("Retry-After");
        if (!val.isPresent() || val.get().trim().isEmpty()) {
            return null;
        }
        String trimmed = val.get().trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
                long seconds = Duration.between(Instant.now(), zdt.toInstant()).getSeconds();
                return Math.max(0L, seconds);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private Long parseLongHeader(HttpHeaders headers, String... headerNames) {
        if (headers == null) {
            return null;
        }
        for (String name : headerNames) {
            java.util.Optional<String> val = headers.firstValue(name);
            if (val.isPresent() && !val.get().trim().isEmpty()) {
                try {
                    return Long.parseLong(val.get().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
