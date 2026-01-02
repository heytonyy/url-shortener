package com.urlshortener.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for URL details.
 * Used when fetching a user's URLs or URL details.
 */
public class UrlResponse {
    
    private Long id;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer clickCount;
    private Boolean isActive;
    private List<String> customAliases;  // List of custom codes for this URL
    
    // Constructors
    public UrlResponse() {
        this.customAliases = new ArrayList<>();
    }

    public UrlResponse(Long id, String shortCode, String originalUrl, LocalDateTime createdAt, 
                       LocalDateTime expiresAt, Integer clickCount, Boolean isActive) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.isActive = isActive;
        this.customAliases = new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<String> getCustomAliases() {
        return customAliases;
    }

    public void setCustomAliases(List<String> customAliases) {
        this.customAliases = customAliases;
    }

    @Override
    public String toString() {
        return "UrlResponse{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", clickCount=" + clickCount +
                ", customAliases=" + customAliases +
                '}';
    }
}