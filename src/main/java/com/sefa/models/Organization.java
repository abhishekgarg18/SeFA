package com.sefa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Organization model representing investment entities
 * Converted from Python models/org.py
 */
public class Organization {
    @JsonProperty("country_name")
    private String countryName;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("nature")
    private String nature;
    
    @JsonProperty("zip_code")
    private String zipCode;
    
    // Default constructor
    public Organization() {}
    
    // Constructor
    public Organization(String countryName, String name, String address, String nature, String zipCode) {
        this.countryName = countryName;
        this.name = name;
        this.address = address;
        this.nature = nature;
        this.zipCode = zipCode;
    }
    
    // Getters and Setters
    public String getCountryName() {
        return countryName;
    }
    
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getNature() {
        return nature;
    }
    
    public void setNature(String nature) {
        this.nature = nature;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    @Override
    public String toString() {
        return "Organization{" +
                "countryName='" + countryName + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", nature='" + nature + '\'' +
                ", zipCode='" + zipCode + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Organization that = (Organization) o;
        
        if (countryName != null ? !countryName.equals(that.countryName) : that.countryName != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (nature != null ? !nature.equals(that.nature) : that.nature != null) return false;
        return zipCode != null ? zipCode.equals(that.zipCode) : that.zipCode == null;
    }
    
    @Override
    public int hashCode() {
        int result = countryName != null ? countryName.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (nature != null ? nature.hashCode() : 0);
        result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
        return result;
    }
} 