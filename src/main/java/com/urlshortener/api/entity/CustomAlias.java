package com.urlshortener.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * CustomAlias entity representing user-defined custom short codes.
 * Allows users to create memorable aliases like "my-blog" instead of random codes.
 */
@Entity
@Table(name = "custom_aliases", indexes = {
    @Index(name = "idx_custom_aliases_code", columnList = "custom_code"),
    @Index(name = "idx_custom_aliases_url_id", columnList = "url_id")
})
@EntityListeners(AuditingEntityListener.class)
public class CustomAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "custom_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Custom code is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", 
             message = "Custom code must be 3-50 characters and contain only letters, numbers, dash, and underscore")
    private String customCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public CustomAlias() {
    }

    public CustomAlias(Long id, Url url, String customCode, LocalDateTime createdAt, Boolean isActive) {
        this.id = id;
        this.url = url;
        this.customCode = customCode;
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

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomAlias)) return false;
        CustomAlias that = (CustomAlias) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CustomAlias{" +
                "id=" + id +
                ", customCode='" + customCode + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}