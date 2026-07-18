package com.xyo.financial;

public class XyoException extends RuntimeException {
    private final ErrorCategory category;
    private final int httpStatusCode;
    private final int transportCode;
    private final String responseBody;

    public XyoException(ErrorCategory category, String message) {
        this(category, message, null, 0, 0, null);
    }

    public XyoException(ErrorCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.httpStatusCode = 0;
        this.transportCode = 0;
        this.responseBody = null;
    }

    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode) {
        this(category, message, null, httpStatusCode, transportCode, null);
    }

    public XyoException(ErrorCategory category, String message, int httpStatusCode, int transportCode, String responseBody) {
        this(category, message, null, httpStatusCode, transportCode, responseBody);
    }

    public XyoException(ErrorCategory category, String message, Throwable cause, int httpStatusCode, int transportCode, String responseBody) {
        super(message, cause);
        this.category = category;
        this.httpStatusCode = httpStatusCode;
        this.transportCode = transportCode;
        this.responseBody = responseBody;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public int getTransportCode() {
        return transportCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
