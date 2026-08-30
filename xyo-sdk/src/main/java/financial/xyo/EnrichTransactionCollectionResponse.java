package financial.xyo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Result returned when submitting a bulk asynchronous transaction collection request.
 * Contains batch ID and tracing URL.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnrichTransactionCollectionResponse {
    private final String id;
    private final String link;

    /**
     * Constructs a new EnrichTransactionCollectionResponse.
     * 
     * @param id the unique batch tracking ID
     * @param link the REST URL pointing to the status check endpoint for this batch
     */
    @JsonCreator
    public EnrichTransactionCollectionResponse(
            @JsonProperty("id") String id,
            @JsonProperty("link") String link) {
        this.id = id;
        this.link = link;
    }

    /**
     * Gets the unique tracking batch ID.
     * 
     * @return batch id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets status check tracking URL link.
     * 
     * @return status link
     */
    public String getLink() {
        return link;
    }

    /**
     * Creates a new Builder instance for constructing {@link EnrichTransactionCollectionResponse}.
     * 
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new {@link Builder} initialized with values from this instance.
     * 
     * @return a pre-populated Builder
     */
    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .link(this.link);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrichTransactionCollectionResponse that = (EnrichTransactionCollectionResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, link);
    }

    @Override
    public String toString() {
        return "EnrichTransactionCollectionResponse{" +
                "id='" + id + '\'' +
                ", link='" + link + '\'' +
                '}';
    }

    /**
     * Helper builder class to construct immutable EnrichTransactionCollectionResponse.
     */
    public static class Builder {
        private String id;
        private String link;

        /**
         * Sets batch ID.
         * 
         * @param id batch ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets status URL link.
         * 
         * @param link status URL
         * @return this builder
         */
        public Builder link(String link) {
            this.link = link;
            return this;
        }

        /**
         * Builds the {@link EnrichTransactionCollectionResponse}.
         * 
         * @return response
         */
        public EnrichTransactionCollectionResponse build() {
            return new EnrichTransactionCollectionResponse(id, link);
        }
    }
}
