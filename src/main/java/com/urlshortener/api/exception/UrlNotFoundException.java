package com.urlshortener.api.exception;

/**
 * Exception thrown when a URL is not found.
 * Results in a 404 HTTP response.
 */
public class UrlNotFoundException extends RuntimeException {
    
    public UrlNotFoundException(String shortCode) {
        super("URL not found for short code: " + shortCode);
    }
    
    public UrlNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}