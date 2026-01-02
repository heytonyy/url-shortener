package com.urlshortener.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the URL Shortener API.
 * 
 * Allows:
 * - Guest access to create short URLs and redirect
 * - Requires authentication for user management endpoints
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for REST API
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/api/shorten").permitAll()           // Create short URL
                .requestMatchers("/api/actuator/**").permitAll()        // Health checks
                .requestMatchers("/api/users/**").authenticated()       // User endpoints need auth
                .requestMatchers("/api/urls/**").authenticated()        // URL management needs auth
                .anyRequest().permitAll()  // Everything else is public (including redirects)
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}