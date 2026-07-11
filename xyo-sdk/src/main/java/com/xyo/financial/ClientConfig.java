package com.xyo.financial;

public class ClientConfig {
    private String apiKey;
    private String apiBaseUrl = "https://api.xyo.financial";
    private HttpTransport httpTransport;

    private long connectTimeoutMs = 5000;
    private long requestTimeoutMs = 30000;
    private long maxResponseBytes = 1024 * 1024;
    private boolean allowInsecureHttp = false;

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
}
