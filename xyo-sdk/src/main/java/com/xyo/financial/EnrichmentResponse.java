package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result returned for a successful transaction enrichment request.
 * Contains identified merchant, description, categories, logo and location details.
 */
public class EnrichmentResponse {
    private final String merchant;
    private final String description;
    private final List<String> categories;
    private final String logo;
    private final @Nullable String location; // Optional
    private final @Nullable String address;  // Optional

    /**
     * Constructs a new EnrichmentResponse.
     * 
     * @param merchant the name of the resolved merchant
     * @param description cleaned description
     * @param categories classification categories list
     * @param logo merchant logo URL
     * @param location optional geographical location name
     * @param address optional parsed address string
     */
    @JsonCreator
    public EnrichmentResponse(
            @JsonProperty("merchant") String merchant,
            @JsonProperty("description") String description,
            @JsonProperty("categories") List<String> categories,
            @JsonProperty("logo") String logo,
            @JsonProperty("location") @Nullable String location,
            @JsonProperty("address") @Nullable String address) {
        this.merchant = merchant;
        this.description = description;
        this.categories = categories != null ? categories.stream().filter(Objects::nonNull).toList() : Collections.emptyList();
        this.logo = logo;
        this.location = location;
        this.address = address;
    }

    /**
     * Gets the resolved merchant name.
     * 
     * @return merchant name
     */
    public String getMerchant() {
        return merchant;
    }

    /**
     * Gets cleaned description.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets categories list.
     * 
     * @return categories
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Gets merchant logo image URL.
     * 
     * @return logo url
     */
    public String getLogo() {
        return logo;
    }

    /**
     * Gets optional location name.
     * 
     * @return location name, or null if not resolved
     */
    public @Nullable String getLocation() {
        return location;
    }

    /**
     * Gets optional parsed address.
     * 
     * @return address string, or null if not resolved
     */
    public @Nullable String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrichmentResponse that = (EnrichmentResponse) o;
        return Objects.equals(merchant, that.merchant) &&
                Objects.equals(description, that.description) &&
                Objects.equals(categories, that.categories) &&
                Objects.equals(logo, that.logo) &&
                Objects.equals(location, that.location) &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchant, description, categories, logo, location, address);
    }

    /**
     * Helper builder class to construct immutable EnrichmentResponse.
     */
    public static class Builder {
        private String merchant;
        private String description;
        private List<String> categories;
        private String logo;
        private String location;
        private String address;

        /**
         * Sets merchant name.
         * 
         * @param merchant merchant name
         * @return this builder
         */
        public Builder merchant(String merchant) {
            this.merchant = merchant;
            return this;
        }

        /**
         * Sets description.
         * 
         * @param description description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets categories.
         * 
         * @param categories categories list
         * @return this builder
         */
        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }

        /**
         * Sets logo URL.
         * 
         * @param logo logo URL
         * @return this builder
         */
        public Builder logo(String logo) {
            this.logo = logo;
            return this;
        }

        /**
         * Sets location.
         * 
         * @param location location name
         * @return this builder
         */
        public Builder location(String location) {
            this.location = location;
            return this;
        }

        /**
         * Sets address.
         * 
         * @param address address
         * @return this builder
         */
        public Builder address(String address) {
            this.address = address;
            return this;
        }

        /**
         * Builds the {@link EnrichmentResponse}.
         * 
         * @return response
         */
        public EnrichmentResponse build() {
            return new EnrichmentResponse(merchant, description, categories, logo, location, address);
        }
    }
}
