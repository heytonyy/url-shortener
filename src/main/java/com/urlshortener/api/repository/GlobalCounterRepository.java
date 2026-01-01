package com.urlshortener.api.repository;

import com.urlshortener.api.entity.GlobalCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for GlobalCounter entity operations.
 * Used for allocating unique ranges for short code generation.
 */
@Repository
public interface GlobalCounterRepository extends JpaRepository<GlobalCounter, Integer> {

    /**
     * Get the global counter with pessimistic write lock.
     * This ensures atomic operations during range allocation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gc FROM GlobalCounter gc WHERE gc.id = 1")
    Optional<GlobalCounter> findByIdWithLock();

    /**
     * Get the global counter without lock (for reading current state)
     */
    @Query("SELECT gc FROM GlobalCounter gc WHERE gc.id = 1")
    Optional<GlobalCounter> findGlobalCounter();
}