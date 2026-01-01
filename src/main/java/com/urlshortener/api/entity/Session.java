package com.urlshortener.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Session entity for managing user authentication sessions.
 * Stores JWT tokens with expiration for server-side validation.
 */
@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_sessions_token", columnList = "session_token"),
    @Index(name = "idx_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_sessions_expires_at", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token", nullable = false, unique = true, length = 255)
    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Session() {
    }

    public Session(Long id, User user, String sessionToken, LocalDateTime createdAt, 
                   LocalDateTime expiresAt, String ipAddress, String userAgent, Boolean isActive) {
        this.id = id;
        this.user = user;
        this.sessionToken = sessionToken;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // Business methods
    public boolean isValid() {
        return isActive && LocalDateTime.now().isBefore(expiresAt);
    }

    public void invalidate() {
        this.isActive = false;
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
        if (!(o instanceof Session)) return false;
        Session session = (Session) o;
        return id != null && id.equals(session.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", sessionToken='" + (sessionToken != null ? sessionToken.substring(0, Math.min(10, sessionToken.length())) + "..." : "null") + '\'' +
                ", expiresAt=" + expiresAt +
                ", isActive=" + isActive +
                '}';
    }
}