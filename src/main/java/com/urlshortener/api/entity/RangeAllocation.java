package com.urlshortener.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * RangeAllocation entity for distributed short code generation.
 * Each service instance gets a unique range of codes to prevent collisions.
 */
@Entity
@Table(name = "range_allocations", indexes = {
    @Index(name = "idx_range_allocations_service", columnList = "service_instance_id"),
    @Index(name = "idx_range_allocations_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class RangeAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_instance_id", nullable = false, length = 100)
    @NotBlank(message = "Service instance ID is required")
    private String serviceInstanceId;

    @Column(name = "start_range", nullable = false)
    private Long startRange;

    @Column(name = "end_range", nullable = false)
    private Long endRange;

    @Column(name = "allocated_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime allocatedAt;

    @Column(name = "exhausted_at")
    private LocalDateTime exhaustedAt;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RangeStatus status = RangeStatus.ACTIVE;

    public enum RangeStatus {
        ACTIVE,
        EXHAUSTED,
        EXPIRED
    }

    public RangeAllocation() {
    }

    public RangeAllocation(Long id, String serviceInstanceId, Long startRange, Long endRange, 
                           LocalDateTime allocatedAt, LocalDateTime exhaustedAt, RangeStatus status) {
        this.id = id;
        this.serviceInstanceId = serviceInstanceId;
        this.startRange = startRange;
        this.endRange = endRange;
        this.allocatedAt = allocatedAt;
        this.exhaustedAt = exhaustedAt;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public Long getStartRange() {
        return startRange;
    }

    public void setStartRange(Long startRange) {
        this.startRange = startRange;
    }

    public Long getEndRange() {
        return endRange;
    }

    public void setEndRange(Long endRange) {
        this.endRange = endRange;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public LocalDateTime getExhaustedAt() {
        return exhaustedAt;
    }

    public void setExhaustedAt(LocalDateTime exhaustedAt) {
        this.exhaustedAt = exhaustedAt;
    }

    public RangeStatus getStatus() {
        return status;
    }

    public void setStatus(RangeStatus status) {
        this.status = status;
    }

    // Business methods
    public void markExhausted() {
        this.status = RangeStatus.EXHAUSTED;
        this.exhaustedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == RangeStatus.ACTIVE;
    }

    public long getRangeSize() {
        return endRange - startRange + 1;
    }

    @PrePersist
    protected void onCreate() {
        if (allocatedAt == null) {
            allocatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RangeStatus.ACTIVE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RangeAllocation)) return false;
        RangeAllocation that = (RangeAllocation) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RangeAllocation{" +
                "id=" + id +
                ", serviceInstanceId='" + serviceInstanceId + '\'' +
                ", startRange=" + startRange +
                ", endRange=" + endRange +
                ", status=" + status +
                '}';
    }
}