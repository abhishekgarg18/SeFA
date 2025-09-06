package com.sefa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Purchase model representing stock purchase transactions
 * Converted from Python models/purchase.py Purchase class
 */
public class Purchase {
    @JsonProperty("date")
    private DateObj date;
    
    @JsonProperty("purchase_fmv")
    private Price purchaseFmv;
    
    @JsonProperty("quantity")
    private double quantity;
    
    @JsonProperty("ticker")
    private String ticker;
    
    // Default constructor
    public Purchase() {}
    
    // Constructor
    public Purchase(DateObj date, Price purchaseFmv, double quantity, String ticker) {
        this.date = date;
        this.purchaseFmv = purchaseFmv;
        this.quantity = quantity;
        this.ticker = ticker;
    }
    
    // Getters and Setters
    public DateObj getDate() {
        return date;
    }
    
    public void setDate(DateObj date) {
        this.date = date;
    }
    
    public Price getPurchaseFmv() {
        return purchaseFmv;
    }
    
    public void setPurchaseFmv(Price purchaseFmv) {
        this.purchaseFmv = purchaseFmv;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
    
    @Override
    public String toString() {
        return "Purchase{" +
                "date=" + date +
                ", purchaseFmv=" + purchaseFmv +
                ", quantity=" + quantity +
                ", ticker='" + ticker + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Purchase purchase = (Purchase) o;
        
        if (Double.compare(purchase.quantity, quantity) != 0) return false;
        if (date != null ? !date.equals(purchase.date) : purchase.date != null) return false;
        if (purchaseFmv != null ? !purchaseFmv.equals(purchase.purchaseFmv) : purchase.purchaseFmv != null)
            return false;
        return ticker != null ? ticker.equals(purchase.ticker) : purchase.ticker == null;
    }
    
    @Override
    public int hashCode() {
        int result;
        long temp;
        result = date != null ? date.hashCode() : 0;
        result = 31 * result + (purchaseFmv != null ? purchaseFmv.hashCode() : 0);
        temp = Double.doubleToLongBits(quantity);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (ticker != null ? ticker.hashCode() : 0);
        return result;
    }
} 