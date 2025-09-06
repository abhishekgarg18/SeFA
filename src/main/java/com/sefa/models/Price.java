package com.sefa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Price model representing monetary values with currency
 * Converted from Python models/purchase.py Price class
 */
public class Price {
    @JsonProperty("price")
    private double price;
    
    @JsonProperty("currency_code")
    private String currencyCode;
    
    // Default constructor
    public Price() {}
    
    // Constructor
    public Price(double price, String currencyCode) {
        this.price = price;
        this.currencyCode = currencyCode;
    }
    
    // Getters and Setters
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    @Override
    public String toString() {
        return "Price{" +
                "price=" + price +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Price price1 = (Price) o;
        
        if (Double.compare(price1.price, price) != 0) return false;
        return currencyCode != null ? currencyCode.equals(price1.currencyCode) : price1.currencyCode == null;
    }
    
    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(price);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (currencyCode != null ? currencyCode.hashCode() : 0);
        return result;
    }
} 