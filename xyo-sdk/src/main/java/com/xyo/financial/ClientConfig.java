package com.xyo.financial;

import java.net.http.HttpClient;

/**
 * Configuration options for the {@link XyoClient}.
 * <p>
 * Use the {@link Builder} to construct a new immutable-safe configuration.
 */
public class ClientConfig {
    
    /** The default XYO API base URL. */
    public static final String DEFAULT_API_BASE_URL = "https://api.xyo.financial";
    
    /** The default connection timeout in milliseconds (5 seconds). */
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    
    /** The default request timeout in milliseconds (30 seconds). */
    public static final long DEFAULT_REQUEST_TIMEOUT_MS = 30000;
    
    /** The default maximum allowed response size in bytes (1 MB). */
    public static final long DEFAULT_MAX_RESPONSE_BYTES = 1024 * 1024;
    
    /** Default option for allowing insecure HTTP connections. */
    public static final boolean DEFAULT_ALLOW_INSECURE_HTTP = false;

    private String apiKey;
    private String apiBaseUrl = DEFAULT_API_BASE_URL;

    private long connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    private long requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    private long maxResponseBytes = DEFAULT_MAX_RESPONSE_BYTES;
    private boolean allowInsecureHttp = DEFAULT_ALLOW_INSECURE_HTTP;
    private HttpClient httpClient;

    /**
     * Constructs a new ClientConfig with the specified API key.
     * 
     * @param apiKey the API key for authentication
     * @deprecated Use {@link Builder} instead to ensure robust initialization.
     */
    @Deprecated
    public ClientConfig(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Gets the API key.
     * 
     * @return the API key
     */
    public String getApiKey() { return apiKey; }

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
        private String apiBaseUrl = DEFAULT_API_BASE_URL;
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
            ClientConfig config = new ClientConfig(apiKey);
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
