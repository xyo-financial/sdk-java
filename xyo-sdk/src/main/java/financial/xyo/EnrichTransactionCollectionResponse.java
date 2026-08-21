package financial.xyo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Result returned when submitting a bulk asynchronous transaction collection request.
 * Contains batch ID and tracing URL.
 */
public class EnrichTransactionCollectionResponse {
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
