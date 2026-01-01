package com.urlshortener.api.repository;

import com.urlshortener.api.entity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Analytics entity operations.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    /**
     * Find all analytics for a specific URL
     */
    @Query("SELECT a FROM Analytics a WHERE a.url.id = :urlId ORDER BY a.clickedAt DESC")
    List<Analytics> findByUrlId(@Param("urlId") Long urlId);

    /**
     * Find analytics for a URL within a date range
     */
    @Query("SELECT a FROM Analytics a WHERE a.url.id = :urlId AND a.clickedAt BETWEEN :startDate AND :endDate ORDER BY a.clickedAt DESC")
    List<Analytics> findByUrlIdAndDateRange(
        @Param("urlId") Long urlId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count total clicks for a URL
     */
    @Query("SELECT COUNT(a) FROM Analytics a WHERE a.url.id = :urlId")
    long countByUrlId(@Param("urlId") Long urlId);

    /**
     * Count unique visitors for a URL (by IP)
     */
    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM Analytics a WHERE a.url.id = :urlId")
    long countUniqueVisitorsByUrlId(@Param("urlId") Long urlId);

    /**
     * Count unique countries for a URL
     */
    @Query("SELECT COUNT(DISTINCT a.country) FROM Analytics a WHERE a.url.id = :urlId AND a.country IS NOT NULL")
    long countUniqueCountriesByUrlId(@Param("urlId") Long urlId);

    /**
     * Get analytics grouped by date for a URL
     */
    @Query("SELECT DATE(a.clickedAt) as date, COUNT(a) as clicks " +
           "FROM Analytics a " +
           "WHERE a.url.id = :urlId AND a.clickedAt > :since " +
           "GROUP BY DATE(a.clickedAt) " +
           "ORDER BY DATE(a.clickedAt) DESC")
    List<Object[]> getClicksByDate(@Param("urlId") Long urlId, @Param("since") LocalDateTime since);

    /**
     * Get top countries for a URL
     */
    @Query("SELECT a.country, COUNT(a) as clicks " +
           "FROM Analytics a " +
           "WHERE a.url.id = :urlId AND a.country IS NOT NULL " +
           "GROUP BY a.country " +
           "ORDER BY clicks DESC")
    List<Object[]> getTopCountries(@Param("urlId") Long urlId);
}