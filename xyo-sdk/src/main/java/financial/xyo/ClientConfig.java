package financial.xyo;

import java.net.http.HttpClient;

/**
 * Configuration options for the {@link XyoClient}.
 * <p>
 * Use the {@link Builder} to construct a new immutable-safe configuration.
 */
public class ClientConfig {
    
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

    private String apiKey;
    private java.util.function.Supplier<String> apiKeySupplier;
    private String apiBaseUrl = resolveDefaultBaseUrl();

    private long connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    private long requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    private long maxResponseBytes = DEFAULT_MAX_RESPONSE_BYTES;
    private boolean allowInsecureHttp = DEFAULT_ALLOW_INSECURE_HTTP;
    private HttpClient httpClient;

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
     * Constructs a new ClientConfig with the specified API key.
     * 
     * @param apiKey the API key for authentication
     * @deprecated Use {@link Builder} instead to ensure robust initialization.
     */
    @Deprecated
    public ClientConfig(String apiKey) {
        this.apiKey = apiKey;
        this.apiBaseUrl = resolveDefaultBaseUrl();
    }

    /**
     * Gets the API key. If an {@link java.util.function.Supplier} was configured, executes the supplier to fetch the current key.
     * 
     * @return the API key
     */
    public String getApiKey() {
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
    public java.util.function.Supplier<String> getApiKeySupplier() {
        return apiKeySupplier;
    }

    /**
     * Sets the API key.
     * 
     * @param apiKey the API key
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    /**
     * Gets the API base URL.
     * 
     * @return the base URL
     */
    public String getApiBaseUrl() { return apiBaseUrl; }

    /**
     * Sets the API base URL.
     * 
     * @param apiBaseUrl the base URL
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    /**
     * Gets the connection timeout in milliseconds.
     * 
     * @return the connection timeout
     */
    public long getConnectTimeoutMs() { return connectTimeoutMs; }

    /**
     * Sets the connection timeout in milliseconds.
     * 
     * @param connectTimeoutMs the connection timeout
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    /**
     * Gets the request timeout in milliseconds.
     * 
     * @return the request timeout
     */
    public long getRequestTimeoutMs() { return requestTimeoutMs; }

    /**
     * Sets the request timeout in milliseconds.
     * 
     * @param requestTimeoutMs the request timeout
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setRequestTimeoutMs(long requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }

    /**
     * Gets the maximum allowed response body size in bytes.
     * 
     * @return the maximum allowed response size
     */
    public long getMaxResponseBytes() { return maxResponseBytes; }

    /**
     * Sets the maximum allowed response body size in bytes.
     * 
     * @param maxResponseBytes the maximum allowed response size
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setMaxResponseBytes(long maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }

    /**
     * Checks if insecure HTTP connections are allowed.
     * 
     * @return true if insecure HTTP is allowed
     */
    public boolean isAllowInsecureHttp() { return allowInsecureHttp; }

    /**
     * Sets whether to allow insecure HTTP connections.
     * 
     * @param allowInsecureHttp true to allow insecure HTTP
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setAllowInsecureHttp(boolean allowInsecureHttp) { this.allowInsecureHttp = allowInsecureHttp; }

    /**
     * Gets the custom {@link HttpClient}.
     * 
     * @return the client
     */
    public HttpClient getHttpClient() { return httpClient; }

    /**
     * Sets the custom {@link HttpClient}.
     * 
     * @param httpClient the custom client
     * @deprecated Use {@link Builder} configuration.
     */
    @Deprecated
    public void setHttpClient(HttpClient httpClient) { this.httpClient = httpClient; }

    /**
     * Builder helper for constructing {@link ClientConfig} objects.
     */
    public static class Builder {
        private String apiKey;
        private java.util.function.Supplier<String> apiKeySupplier;
        private String apiBaseUrl = resolveDefaultBaseUrl();
        private long connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        private long requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
        private long maxResponseBytes = DEFAULT_MAX_RESPONSE_BYTES;
        private boolean allowInsecureHttp = DEFAULT_ALLOW_INSECURE_HTTP;
        private HttpClient httpClient;

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
        public Builder(java.util.function.Supplier<String> apiKeySupplier) {
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
        public Builder apiKeySupplier(java.util.function.Supplier<String> apiKeySupplier) {
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
         * Sets the connect timeout.
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
            ClientConfig config = new ClientConfig(apiKey);
            config.apiKeySupplier = this.apiKeySupplier;
            config.apiBaseUrl = this.apiBaseUrl;
            config.connectTimeoutMs = this.connectTimeoutMs;
            config.requestTimeoutMs = this.requestTimeoutMs;
            config.maxResponseBytes = this.maxResponseBytes;
            config.allowInsecureHttp = this.allowInsecureHttp;
            config.httpClient = this.httpClient;
            return config;
        }
    }
}
