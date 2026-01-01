package com.urlshortener.api.repository;

import com.urlshortener.api.entity.CustomAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CustomAlias entity operations.
 */
@Repository
public interface CustomAliasRepository extends JpaRepository<CustomAlias, Long> {

    /**
     * Find active custom alias by code
     */
    @Query("SELECT ca FROM CustomAlias ca WHERE ca.customCode = :code AND ca.isActive = true")
    Optional<CustomAlias> findByCustomCodeAndActive(@Param("code") String code);

    /**
     * Find custom alias by code (including inactive)
     */
    Optional<CustomAlias> findByCustomCode(String code);

    /**
     * Check if custom code already exists
     */
    boolean existsByCustomCode(String code);

    /**
     * Find all active aliases for a URL
     */
    @Query("SELECT ca FROM CustomAlias ca WHERE ca.url.id = :urlId AND ca.isActive = true")
    List<CustomAlias> findByUrlIdAndActive(@Param("urlId") Long urlId);

    /**
     * Find all aliases for a user's URLs
     */
    @Query("SELECT ca FROM CustomAlias ca WHERE ca.url.user.id = :userId AND ca.isActive = true")
    List<CustomAlias> findByUserId(@Param("userId") Long userId);
}