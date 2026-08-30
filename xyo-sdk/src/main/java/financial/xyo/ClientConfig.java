package financial.xyo;

import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immutable configuration options for the {@link XyoClient}.
 * <p>
 * Use {@link #builder(String)} or {@link #builder(Supplier)} to construct instances.
 */
public final class ClientConfig {
    
    /** The default XYO API base URL. */
    public static final String DEFAULT_API_BASE_URL = "https://api.xyo.financial";

    /** Environment variable name used to configure the API base URL. */
    public static final String ENV_API_BASE_URL = "XYO_API_BASE_URL";

    /** Default maximum number of entries permitted when unpacking results archives (50,000). */
    public static final int DEFAULT_MAX_TAR_ENTRIES = 50000;
    
    /** The default connection timeout in milliseconds (5 seconds). */
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    
    /** The default request timeout in milliseconds (30 seconds). */
    public static final long DEFAULT_REQUEST_TIMEOUT_MS = 30000;
    
    /** The default maximum allowed compressed response size in bytes (10 MB). */
    public static final long DEFAULT_MAX_RESPONSE_BYTES = 10 * 1024 * 1024;

    /** The default maximum allowed decompressed archive size in bytes (64 MB). */
    public static final long DEFAULT_MAX_DECOMPRESSED_BYTES = 64 * 1024 * 1024;
    
    /** Default option for allowing insecure HTTP connections. */
    public static final boolean DEFAULT_ALLOW_INSECURE_HTTP = false;

    private final @Nullable String apiKey;
    private final @Nullable Supplier<String> apiKeySupplier;
    private final String apiBaseUrl;
    private final long connectTimeoutMs;
    private final long requestTimeoutMs;
    private final long maxResponseBytes;
    private final long maxDecompressedBytes;
    private final int maxTarEntries;
    private final boolean allowInsecureHttp;
    private final HttpClient.@Nullable Builder httpClientBuilder;

    /**
     * Resolves the default API base URL, checking the {@value #ENV_API_BASE_URL} environment variable first.
     * 
     * @return the resolved base URL
     */
    public static String resolveDefaultBaseUrl() {
        String envUrl = System.getenv(ENV_API_BASE_URL);
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            return envUrl.trim();
        }
        return DEFAULT_API_BASE_URL;
    }

    /**
     * Constructs a new ClientConfig with the specified API key and default settings.
     * 
     * @param apiKey the API key for authentication
     * @deprecated Use {@link #builder(String)} instead to ensure robust initialization.
     */
    @Deprecated
    public ClientConfig(String apiKey) {
        this(new Builder(apiKey));
    }

    private ClientConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.apiKeySupplier = builder.apiKeySupplier;
        this.apiBaseUrl = builder.apiBaseUrl != null ? builder.apiBaseUrl : resolveDefaultBaseUrl();
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.requestTimeoutMs = builder.requestTimeoutMs;
        this.maxResponseBytes = builder.maxResponseBytes;
        this.maxDecompressedBytes = builder.maxDecompressedBytes;
        this.maxTarEntries = builder.maxTarEntries;
        this.allowInsecureHttp = builder.allowInsecureHttp;
        this.httpClientBuilder = builder.httpClientBuilder;
    }

    /**
     * Gets the API key. If an {@link Supplier} was configured, executes the supplier to fetch the current key.
     * 
     * @return the API key
     */
    public @Nullable String getApiKey() {
        if (apiKeySupplier != null) {
            return apiKeySupplier.get();
        }
        return apiKey;
    }

    /**
     * Gets the dynamic API key supplier, if configured.
     * 
     * @return the key supplier, or null if a static key was configured
     */
    public @Nullable Supplier<String> getApiKeySupplier() {
        return apiKeySupplier;
    }

    /**
     * Gets the configured API base URL.
     * 
     * @return the base URL
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * Gets the connection timeout in milliseconds.
     * 
     * @return connection timeout in ms
     */
    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * Gets the request/read timeout in milliseconds.
     * 
     * @return request timeout in ms
     */
    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    /**
     * Gets the maximum allowed compressed response size in bytes.
     * 
     * @return maximum response bytes
     */
    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }

    /**
     * Gets the maximum allowed decompressed archive size in bytes.
     * 
     * @return maximum decompressed bytes
     */
    public long getMaxDecompressedBytes() {
        return maxDecompressedBytes;
    }

    /**
     * Gets the maximum allowed tar entries when extracting archive responses.
     * 
     * @return maximum tar entries
     */
    public int getMaxTarEntries() {
        return maxTarEntries;
    }

    /**
     * Indicates whether insecure HTTP connections are allowed.
     * 
     * @return true if insecure HTTP is allowed
     */
    public boolean isAllowInsecureHttp() {
        return allowInsecureHttp;
    }

    /**
     * Gets the custom {@link HttpClient.Builder}, if configured.
     * 
     * @return the client builder, or null if default builder is used
     */
    public HttpClient.@Nullable Builder getHttpClientBuilder() {
        return httpClientBuilder;
    }

    /**
     * Creates a new empty {@link Builder}.
     * 
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new {@link Builder} with the specified static API key.
     * 
     * @param apiKey the API key
     * @return a new Builder
     */
    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    /**
     * Creates a new {@link Builder} with the specified dynamic API key supplier.
     * 
     * @param apiKeySupplier supplier providing current API keys
     * @return a new Builder
     */
    public static Builder builder(Supplier<String> apiKeySupplier) {
        return new Builder(apiKeySupplier);
    }

    /**
     * Creates a new {@link Builder} initialized with values from this instance.
     * 
     * @return a pre-populated Builder
     */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.apiKey = this.apiKey;
        b.apiKeySupplier = this.apiKeySupplier;
        b.apiBaseUrl = this.apiBaseUrl;
        b.connectTimeoutMs = this.connectTimeoutMs;
        b.requestTimeoutMs = this.requestTimeoutMs;
        b.maxResponseBytes = this.maxResponseBytes;
        b.maxDecompressedBytes = this.maxDecompressedBytes;
        b.maxTarEntries = this.maxTarEntries;
        b.allowInsecureHttp = this.allowInsecureHttp;
        b.httpClientBuilder = this.httpClientBuilder;
        return b;
    }

    /**
     * Compares this configuration with another for structural and credential equality.
     * <p>
     * <b>Note on dynamic suppliers:</b> Configurations using {@link Supplier} for secret rotation
     * or custom {@link HttpClient.Builder} evaluate those functional components by reference identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientConfig that = (ClientConfig) o;
        return connectTimeoutMs == that.connectTimeoutMs &&
                requestTimeoutMs == that.requestTimeoutMs &&
                maxResponseBytes == that.maxResponseBytes &&
                maxDecompressedBytes == that.maxDecompressedBytes &&
                maxTarEntries == that.maxTarEntries &&
                allowInsecureHttp == that.allowInsecureHttp &&
                Objects.equals(apiBaseUrl, that.apiBaseUrl) &&
                Objects.equals(apiKey, that.apiKey) &&
                Objects.equals(apiKeySupplier, that.apiKeySupplier) &&
                Objects.equals(httpClientBuilder, that.httpClientBuilder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                apiBaseUrl,
                apiKey,
                apiKeySupplier,
                connectTimeoutMs,
                requestTimeoutMs,
                maxResponseBytes,
                maxDecompressedBytes,
                maxTarEntries,
                allowInsecureHttp,
                httpClientBuilder
        );
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "apiKey=" + (apiKey != null ? "[REDACTED]" : "null") +
                ", apiKeySupplier=" + (apiKeySupplier != null ? "[CONFIGURED]" : "null") +
                ", apiBaseUrl='" + apiBaseUrl + '\'' +
                ", connectTimeoutMs=" + connectTimeoutMs +
                ", requestTimeoutMs=" + requestTimeoutMs +
                ", maxResponseBytes=" + maxResponseBytes +
                ", maxDecompressedBytes=" + maxDecompressedBytes +
                ", maxTarEntries=" + maxTarEntries +
                ", allowInsecureHttp=" + allowInsecureHttp +
                ", httpClientBuilder=" + (httpClientBuilder != null ? "[CONFIGURED]" : "null") +
                '}';
    }

    /**
     * Builder helper for constructing immutable {@link ClientConfig} objects.
     */
    public static class Builder {
        private String apiKey;
        private Supplier<String> apiKeySupplier;
        private String apiBaseUrl = resolveDefaultBaseUrl();
        private long connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        private long requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
        private long maxResponseBytes = DEFAULT_MAX_RESPONSE_BYTES;
        private long maxDecompressedBytes = DEFAULT_MAX_DECOMPRESSED_BYTES;
        private int maxTarEntries = DEFAULT_MAX_TAR_ENTRIES;
        private boolean allowInsecureHttp = DEFAULT_ALLOW_INSECURE_HTTP;
        private HttpClient.Builder httpClientBuilder;

        /**
         * Creates a new Builder instance without authentication parameters.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder instance with the specified API key.
         * 
         * @param apiKey the API key to use for authentication
         */
        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * Creates a new Builder instance with a dynamic API key supplier for runtime secret rotation.
         * 
         * @param apiKeySupplier supplier providing current API keys
         */
        public Builder(Supplier<String> apiKeySupplier) {
            this.apiKeySupplier = apiKeySupplier;
        }

        /**
         * Sets the API key.
         * 
         * @param apiKey the API key
         * @return this builder
         */
        public Builder apiKey(@Nullable String apiKey) {
            this.apiKey = apiKey;
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                this.apiKeySupplier = null; // Clear supplier to avoid mutual exclusivity collision
            }
            return this;
        }

        /**
         * Sets the dynamic API key supplier for runtime secret rotation.
         * 
         * @param apiKeySupplier the dynamic key supplier
         * @return this builder
         */
        public Builder apiKeySupplier(@Nullable Supplier<String> apiKeySupplier) {
            this.apiKeySupplier = apiKeySupplier;
            if (apiKeySupplier != null) {
                this.apiKey = null; // Clear static key to avoid mutual exclusivity collision
            }
            return this;
        }

        /**
         * Sets the base URL.
         * 
         * @param apiBaseUrl the base URL
         * @return this builder
         */
        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds.
         * 
         * @param connectTimeoutMs connection timeout in milliseconds
         * @return this builder
         */
        public Builder connectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        /**
         * Sets the request timeout in milliseconds.
         * 
         * @param requestTimeoutMs request timeout in milliseconds
         * @return this builder
         */
        public Builder requestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        /**
         * Sets the maximum allowed compressed response size in bytes.
         * 
         * @param maxResponseBytes maximum response size in bytes
         * @return this builder
         */
        public Builder maxResponseBytes(long maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
            return this;
        }

        /**
         * Sets the maximum allowed decompressed archive size in bytes.
         * 
         * @param maxDecompressedBytes maximum decompressed size in bytes
         * @return this builder
         */
        public Builder maxDecompressedBytes(long maxDecompressedBytes) {
            this.maxDecompressedBytes = maxDecompressedBytes;
            return this;
        }

        /**
         * Sets the maximum number of archive tar entries to extract.
         * 
         * @param maxTarEntries maximum tar entries
         * @return this builder
         */
        public Builder maxTarEntries(int maxTarEntries) {
            this.maxTarEntries = maxTarEntries;
            return this;
        }

        /**
         * Sets whether to allow insecure HTTP connections.
         * 
         * @param allowInsecureHttp true to allow insecure connections
         * @return this builder
         */
        public Builder allowInsecureHttp(boolean allowInsecureHttp) {
            this.allowInsecureHttp = allowInsecureHttp;
            return this;
        }

        /**
         * Sets the custom HttpClient.Builder.
         * 
         * @param httpClientBuilder the custom client builder
         * @return this builder
         */
        public Builder httpClientBuilder(HttpClient.@Nullable Builder httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
            return this;
        }

        /**
         * Sets the custom HttpClient.
         * 
         * @param httpClient the custom client
         * @return this builder
         * @deprecated java.net.http.HttpClient is strictly immutable and cannot be cloned into a builder.
         *             Use {@link #httpClientBuilder(HttpClient.Builder)} instead.
         */
        @Deprecated
        public Builder httpClient(HttpClient httpClient) {
            throw new UnsupportedOperationException(
                    "java.net.http.HttpClient is strictly immutable and cannot be cloned. " +
                    "Configure an HttpClient.Builder and pass it via httpClientBuilder(builder) instead.");
        }

        /**
         * Builds a new {@link ClientConfig} based on the configured properties.
         * 
         * @return the constructed ClientConfig
         */
        public ClientConfig build() {
            boolean hasKey = this.apiKey != null && !this.apiKey.trim().isEmpty();
            boolean hasSupplier = this.apiKeySupplier != null;
            if (!hasKey && !hasSupplier) {
                throw new IllegalArgumentException("apiKey or apiKeySupplier must be provided");
            }
            if (hasKey && hasSupplier) {
                throw new IllegalArgumentException("Provide either a static apiKey or an apiKeySupplier, not both");
            }
            if (this.connectTimeoutMs < 0) {
                throw new IllegalArgumentException("connectTimeoutMs must not be negative");
            }
            if (this.requestTimeoutMs < 0) {
                throw new IllegalArgumentException("requestTimeoutMs must not be negative");
            }
            if (this.maxResponseBytes <= 0) {
                throw new IllegalArgumentException("maxResponseBytes must be strictly positive");
            }
            if (this.maxDecompressedBytes <= 0) {
                throw new IllegalArgumentException("maxDecompressedBytes must be strictly positive");
            }
            if (this.maxTarEntries <= 0) {
                throw new IllegalArgumentException("maxTarEntries must be strictly positive");
            }
            return new ClientConfig(this);
        }
    }
}
