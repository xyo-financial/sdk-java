package financial.xyo;

/**
 * Base exception thrown for all errors encountered when using the XYO.Financial SDK.
 * Includes information about error categories, HTTP status codes, and HTTP response bodies.
 */
public class XyoException extends RuntimeException {
    private final ErrorCategory category;
    private final int httpStatusCode;
    private final int transportCode;
    private final String responseBody;
    private final Long retryAfter;
    private final Long rateLimitLimit;
    private final Long rateLimitRemaining;
    private final Long rateLimitReset;

    /**
     * Constructs a new exception with category and message.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     */
    public XyoException(ErrorCategory category, String message) {
        this(category, message, null, 0, 0, null, null, null, null, null);
    }

    /**
     * Constructs a new exception with category, message, and cause.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param cause the root throwable cause
     */
    public XyoException(ErrorCategory category, String message, Throwable cause) {
        this(category, message, cause, 0, 0, null, null, null, null, null);
    }

    /**
     * Constructs a new exception with status code details.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param httpStatusCode the HTTP status code
     * @param transportCode the transport error code
     */
    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode) {
        this(category, message, null, httpStatusCode, transportCode, null, null, null, null, null);
    }

    /**
     * Constructs a new exception with status code details and response body.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param httpStatusCode the HTTP status code
     * @param transportCode the transport error code
     * @param responseBody the raw HTTP error body returned by the API
     */
    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode, String responseBody) {
        this(category, message, null, httpStatusCode, transportCode, responseBody, null, null, null, null);
    }

    /**
     * Full constructor for custom exception properties without rate limit headers.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param cause the root throwable cause
     * @param httpStatusCode the HTTP status code
     * @param transportCode the transport error code
     * @param responseBody the raw HTTP error body returned by the API
     */
    public XyoException(ErrorCategory category, String message, Throwable cause, int httpStatusCode, int transportCode, String responseBody) {
        this(category, message, cause, httpStatusCode, transportCode, responseBody, null, null, null, null);
    }

    /**
     * Constructs a new exception with status code details, response body, and rate limit headers.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param httpStatusCode the HTTP status code
     * @param transportCode the transport error code
     * @param responseBody the raw HTTP error body returned by the API
     * @param retryAfter the Retry-After header value in seconds
     * @param rateLimitLimit the RateLimit-Limit header value
     * @param rateLimitRemaining the RateLimit-Remaining header value
     * @param rateLimitReset the RateLimit-Reset header value
     */
    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode, String responseBody, Long retryAfter, Long rateLimitLimit, Long rateLimitRemaining, Long rateLimitReset) {
        this(category, message, null, httpStatusCode, transportCode, responseBody, retryAfter, rateLimitLimit, rateLimitRemaining, rateLimitReset);
    }

    /**
     * Full constructor for custom exception properties including rate limit headers.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param cause the root throwable cause
     * @param httpStatusCode the HTTP status code
     * @param transportCode the transport error code
     * @param responseBody the raw HTTP error body returned by the API
     * @param retryAfter the Retry-After header value in seconds
     * @param rateLimitLimit the RateLimit-Limit header value
     * @param rateLimitRemaining the RateLimit-Remaining header value
     * @param rateLimitReset the RateLimit-Reset header value
     */
    public XyoException(ErrorCategory category, String message, Throwable cause, int httpStatusCode, int transportCode, String responseBody, Long retryAfter, Long rateLimitLimit, Long rateLimitRemaining, Long rateLimitReset) {
        super(message, cause);
        this.category = category;
        this.httpStatusCode = httpStatusCode;
        this.transportCode = transportCode;
        this.responseBody = responseBody;
        this.retryAfter = retryAfter;
        this.rateLimitLimit = rateLimitLimit;
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitReset = rateLimitReset;
    }

    /**
     * Gets the error category.
     * 
     * @return the error category
     */
    public ErrorCategory getCategory() {
        return category;
    }

    /**
     * Gets the HTTP status code, if applicable (otherwise 0).
     * 
     * @return the HTTP status code
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Gets the transport code, if applicable (otherwise 0).
     * 
     * @return the transport code
     */
    public int getTransportCode() {
        return transportCode;
    }

    /**
     * Gets the response body returned by the server on error.
     * 
     * @return the error response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Gets the Retry-After header value in seconds, if returned by the server.
     * 
     * @return retry after value in seconds, or null if absent
     */
    public Long getRetryAfter() {
        return retryAfter;
    }

    /**
     * Gets the RateLimit-Limit header value, if returned by the server.
     * 
     * @return rate limit limit, or null if absent
     */
    public Long getRateLimitLimit() {
        return rateLimitLimit;
    }

    /**
     * Gets the RateLimit-Remaining header value, if returned by the server.
     * 
     * @return remaining rate limit quota, or null if absent
     */
    public Long getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    /**
     * Gets the RateLimit-Reset header value, if returned by the server.
     * 
     * @return rate limit reset time in seconds, or null if absent
     */
    public Long getRateLimitReset() {
        return rateLimitReset;
    }
}
