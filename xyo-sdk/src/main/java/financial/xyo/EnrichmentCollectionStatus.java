package financial.xyo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the status of a transaction collection enrichment request.
 */
public enum EnrichmentCollectionStatus {
    /** The enrichment has finished successfully and results are available. */
    READY("READY"),
    
    /** The enrichment failed during processing. */
    FAILED("FAILED"),
    
    /** The enrichment is still running. */
    PENDING("PENDING");

    private final String value;

    EnrichmentCollectionStatus(String value) {
        this.value = value;
    }

    /**
     * Gets the raw string representation.
     * 
     * @return the string value
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Factory method to deserialize string value to status enum.
     * 
     * @param value the raw status string
     * @return the mapped enum instance
     * @throws IllegalArgumentException if the value is null or unrecognized
     */
    @JsonCreator
    public static EnrichmentCollectionStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Status value must not be null");
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
