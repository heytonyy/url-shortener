package com.urlshortener.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for shortening a URL.
 * This is what the client sends to create a short URL.
 */
public class ShortenUrlRequest {
    
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String url;
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", 
             message = "Custom code must be 3-50 characters and contain only letters, numbers, dash, and underscore")
    private String customCode;  // Optional - for authenticated users
    
    private Integer expirationDays;  // Optional - how many days until URL expires

    // Constructors
    public ShortenUrlRequest() {
    }

    public ShortenUrlRequest(String url, String customCode, Integer expirationDays) {
        this.url = url;
        this.customCode = customCode;
        this.expirationDays = expirationDays;
    }

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expirationDays = expirationDays;
    }

    @Override
    public String toString() {
        return "ShortenUrlRequest{" +
                "url='" + url + '\'' +
                ", customCode='" + customCode + '\'' +
                ", expirationDays=" + expirationDays +
                '}';
    }
}