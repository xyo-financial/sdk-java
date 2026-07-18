package com.xyo.financial;

/**
 * Interface representing the HTTP layer utilized by {@link XyoClient}.
 * Allows developers to inject custom networking configurations.
 */
public interface HttpTransport {
    
    /**
     * Executes the given HttpRequest synchronously.
     * 
     * @param request the request model with URL, headers, and body
     * @return the response model with status, body, and headers
     * @throws XyoException if a network timeout, transport issue, or socket failure occurs
     */
    HttpResponse send(HttpRequest request);
}
