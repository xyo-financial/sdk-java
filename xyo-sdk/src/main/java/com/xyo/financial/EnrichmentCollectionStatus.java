package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EnrichmentCollectionStatus {
    READY("READY"),
    FAILED("FAILED"),
    PENDING("PENDING");

    private final String value;

    EnrichmentCollectionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
