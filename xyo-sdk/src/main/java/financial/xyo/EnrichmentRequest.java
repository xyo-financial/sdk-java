package financial.xyo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import java.util.Objects;

/**
 * Parameters submitted to enrich a transaction description.
 * <p>
 * Enforces domain invariants and canonicalization at construction time (fail-fast).
 * Use the {@link Builder} to construct immutable EnrichmentRequest instances.
 */
public final class EnrichmentRequest {
    private final String content;
    private final String countryCode;

    /**
     * Constructs a new EnrichmentRequest with trimmed and normalized parameters.
     * 
     * @param content the raw description string of the transaction (e.g. "COSTA PICKUP")
     * @param countryCode the 2-letter ISO country code representing the location (e.g. "GB")
     * @throws XyoException if input validation fails
     */
    @JsonCreator
    public EnrichmentRequest(
            @JsonProperty("content") String content,
            @JsonProperty("countryCode") String countryCode) {
        this.content = content != null ? content.trim() : null;
        this.countryCode = countryCode != null ? countryCode.trim().toUpperCase(Locale.ROOT) : null;
        validate();
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

    private static boolean isAlpha2(String s) {
        if (s == null || s.length() != 2) {
            return false;
        }
        char c1 = s.charAt(0);
        char c2 = s.charAt(1);
        return ((c1 >= 'A' && c1 <= 'Z') || (c1 >= 'a' && c1 <= 'z'))
                && ((c2 >= 'A' && c2 <= 'Z') || (c2 >= 'a' && c2 <= 'z'));
    }

    /**
     * Validates that request fields are populated, conform to ISO 3166-1 alpha-2 format, and do not exceed length limits.
     * 
     * @throws XyoException if validation of content or countryCode fails
     */
    public void validate() throws XyoException {
        if (content == null || content.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "content must not be null or empty");
        }
        if (content.codePointCount(0, content.length()) > 128) {
            throw new XyoException(ErrorCategory.VALIDATION, "content must not exceed 128 characters");
        }
        if (countryCode == null || countryCode.isEmpty()) {
            throw new XyoException(ErrorCategory.VALIDATION, "countryCode must not be null or empty");
        }
        if (!isAlpha2(countryCode)) {
            throw new XyoException(ErrorCategory.VALIDATION, "countryCode must be a 2-letter ISO 3166-1 alpha-2 country code");
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

    @Override
    public String toString() {
        return "EnrichmentRequest{content=[REDACTED], countryCode='" + countryCode + "'}";
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
