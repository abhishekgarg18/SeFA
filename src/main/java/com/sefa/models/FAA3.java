package com.sefa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FAA3 model representing Form A3 entry for ITR
 * Converted from Python models/itr/faa3.py
 */
public class FAA3 {
    @JsonProperty("org")
    private Organization org;
    
    @JsonProperty("purchase")
    private Purchase purchase;
    
    @JsonProperty("purchase_price")
    private double purchasePrice;
    
    @JsonProperty("peak_price")
    private double peakPrice;
    
    @JsonProperty("closing_price")
    private double closingPrice;
    
    @JsonProperty("sales_proceeds")
    private double salesProceeds;
    
    // Default constructor
    public FAA3() {
        this.salesProceeds = 0.0;
    }
    
    // Constructor
    public FAA3(Organization org, Purchase purchase, double purchasePrice, 
                double peakPrice, double closingPrice, double salesProceeds) {
        this.org = org;
        this.purchase = purchase;
        this.purchasePrice = purchasePrice;
        this.peakPrice = peakPrice;
        this.closingPrice = closingPrice;
        this.salesProceeds = salesProceeds;
    }
    
    // Constructor without sales proceeds (defaults to 0.0)
    public FAA3(Organization org, Purchase purchase, double purchasePrice, 
                double peakPrice, double closingPrice) {
        this(org, purchase, purchasePrice, peakPrice, closingPrice, 0.0);
    }
    
    // Getters and Setters
    public Organization getOrg() {
        return org;
    }
    
    public void setOrg(Organization org) {
        this.org = org;
    }
    
    public Purchase getPurchase() {
        return purchase;
    }
    
    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public double getPurchasePrice() {
        return purchasePrice;
    }
    
    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
    
    public double getPeakPrice() {
        return peakPrice;
    }
    
    public void setPeakPrice(double peakPrice) {
        this.peakPrice = peakPrice;
    }
    
    public double getClosingPrice() {
        return closingPrice;
    }
    
    public void setClosingPrice(double closingPrice) {
        this.closingPrice = closingPrice;
    }
    
    public double getSalesProceeds() {
        return salesProceeds;
    }
    
    public void setSalesProceeds(double salesProceeds) {
        this.salesProceeds = salesProceeds;
    }
    
    @Override
    public String toString() {
        return "FAA3{" +
                "org=" + org +
                ", purchase=" + purchase +
                ", purchasePrice=" + purchasePrice +
                ", peakPrice=" + peakPrice +
                ", closingPrice=" + closingPrice +
                ", salesProceeds=" + salesProceeds +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FAA3 faa3 = (FAA3) o;
        
        if (Double.compare(faa3.purchasePrice, purchasePrice) != 0) return false;
        if (Double.compare(faa3.peakPrice, peakPrice) != 0) return false;
        if (Double.compare(faa3.closingPrice, closingPrice) != 0) return false;
        if (Double.compare(faa3.salesProceeds, salesProceeds) != 0) return false;
        if (org != null ? !org.equals(faa3.org) : faa3.org != null) return false;
        return purchase != null ? purchase.equals(faa3.purchase) : faa3.purchase == null;
    }
    
    @Override
    public int hashCode() {
        int result;
        long temp;
        result = org != null ? org.hashCode() : 0;
        result = 31 * result + (purchase != null ? purchase.hashCode() : 0);
        temp = Double.doubleToLongBits(purchasePrice);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(peakPrice);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(closingPrice);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(salesProceeds);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
} 