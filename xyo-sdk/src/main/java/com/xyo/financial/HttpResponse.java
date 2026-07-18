package com.xyo.financial;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Representation of an HTTP response returned by the transport layer.
 */
public class HttpResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    /**
     * Legacy constructor for backward compatibility.
     * 
     * @param statusCode HTTP response status code
     * @param body HTTP response body content
     */
    public HttpResponse(long statusCode, String body) {
        this((int) statusCode, body, Collections.emptyMap());
    }

    /**
     * Constructs a new HttpResponse instance.
     * 
     * @param statusCode HTTP status code
     * @param body response body payload
     * @param headers response headers mapping
     */
    public HttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? Map.copyOf(headers) : Collections.emptyMap();
    }

    /**
     * Gets the HTTP response status code.
     * 
     * @return status code
     */
    public int getStatusCode() { return statusCode; }

    /**
     * Gets response body payload.
     * 
     * @return body string
     */
    public String getBody() { return body; }

    /**
     * Gets HTTP response headers.
     * 
     * @return headers map
     */
    public Map<String, List<String>> getHeaders() { return headers; }
}
