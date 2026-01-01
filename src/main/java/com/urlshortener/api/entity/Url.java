package com.urlshortener.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Url entity representing a shortened URL.
 * Can be created by guests (user = null) or authenticated users.
 */
@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_urls_short_code", columnList = "short_code"),
    @Index(name = "idx_urls_user_id", columnList = "user_id"),
    @Index(name = "idx_urls_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Short code is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", 
             message = "Short code can only contain letters, numbers, dash, and underscore")
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Original URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String originalUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "click_count", nullable = false)
    @Min(0)
    private Integer clickCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Analytics> analytics = new ArrayList<>();

    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomAlias> customAliases = new ArrayList<>();

    public Url() {
    }

    public Url(Long id, String shortCode, String originalUrl, User user, 
               LocalDateTime createdAt, LocalDateTime expiresAt, 
               Integer clickCount, Boolean isActive, Map<String, Object> metadata) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.isActive = isActive;
        this.metadata = metadata;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Analytics> getAnalytics() {
        return analytics;
    }

    public void setAnalytics(List<Analytics> analytics) {
        this.analytics = analytics;
    }

    public List<CustomAlias> getCustomAliases() {
        return customAliases;
    }

    public void setCustomAliases(List<CustomAlias> customAliases) {
        this.customAliases = customAliases;
    }

    // Business methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementClickCount() {
        this.clickCount = this.clickCount + 1;
    }

    public void addCustomAlias(CustomAlias alias) {
        customAliases.add(alias);
        alias.setUrl(this);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (clickCount == null) {
            clickCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Url)) return false;
        Url url = (Url) o;
        return id != null && id.equals(url.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Url{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", clickCount=" + clickCount +
                ", isActive=" + isActive +
                '}';
    }
}