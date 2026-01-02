package com.urlshortener.api.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for shortened URL creation.
 * This is what the API returns after creating a short URL.
 */
public class ShortenedUrlResponse {
    
    private Long id;
    private String shortCode;
    private String customCode;  // Optional - only if user created a custom alias
    private String shortUrl;    // Full URL: http://localhost:8080/abc
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Constructors
    public ShortenedUrlResponse() {
    }

    public ShortenedUrlResponse(Long id, String shortCode, String customCode, String shortUrl, 
                                String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.shortCode = shortCode;
        this.customCode = customCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
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

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
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

    @Override
    public String toString() {
        return "ShortenedUrlResponse{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", customCode='" + customCode + '\'' +
                ", shortUrl='" + shortUrl + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}