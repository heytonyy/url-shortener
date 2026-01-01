package com.urlshortener.api.repository;

import com.urlshortener.api.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for URL entity operations.
 */
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    /**
     * Find an active URL by its short code
     */
    @Query("SELECT u FROM Url u WHERE u.shortCode = :shortCode AND u.isActive = true")
    Optional<Url> findByShortCodeAndActive(@Param("shortCode") String shortCode);

    /**
     * Find URL by short code (including inactive)
     */
    Optional<Url> findByShortCode(String shortCode);

    /**
     * Check if a short code already exists
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Find all URLs for a specific user
     */
    @Query("SELECT u FROM Url u WHERE u.user.id = :userId AND u.isActive = true ORDER BY u.createdAt DESC")
    List<Url> findByUserIdAndActive(@Param("userId") Long userId);

    /**
     * Find all active URLs for a user with pagination
     */
    @Query("SELECT u FROM Url u WHERE u.user.id = :userId AND u.isActive = true ORDER BY u.createdAt DESC")
    List<Url> findByUserId(@Param("userId") Long userId);

    /**
     * Find expired URLs that are still active
     */
    @Query("SELECT u FROM Url u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    List<Url> findExpiredUrls(@Param("now") LocalDateTime now);

    /**
     * Increment click count for a URL
     */
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    /**
     * Find top N most clicked URLs
     */
    @Query("SELECT u FROM Url u WHERE u.isActive = true ORDER BY u.clickCount DESC")
    List<Url> findTopClickedUrls();

    /**
     * Deactivate expired URLs
     */
    @Modifying
    @Query("UPDATE Url u SET u.isActive = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);

    /**
     * Find all URLs created by guests (user_id is null)
     */
    @Query("SELECT u FROM Url u WHERE u.user IS NULL AND u.isActive = true")
    List<Url> findGuestUrls();

    /**
     * Count URLs by user
     */
    @Query("SELECT COUNT(u) FROM Url u WHERE u.user.id = :userId AND u.isActive = true")
    long countByUserId(@Param("userId") Long userId);
}