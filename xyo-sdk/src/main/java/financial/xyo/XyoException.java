package financial.xyo;

import org.jspecify.annotations.Nullable;

/**
 * Base exception thrown for all errors encountered when using the XYO.Financial SDK.
 * Includes information about error categories, HTTP status codes, and HTTP response bodies.
 */
public class XyoException extends RuntimeException {
    private final ErrorCategory category;
    private final int httpStatusCode;
    private final int transportCode;
    private final @Nullable String responseBody;
    private final @Nullable Long retryAfter;
    private final @Nullable Long rateLimitLimit;
    private final @Nullable Long rateLimitRemaining;
    private final @Nullable Long rateLimitReset;

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
    public XyoException(ErrorCategory category, String message, @Nullable Throwable cause) {
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
    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode, @Nullable String responseBody) {
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
    public XyoException(ErrorCategory category, String message, @Nullable Throwable cause, int httpStatusCode, int transportCode, @Nullable String responseBody) {
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
    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode, @Nullable String responseBody, @Nullable Long retryAfter, @Nullable Long rateLimitLimit, @Nullable Long rateLimitRemaining, @Nullable Long rateLimitReset) {
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
    public XyoException(ErrorCategory category, String message, @Nullable Throwable cause, int httpStatusCode, int transportCode, @Nullable String responseBody, @Nullable Long retryAfter, @Nullable Long rateLimitLimit, @Nullable Long rateLimitRemaining, @Nullable Long rateLimitReset) {
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

    // Static factory methods for convenient instantiation
    public static XyoException validation(String message) {
        return new XyoException(ErrorCategory.VALIDATION, message);
    }

    public static XyoException validation(String message, Throwable cause) {
        return new XyoException(ErrorCategory.VALIDATION, message, cause);
    }

    public static XyoException http(String message, int httpStatusCode, @Nullable String responseBody) {
        return new XyoException(ErrorCategory.HTTP, message, null, httpStatusCode, 0, responseBody, null, null, null, null);
    }

    public static XyoException transport(String message, Throwable cause) {
        return new XyoException(ErrorCategory.TRANSPORT, message, cause);
    }

    public static XyoException parsing(String message, Throwable cause) {
        return new XyoException(ErrorCategory.PARSING, message, cause);
    }

    public static XyoException rateLimit(String message, int httpStatusCode, @Nullable Long retryAfter, @Nullable Long rateLimitLimit, @Nullable Long rateLimitRemaining, @Nullable Long rateLimitReset) {
        return new XyoException(ErrorCategory.RATE_LIMIT, message, null, httpStatusCode, 0, null, retryAfter, rateLimitLimit, rateLimitRemaining, rateLimitReset);
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
    public @Nullable String getResponseBody() {
        return responseBody;
    }

    /**
     * Gets the Retry-After header value in seconds, if returned by the server.
     * 
     * @return retry after value in seconds, or null if absent
     */
    public @Nullable Long getRetryAfter() {
        return retryAfter;
    }

    /**
     * Gets the RateLimit-Limit header value, if returned by the server.
     * 
     * @return rate limit limit, or null if absent
     */
    public @Nullable Long getRateLimitLimit() {
        return rateLimitLimit;
    }

    /**
     * Gets the RateLimit-Remaining header value, if returned by the server.
     * 
     * @return remaining rate limit quota, or null if absent
     */
    public @Nullable Long getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    /**
     * Gets the RateLimit-Reset header value, if returned by the server.
     * 
     * @return rate limit reset time in seconds, or null if absent
     */
    public @Nullable Long getRateLimitReset() {
        return rateLimitReset;
    }
}
