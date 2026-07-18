package com.xyo.financial;

/**
 * Base exception thrown for all errors encountered when using the XYO.Financial SDK.
 * Includes information about error categories, HTTP status codes, and HTTP response bodies.
 */
public class XyoException extends RuntimeException {
    private final ErrorCategory category;
    private final int httpStatusCode;
    private final int transportCode;
    private final String responseBody;

    /**
     * Constructs a new exception with category and message.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     */
    public XyoException(ErrorCategory category, String message) {
        this(category, message, null, 0, 0, null);
    }

    /**
     * Constructs a new exception with category, message, and cause.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param cause the root throwable cause
     */
    public XyoException(ErrorCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.httpStatusCode = 0;
        this.transportCode = 0;
        this.responseBody = null;
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
        this(category, message, null, httpStatusCode, transportCode, null);
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
        this(category, message, null, httpStatusCode, transportCode, responseBody);
    }

    /**
     * Full constructor for custom exception properties.
     * 
     * @param category the classification category
     * @param message the descriptive error message
     * @param cause the root throwable cause
     * @param httpStatusCode the HTTP status code
     * @param transportCode the transport error code
     * @param responseBody the raw HTTP error body returned by the API
     */
    public XyoException(ErrorCategory category, String message, Throwable cause, int httpStatusCode, int transportCode, String responseBody) {
        super(message, cause);
        this.category = category;
        this.httpStatusCode = httpStatusCode;
        this.transportCode = transportCode;
        this.responseBody = responseBody;
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
}
