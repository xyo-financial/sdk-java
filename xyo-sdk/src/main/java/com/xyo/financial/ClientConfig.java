package com.xyo.financial;

import java.net.http.HttpClient;

public class ClientConfig {
    private String apiKey;
    private String apiBaseUrl = "https://api.xyo.financial";
    private HttpTransport httpTransport;

    private long connectTimeoutMs = 5000;
    private long requestTimeoutMs = 30000;
    private long maxResponseBytes = 1024 * 1024;
    private boolean allowInsecureHttp = false;
    private HttpClient httpClient;

    public ClientConfig(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public HttpTransport getHttpTransport() { return httpTransport; }
    public void setHttpTransport(HttpTransport httpTransport) { this.httpTransport = httpTransport; }

    public long getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public long getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(long requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }

    public long getMaxResponseBytes() { return maxResponseBytes; }
    public void setMaxResponseBytes(long maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }

    public boolean isAllowInsecureHttp() { return allowInsecureHttp; }
    public void setAllowInsecureHttp(boolean allowInsecureHttp) { this.allowInsecureHttp = allowInsecureHttp; }

    public HttpClient getHttpClient() { return httpClient; }
    public void setHttpClient(HttpClient httpClient) { this.httpClient = httpClient; }

    public static class Builder {
        private String apiKey;
        private String apiBaseUrl = "https://api.xyo.financial";
        private HttpTransport httpTransport;
        private long connectTimeoutMs = 5000;
        private long requestTimeoutMs = 30000;
        private long maxResponseBytes = 1024 * 1024;
        private boolean allowInsecureHttp = false;
        private HttpClient httpClient;

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        public Builder httpTransport(HttpTransport httpTransport) {
            this.httpTransport = httpTransport;
            return this;
        }

        public Builder connectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder requestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        public Builder maxResponseBytes(long maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
            return this;
        }

        public Builder allowInsecureHttp(boolean allowInsecureHttp) {
            this.allowInsecureHttp = allowInsecureHttp;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public ClientConfig build() {
            ClientConfig config = new ClientConfig(apiKey);
            config.setApiBaseUrl(apiBaseUrl);
            config.setHttpTransport(httpTransport);
            config.setConnectTimeoutMs(connectTimeoutMs);
            config.setRequestTimeoutMs(requestTimeoutMs);
            config.setMaxResponseBytes(maxResponseBytes);
            config.setAllowInsecureHttp(allowInsecureHttp);
            config.setHttpClient(httpClient);
            return config;
        }
    }
}
