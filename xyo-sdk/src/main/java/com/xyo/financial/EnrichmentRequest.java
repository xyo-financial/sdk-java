package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Parameters submitted to enrich a transaction description.
 * <p>
 * Use the {@link Builder} to construct immutable EnrichmentRequest instances.
 */
public class EnrichmentRequest {
    private final String content;
    private final String countryCode;

    /**
     * Constructs a new EnrichmentRequest.
     * 
     * @param content the raw description string of the transaction (e.g. "COSTA PICKUP")
     * @param countryCode the 2-letter ISO country code representing the location (e.g. "GB")
     */
    @JsonCreator
    public EnrichmentRequest(
            @JsonProperty("content") String content,
            @JsonProperty("countryCode") String countryCode) {
        this.content = content;
        this.countryCode = countryCode;
    }

    /**
     * Gets the transaction description content.
     * 
     * @return transaction content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the 2-letter ISO country code.
     * 
     * @return the ISO country code
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Validates that request fields are populated and not empty.
     * 
     * @throws XyoException if validation of content or countryCode fails
     */
    public void validate() throws XyoException {
        if (content == null || content.trim().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "content must not be null or empty");
        }
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "countryCode must not be null or empty");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrichmentRequest that = (EnrichmentRequest) o;
        return Objects.equals(content, that.content) && Objects.equals(countryCode, that.countryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, countryCode);
    }

    /**
     * Helper builder class to initialize immutable EnrichmentRequest.
     */
    public static class Builder {
        private String content;
        private String countryCode;

        /**
         * Sets transaction description content.
         * 
         * @param content transaction content
         * @return this builder
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * Sets the country code.
         * 
         * @param countryCode ISO country code
         * @return this builder
         */
        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        /**
         * Constructs the {@link EnrichmentRequest} instance.
         * 
         * @return the EnrichmentRequest
         */
        public EnrichmentRequest build() {
            return new EnrichmentRequest(content, countryCode);
        }
    }
}
