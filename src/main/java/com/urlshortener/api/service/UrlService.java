package com.urlshortener.api.service;

import com.urlshortener.api.dto.CustomAliasResponse;
import com.urlshortener.api.dto.ShortenedUrlResponse;
import com.urlshortener.api.dto.UrlResponse;
import com.urlshortener.api.entity.CustomAlias;
import com.urlshortener.api.entity.Url;
import com.urlshortener.api.entity.User;
import com.urlshortener.api.exception.UrlNotFoundException;
import com.urlshortener.api.repository.CustomAliasRepository;
import com.urlshortener.api.repository.UrlRepository;
import com.urlshortener.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final CustomAliasRepository customAliasRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    @Value("${app.url.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlService(UrlRepository urlRepository, 
                      CustomAliasRepository customAliasRepository,
                      UserRepository userRepository,
                      ShortCodeGenerator shortCodeGenerator) {
        this.urlRepository = urlRepository;
        this.customAliasRepository = customAliasRepository;
        this.userRepository = userRepository;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    /**
     * Create short URL for guest (no authentication)
     */
    @Transactional
    public ShortenedUrlResponse createShortUrl(String originalUrl) {
        return createShortUrl(originalUrl, null, null);
    }

    /**
     * Create short URL with optional custom alias for authenticated user
     */
    @Transactional
    public ShortenedUrlResponse createShortUrl(String originalUrl, String customCode, Long userId) {
        // Generate short code
        String shortCode = shortCodeGenerator.generateShortCode();

        // Check if short code already exists (very unlikely, but safety check)
        while (urlRepository.existsByShortCode(shortCode)) {
            shortCode = shortCodeGenerator.generateShortCode();
        }

        // Create URL entity
        Url url = new Url();
        url.setShortCode(shortCode);
        url.setOriginalUrl(originalUrl);
        url.setIsActive(true);
        url.setClickCount(0);

        // Set user if authenticated
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            url.setUser(user);
        }

        // Save URL
        url = urlRepository.save(url);

        // Handle custom code if provided
        String finalCustomCode = null;
        if (customCode != null && !customCode.isEmpty()) {
            // Validate custom code
            if (!shortCodeGenerator.isValidCustomCode(customCode)) {
                throw new RuntimeException("Invalid custom code format");
            }

            // Check if custom code already exists
            if (customAliasRepository.existsByCustomCode(customCode)) {
                throw new RuntimeException("Custom code already taken");
            }

            // Create custom alias
            CustomAlias alias = new CustomAlias();
            alias.setUrl(url);
            alias.setCustomCode(customCode);
            alias.setIsActive(true);
            customAliasRepository.save(alias);

            finalCustomCode = customCode;
        }

        // Build response
        return buildShortenedUrlResponse(url, finalCustomCode);
    }

    /**
     * Get original URL by short code (for redirects)
     */
    public String getOriginalUrl(String shortCode) {
        // Check if it's a custom alias first
        return customAliasRepository.findByCustomCodeAndActive(shortCode)
                .map(alias -> alias.getUrl().getOriginalUrl())
                .orElseGet(() -> 
                    // If not custom alias, check regular short codes
                    urlRepository.findByShortCodeAndActive(shortCode)
                            .map(Url::getOriginalUrl)
                            .orElseThrow(() -> new UrlNotFoundException(shortCode))
                );
    }

    /**
     * Get all URLs for a user
     */
    public List<UrlResponse> getUserUrls(Long userId) {
        List<Url> urls = urlRepository.findByUserId(userId);
        
        return urls.stream()
                .map(this::buildUrlResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update custom alias for a URL
     */
    @Transactional
    public CustomAliasResponse updateCustomAlias(Long urlId, String customCode, Long userId) {
        // Find URL and verify ownership
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        if (url.getUser() == null || !url.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this URL");
        }

        // Validate custom code
        if (!shortCodeGenerator.isValidCustomCode(customCode)) {
            throw new RuntimeException("Invalid custom code format");
        }

        // Check if custom code already exists
        if (customAliasRepository.existsByCustomCode(customCode)) {
            throw new RuntimeException("Custom code already taken");
        }

        // Create new custom alias
        CustomAlias alias = new CustomAlias();
        alias.setUrl(url);
        alias.setCustomCode(customCode);
        alias.setIsActive(true);
        alias = customAliasRepository.save(alias);

        return buildCustomAliasResponse(alias);
    }

    /**
     * Deactivate (soft delete) a URL
     */
    @Transactional
    public void deactivateUrl(Long urlId, Long userId) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        // Verify ownership
        if (url.getUser() == null || !url.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this URL");
        }

        // Soft delete
        url.setIsActive(false);
        urlRepository.save(url);
    }

    // ========================================
    // Helper Methods to Convert Entities to DTOs
    // ========================================

    private ShortenedUrlResponse buildShortenedUrlResponse(Url url, String customCode) {
        ShortenedUrlResponse response = new ShortenedUrlResponse();
        response.setId(url.getId());
        response.setShortCode(url.getShortCode());
        response.setCustomCode(customCode);
        response.setShortUrl(baseUrl + "/" + url.getShortCode());
        response.setOriginalUrl(url.getOriginalUrl());
        response.setCreatedAt(url.getCreatedAt());
        response.setExpiresAt(url.getExpiresAt());
        return response;
    }

    private UrlResponse buildUrlResponse(Url url) {
        UrlResponse response = new UrlResponse();
        response.setId(url.getId());
        response.setShortCode(url.getShortCode());
        response.setOriginalUrl(url.getOriginalUrl());
        response.setCreatedAt(url.getCreatedAt());
        response.setExpiresAt(url.getExpiresAt());
        response.setClickCount(url.getClickCount());
        response.setIsActive(url.getIsActive());

        // Get all custom aliases for this URL
        List<String> aliases = customAliasRepository.findByUrlIdAndActive(url.getId())
                .stream()
                .map(CustomAlias::getCustomCode)
                .collect(Collectors.toList());
        response.setCustomAliases(aliases);

        return response;
    }

    private CustomAliasResponse buildCustomAliasResponse(CustomAlias alias) {
        CustomAliasResponse response = new CustomAliasResponse();
        response.setId(alias.getId());
        response.setUrlId(alias.getUrl().getId());
        response.setCustomCode(alias.getCustomCode());
        response.setCustomUrl(baseUrl + "/" + alias.getCustomCode());
        response.setCreatedAt(alias.getCreatedAt());
        response.setIsActive(alias.getIsActive());
        return response;
    }
}