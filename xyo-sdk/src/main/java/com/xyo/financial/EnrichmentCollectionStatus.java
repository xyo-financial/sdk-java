package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static EnrichmentCollectionStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (EnrichmentCollectionStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
