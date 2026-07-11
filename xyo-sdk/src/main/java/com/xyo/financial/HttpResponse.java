package com.xyo.financial;

public class HttpResponse {
    private long statusCode;
    private String body;

    public HttpResponse(long statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public long getStatusCode() { return statusCode; }
    public String getBody() { return body; }
}
