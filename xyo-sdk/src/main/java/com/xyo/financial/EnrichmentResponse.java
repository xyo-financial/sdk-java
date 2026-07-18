package com.xyo.financial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EnrichmentResponse {
    private final String merchant;
    private final String description;
    private final List<String> categories;
    private final String logo;
    private final @Nullable String location; // Optional
    private final @Nullable String address;  // Optional

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
        this.categories = categories != null ? List.copyOf(categories) : Collections.emptyList();
        this.logo = logo;
        this.location = location;
        this.address = address;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getLogo() {
        return logo;
    }

    public @Nullable String getLocation() {
        return location;
    }

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

    public static class Builder {
        private String merchant;
        private String description;
        private List<String> categories;
        private String logo;
        private String location;
        private String address;

        public Builder merchant(String merchant) {
            this.merchant = merchant;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }

        public Builder logo(String logo) {
            this.logo = logo;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public EnrichmentResponse build() {
            return new EnrichmentResponse(merchant, description, categories, logo, location, address);
        }
    }
}
