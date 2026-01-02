package com.urlshortener.api.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for custom alias operations.
 * Returned when creating or updating a custom alias.
 */
public class CustomAliasResponse {
    
    private Long id;
    private Long urlId;
    private String customCode;
    private String customUrl;  // Full URL with custom code: http://localhost:8080/my-blog
    private LocalDateTime createdAt;
    private Boolean isActive;

    // Constructors
    public CustomAliasResponse() {
    }

    public CustomAliasResponse(Long id, Long urlId, String customCode, String customUrl, 
                               LocalDateTime createdAt, Boolean isActive) {
        this.id = id;
        this.urlId = urlId;
        this.customCode = customCode;
        this.customUrl = customUrl;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUrlId() {
        return urlId;
    }

    public void setUrlId(Long urlId) {
        this.urlId = urlId;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public String getCustomUrl() {
        return customUrl;
    }

    public void setCustomUrl(String customUrl) {
        this.customUrl = customUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "CustomAliasResponse{" +
                "id=" + id +
                ", urlId=" + urlId +
                ", customCode='" + customCode + '\'' +
                ", customUrl='" + customUrl + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}