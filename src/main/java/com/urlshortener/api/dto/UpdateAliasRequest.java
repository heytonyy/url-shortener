package com.urlshortener.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating a custom alias.
 * This is what the client sends to add/update a custom code.
 */
public class UpdateAliasRequest {
    
    @NotBlank(message = "Custom code is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", 
             message = "Custom code must be 3-50 characters and contain only letters, numbers, dash, and underscore")
    private String customCode;

    // Constructors
    public UpdateAliasRequest() {
    }

    public UpdateAliasRequest(String customCode) {
        this.customCode = customCode;
    }

    // Getters and Setters
    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    @Override
    public String toString() {
        return "UpdateAliasRequest{" +
                "customCode='" + customCode + '\'' +
                '}';
    }
}