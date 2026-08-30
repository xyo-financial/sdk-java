package financial.xyo;

import java.util.UUID;

/**
 * Configuration options for individual API requests, supporting distributed tracing parameters
 * and custom header values.
 */
public final class RequestOptions {

    private final UUID correlationId;
    private final String traceparent;
    private final String apiUser;

    /**
     * Constructs a new RequestOptions instance with no tracing headers or api user.
     */
    public RequestOptions() {
        this(null, null, null);
    }

    /**
     * Constructs a new RequestOptions with distributed tracing parameters.
     * 
     * @param correlationId the correlation UUID for request tracing across services
     * @param traceparent the W3C traceparent header string
     */
    public RequestOptions(UUID correlationId, String traceparent) {
        this(correlationId, traceparent, null);
    }

    /**
     * Constructs a new RequestOptions with distributed tracing parameters and API user identifier.
     * 
     * @param correlationId the correlation UUID for request tracing across services
     * @param traceparent the W3C traceparent header string
     * @param apiUser optional user/tenant identifier header value
     */
    public RequestOptions(UUID correlationId, String traceparent, String apiUser) {
        this.correlationId = correlationId;
        this.traceparent = traceparent;
        this.apiUser = apiUser;
    }

    /**
     * Gets the correlation ID UUID.
     * 
     * @return the correlation ID, or null if unset
     */
    public UUID getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the W3C traceparent header string.
     * 
     * @return the traceparent string, or null if unset
     */
    public String getTraceparent() {
        return traceparent;
    }

    /**
     * Gets the optional API user/tenant identifier header value.
     * 
     * @return the API user, or null if unset
     */
    public String getApiUser() {
        return apiUser;
    }

    /**
     * Creates a new {@link Builder} initialized with values from this instance.
     * 
     * @return a pre-populated Builder
     */
    public Builder toBuilder() {
        return new Builder()
                .correlationId(this.correlationId)
                .traceparent(this.traceparent)
                .apiUser(this.apiUser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestOptions that = (RequestOptions) o;
        return java.util.Objects.equals(correlationId, that.correlationId) &&
               java.util.Objects.equals(traceparent, that.traceparent) &&
               java.util.Objects.equals(apiUser, that.apiUser);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(correlationId, traceparent, apiUser);
    }

    @Override
    public String toString() {
        return "RequestOptions{" +
                "correlationId=" + correlationId +
                ", traceparent='" + traceparent + '\'' +
                ", apiUser='" + apiUser + '\'' +
                '}';
    }

    /**
     * Creates a new Builder instance for constructing RequestOptions.
     * 
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder helper for constructing {@link RequestOptions} objects.
     */
    public static class Builder {
        private UUID correlationId;
        private String traceparent;
        private String apiUser;

        /**
         * Sets the correlation ID UUID.
         * 
         * @param correlationId correlation identifier
         * @return this builder
         */
        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /**
         * Sets the W3C traceparent header string.
         * 
         * @param traceparent W3C traceparent string
         * @return this builder
         */
        public Builder traceparent(String traceparent) {
            this.traceparent = traceparent;
            return this;
        }

        /**
         * Sets the API user/tenant identifier.
         * 
         * @param apiUser API user header value
         * @return this builder
         */
        public Builder apiUser(String apiUser) {
            this.apiUser = apiUser;
            return this;
        }

        /**
         * Builds a new {@link RequestOptions} instance.
         * 
         * @return constructed RequestOptions
         */
        public RequestOptions build() {
            return new RequestOptions(correlationId, traceparent, apiUser);
        }
    }
}
