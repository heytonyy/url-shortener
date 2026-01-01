package com.urlshortener.api.repository;

import com.urlshortener.api.entity.RangeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RangeAllocation entity operations.
 */
@Repository
public interface RangeAllocationRepository extends JpaRepository<RangeAllocation, Long> {

    /**
     * Find active range for a specific service instance
     */
    @Query("SELECT ra FROM RangeAllocation ra WHERE ra.serviceInstanceId = :instanceId AND ra.status = 'ACTIVE'")
    Optional<RangeAllocation> findActiveRangeByInstanceId(@Param("instanceId") String instanceId);

    /**
     * Find all ranges for a service instance
     */
    List<RangeAllocation> findByServiceInstanceId(String serviceInstanceId);

    /**
     * Find all active ranges
     */
    @Query("SELECT ra FROM RangeAllocation ra WHERE ra.status = 'ACTIVE' ORDER BY ra.allocatedAt DESC")
    List<RangeAllocation> findAllActiveRanges();

    /**
     * Count active ranges
     */
    @Query("SELECT COUNT(ra) FROM RangeAllocation ra WHERE ra.status = 'ACTIVE'")
    long countActiveRanges();
}