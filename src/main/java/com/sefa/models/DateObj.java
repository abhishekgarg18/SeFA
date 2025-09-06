package com.sefa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DateObj model representing date information with multiple formats
 * Converted from Python utils/date_utils.py DateObj TypedDict
 */
public class DateObj {
    @JsonProperty("time_in_millis")
    private long timeInMillis;
    
    @JsonProperty("disp_time")
    private String dispTime;
    
    @JsonProperty("orig_disp_time")
    private String origDispTime;
    
    // Default constructor
    public DateObj() {}
    
    // Constructor
    public DateObj(long timeInMillis, String dispTime, String origDispTime) {
        this.timeInMillis = timeInMillis;
        this.dispTime = dispTime;
        this.origDispTime = origDispTime;
    }
    
    // Getters and Setters
    public long getTimeInMillis() {
        return timeInMillis;
    }
    
    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }
    
    public String getDispTime() {
        return dispTime;
    }
    
    public void setDispTime(String dispTime) {
        this.dispTime = dispTime;
    }
    
    public String getOrigDispTime() {
        return origDispTime;
    }
    
    public void setOrigDispTime(String origDispTime) {
        this.origDispTime = origDispTime;
    }
    
    @Override
    public String toString() {
        return "DateObj{" +
                "timeInMillis=" + timeInMillis +
                ", dispTime='" + dispTime + '\'' +
                ", origDispTime='" + origDispTime + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DateObj dateObj = (DateObj) o;
        
        if (timeInMillis != dateObj.timeInMillis) return false;
        if (dispTime != null ? !dispTime.equals(dateObj.dispTime) : dateObj.dispTime != null) return false;
        return origDispTime != null ? origDispTime.equals(dateObj.origDispTime) : dateObj.origDispTime == null;
    }
    
    @Override
    public int hashCode() {
        int result = (int) (timeInMillis ^ (timeInMillis >>> 32));
        result = 31 * result + (dispTime != null ? dispTime.hashCode() : 0);
        result = 31 * result + (origDispTime != null ? origDispTime.hashCode() : 0);
        return result;
    }
} 