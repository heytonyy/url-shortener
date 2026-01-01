package com.urlshortener.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * GlobalCounter entity - Single-row table for managing global range allocation.
 * This table should only ever have one row with id=1.
 */
@Entity
@Table(name = "global_counter")
public class GlobalCounter {

    @Id
    @Column(nullable = false)
    private Integer id = 1;  // Singleton constraint

    @Column(name = "current_counter", nullable = false)
    private Long currentCounter = 0L;

    @Column(name = "range_size", nullable = false)
    private Integer rangeSize = 1000;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    public GlobalCounter() {
    }

    public GlobalCounter(Integer id, Long currentCounter, Integer rangeSize, LocalDateTime lastUpdated) {
        this.id = id;
        this.currentCounter = currentCounter;
        this.rangeSize = rangeSize;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getCurrentCounter() {
        return currentCounter;
    }

    public void setCurrentCounter(Long currentCounter) {
        this.currentCounter = currentCounter;
    }

    public Integer getRangeSize() {
        return rangeSize;
    }

    public void setRangeSize(Integer rangeSize) {
        this.rangeSize = rangeSize;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Business methods
    public Long allocateNewRange() {
        Long rangeStart = this.currentCounter;
        this.currentCounter += this.rangeSize;
        this.lastUpdated = LocalDateTime.now();
        return rangeStart;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "GlobalCounter{" +
                "id=" + id +
                ", currentCounter=" + currentCounter +
                ", rangeSize=" + rangeSize +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}