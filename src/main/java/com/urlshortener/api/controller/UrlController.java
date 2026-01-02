package com.urlshortener.api.controller;

import com.urlshortener.api.dto.CustomAliasResponse;
import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenedUrlResponse;
import com.urlshortener.api.dto.UpdateAliasRequest;
import com.urlshortener.api.dto.UrlResponse;
import com.urlshortener.api.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for URL shortening operations.
 * Handles both guest and authenticated user requests.
 */
@RestController
// @RequestMapping("/api")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Shorten a URL
     * - Guests can create without custom code
     * - Authenticated users can add custom codes
     * 
     * POST /api/shorten
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenedUrlResponse> shortenUrl(
        @Valid @RequestBody ShortenUrlRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        ShortenedUrlResponse response;
        
        if (userDetails == null) {
            // Guest user - create without custom code (ignore it if provided)
            response = urlService.createShortUrl(request.getUrl());
        } else {
            // Authenticated user
            Long userId = getUserIdFromUserDetails(userDetails);
            response = urlService.createShortUrl(
                request.getUrl(), 
                request.getCustomCode(), 
                userId
            );
        }
        
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 Created
                .body(response);
    }

    /**
     * Redirect to original URL
     * This is the main redirect endpoint - handles both short codes and custom aliases
     * 
     * GET /{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
        @PathVariable String shortCode,
        HttpServletRequest request
    ) {
        // Get original URL
        String originalUrl = urlService.getOriginalUrl(shortCode);
        
        // TODO: Track analytics asynchronously here
        // analyticsService.trackClick(shortCode, request);
        
        // Return 301 Permanent Redirect
        return ResponseEntity
                .status(HttpStatus.MOVED_PERMANENTLY)  // 301
                .header("Location", originalUrl)
                .build();
    }

    /**
     * Get all URLs for authenticated user
     * 
     * GET /api/users/me/urls
     */
    @GetMapping("/users/me/urls")
    public ResponseEntity<List<UrlResponse>> getUserUrls(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = getUserIdFromUserDetails(userDetails);
        List<UrlResponse> urls = urlService.getUserUrls(userId);
        
        return ResponseEntity.ok(urls);
    }

    /**
     * Add or update custom alias for a URL
     * 
     * PUT /api/urls/{id}/alias
     */
    @PutMapping("/urls/{id}/alias")
    public ResponseEntity<CustomAliasResponse> updateAlias(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAliasRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = getUserIdFromUserDetails(userDetails);
        CustomAliasResponse response = urlService.updateCustomAlias(
            id, 
            request.getCustomCode(), 
            userId
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate (soft delete) a URL
     * 
     * DELETE /api/urls/{id}
     */
    @DeleteMapping("/urls/{id}")
    public ResponseEntity<Void> deactivateUrl(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = getUserIdFromUserDetails(userDetails);
        urlService.deactivateUrl(id, userId);
        
        return ResponseEntity.noContent().build();  // 204 No Content
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Extract user ID from UserDetails
     * TODO: Replace with actual implementation once you create CustomUserDetails
     */
    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        // This is a placeholder - you'll implement CustomUserDetails later
        // For now, you can just return a dummy value for testing
        
        // When you implement authentication, your CustomUserDetails will look like:
        // public class CustomUserDetails implements UserDetails {
        //     private Long userId;
        //     // ... other fields
        // }
        
        // For testing without auth, you can temporarily do:
        return 1L;  // TEMPORARY - replace when you implement auth
    }
}