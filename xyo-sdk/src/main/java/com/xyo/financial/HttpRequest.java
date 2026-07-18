package com.xyo.financial;

import java.util.List;
import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String url;
    private final Map<String, List<String>> headers;
    private final String body;

    public HttpRequest(String method, String url, Map<String, List<String>> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers != null ? Map.copyOf(headers) : Map.of();
        this.body = body;
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public String getBody() { return body; }

    @Override
    public String toString() {
        java.util.Map<String, List<String>> redactedHeaders = new java.util.HashMap<>();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("Authorization")) {
                    redactedHeaders.put(entry.getKey(), List.of("[REDACTED]"));
                } else {
                    redactedHeaders.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return "HttpRequest{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", headers=" + redactedHeaders +
                ", body='" + body + '\'' +
                '}';
    }
}
