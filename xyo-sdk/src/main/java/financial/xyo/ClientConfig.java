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

    /** Default maximum number of entries permitted when unpacking results archives (10,000). */
    public static final int DEFAULT_MAX_TAR_ENTRIES = 10000;
    
    /** The default connection timeout in milliseconds (5 seconds). */
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    
    /** The default request timeout in milliseconds (30 seconds). */
    public static final long DEFAULT_REQUEST_TIMEOUT_MS = 30000;
    
    /** The default maximum allowed response size in bytes (1 MB). */
    public static final long DEFAULT_MAX_RESPONSE_BYTES = 1024 * 1024;
    
    /** Default option for allowing insecure HTTP connections. */
    public static final boolean DEFAULT_ALLOW_INSECURE_HTTP = false;

    private final @Nullable String apiKey;
    private final @Nullable Supplier<String> apiKeySupplier;
    private final String apiBaseUrl;
    private final long connectTimeoutMs;
    private final long requestTimeoutMs;
    private final long maxResponseBytes;
    private final boolean allowInsecureHttp;
    private final @Nullable HttpClient httpClient;

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
        this.allowInsecureHttp = builder.allowInsecureHttp;
        this.httpClient = builder.httpClient;
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
     * @return the API key supplier, or null if a static key was supplied
     */
    public @Nullable Supplier<String> getApiKeySupplier() {
        return apiKeySupplier;
    }

    /**
     * Gets the API base URL.
     * 
     * @return the base URL
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * Gets the connection timeout in milliseconds.
     * 
     * @return the connection timeout
     */
    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * Gets the request timeout in milliseconds.
     * 
     * @return the request timeout
     */
    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    /**
     * Gets the maximum allowed response body size in bytes.
     * 
     * @return the maximum allowed response size
     */
    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }

    /**
     * Checks if insecure HTTP connections are allowed.
     * 
     * @return true if insecure HTTP is allowed
     */
    public boolean isAllowInsecureHttp() {
        return allowInsecureHttp;
    }

    /**
     * Gets the custom {@link HttpClient}.
     * 
     * @return the client
     */
    public @Nullable HttpClient getHttpClient() {
        return httpClient;
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
        b.allowInsecureHttp = this.allowInsecureHttp;
        b.httpClient = this.httpClient;
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientConfig that = (ClientConfig) o;
        return connectTimeoutMs == that.connectTimeoutMs &&
                requestTimeoutMs == that.requestTimeoutMs &&
                maxResponseBytes == that.maxResponseBytes &&
                allowInsecureHttp == that.allowInsecureHttp &&
                Objects.equals(apiKey, that.apiKey) &&
                Objects.equals(apiKeySupplier, that.apiKeySupplier) &&
                Objects.equals(apiBaseUrl, that.apiBaseUrl) &&
                Objects.equals(httpClient, that.httpClient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, apiKeySupplier, apiBaseUrl, connectTimeoutMs, requestTimeoutMs, maxResponseBytes, allowInsecureHttp, httpClient);
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
                ", allowInsecureHttp=" + allowInsecureHttp +
                ", httpClient=" + httpClient +
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
        private boolean allowInsecureHttp = DEFAULT_ALLOW_INSECURE_HTTP;
        private HttpClient httpClient;

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
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the dynamic API key supplier for runtime secret rotation.
         * 
         * @param apiKeySupplier the dynamic key supplier
         * @return this builder
         */
        public Builder apiKeySupplier(Supplier<String> apiKeySupplier) {
            this.apiKeySupplier = apiKeySupplier;
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
         * Sets the connection timeout.
         * 
         * @param connectTimeoutMs connection timeout in milliseconds
         * @return this builder
         */
        public Builder connectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        /**
         * Sets the request timeout.
         * 
         * @param requestTimeoutMs request timeout in milliseconds
         * @return this builder
         */
        public Builder requestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        /**
         * Sets the maximum response size.
         * 
         * @param maxResponseBytes maximum response size in bytes
         * @return this builder
         */
        public Builder maxResponseBytes(long maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
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
         * Sets the custom HttpClient.
         * 
         * @param httpClient the custom client
         * @return this builder
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Builds a new {@link ClientConfig} based on the configured properties.
         * 
         * @return the constructed ClientConfig
         */
        public ClientConfig build() {
            if ((this.apiKey == null || this.apiKey.trim().isEmpty()) && this.apiKeySupplier == null) {
                throw new IllegalArgumentException("apiKey or apiKeySupplier must be provided");
            }
            return new ClientConfig(this);
        }
    }
}
