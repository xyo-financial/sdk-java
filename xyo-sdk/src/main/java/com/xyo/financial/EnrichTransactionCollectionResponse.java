package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class EnrichTransactionCollectionResponse {
    private final String id;
    private final String link;

    @JsonCreator
    public EnrichTransactionCollectionResponse(
            @JsonProperty("id") String id,
            @JsonProperty("link") String link) {
        this.id = id;
        this.link = link;
    }

    public String getId() {
        return id;
    }

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

    public static class Builder {
        private String id;
        private String link;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public EnrichTransactionCollectionResponse build() {
            return new EnrichTransactionCollectionResponse(id, link);
        }
    }
}
