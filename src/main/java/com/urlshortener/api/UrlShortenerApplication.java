package com.urlshortener.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the URL Shortener application.
 * 
 * @EnableCaching: Enables Spring's annotation-driven cache management
 * @EnableJpaAuditing: Enables JPA auditing for @CreatedDate, @LastModifiedDate
 * @EnableAsync: Enables asynchronous method execution (for analytics tracking)
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}