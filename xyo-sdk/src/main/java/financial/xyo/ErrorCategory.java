package financial.xyo;

/**
 * Categorization of errors returned or thrown by the XyoClient SDK.
 */
public enum ErrorCategory {
    /** Errors validation of method inputs, api keys, or configurations. */
    VALIDATION,
    
    /** Network connection timeouts, interruptions or socket failures. */
    TRANSPORT,
    
    /** Errors returning non-2xx status codes from the HTTP endpoint. */
    HTTP,
    
    /** Failures during JSON serialization or deserialization processes. */
    PARSING
}
