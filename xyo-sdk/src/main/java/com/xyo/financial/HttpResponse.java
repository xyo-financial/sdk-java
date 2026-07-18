package com.xyo.financial;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public HttpResponse(long statusCode, String body) {
        this((int) statusCode, body, Collections.emptyMap());
    }

    public HttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? Map.copyOf(headers) : Collections.emptyMap();
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public Map<String, List<String>> getHeaders() { return headers; }
}
