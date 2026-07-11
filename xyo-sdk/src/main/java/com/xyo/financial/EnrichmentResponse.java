package com.xyo.financial;

import java.util.List;
import java.util.Objects;

public class EnrichmentResponse {
    private String merchant;
    private String description;
    private List<String> categories;
    private String logo;
    private String location; // Optional
    private String address;  // Optional

    public EnrichmentResponse() {}

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrichmentResponse that = (EnrichmentResponse) o;
        return Objects.equals(merchant, that.merchant) && Objects.equals(description, that.description) && Objects.equals(categories, that.categories) && Objects.equals(logo, that.logo) && Objects.equals(location, that.location) && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchant, description, categories, logo, location, address);
    }
}
