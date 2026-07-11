package com.xyo.financial;

public class XyoException extends RuntimeException {
    private final ErrorCategory category;
    private final long httpStatusCode;
    private final int transportCode;

    public XyoException(ErrorCategory category, String message) {
        this(category, message, 0, 0);
    }

    public XyoException(ErrorCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.httpStatusCode = 0;
        this.transportCode = 0;
    }

    public XyoException(ErrorCategory category, String message, long httpStatusCode, int transportCode) {
        super(message);
        this.category = category;
        this.httpStatusCode = httpStatusCode;
        this.transportCode = transportCode;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public long getHttpStatusCode() {
        return httpStatusCode;
    }

    public int getTransportCode() {
        return transportCode;
    }
}
