package com.xyo.financial;

import java.util.List;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String url;
    private Map<String, List<String>> headers;
    private String body;

    public HttpRequest(String method, String url, Map<String, List<String>> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public String getBody() { return body; }
}
