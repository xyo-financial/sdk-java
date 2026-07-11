package com.xyo.financial;

import java.util.Objects;

public class EnrichmentRequest {
    private String content;
    private String countryCode;

    public EnrichmentRequest() {}

    public EnrichmentRequest(String content, String countryCode) {
        this.content = content;
        this.countryCode = countryCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
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
}
