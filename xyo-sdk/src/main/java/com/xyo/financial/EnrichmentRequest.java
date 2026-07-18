package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class EnrichmentRequest {
    private final String content;
    private final String countryCode;

    @JsonCreator
    public EnrichmentRequest(
            @JsonProperty("content") String content,
            @JsonProperty("countryCode") String countryCode) {
        this.content = content;
        this.countryCode = countryCode;
    }

    public String getContent() {
        return content;
    }

    public String getCountryCode() {
        return countryCode;
    }

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

    public static class Builder {
        private String content;
        private String countryCode;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public EnrichmentRequest build() {
            return new EnrichmentRequest(content, countryCode);
        }
    }
}
