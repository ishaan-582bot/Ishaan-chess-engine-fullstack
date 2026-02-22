package com.chess.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for Chess API
 * 
 * Configures Cross-Origin Resource Sharing to allow the frontend
 * application to communicate with the backend API.
 * 
 * CORS settings are loaded from application.properties for easy
 * configuration across different environments.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${chess.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:5500}")
    private String allowedOrigins;

    @Value("${chess.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${chess.cors.allowed-headers:Content-Type,Authorization,X-Requested-With,Accept,Origin}")
    private String allowedHeaders;

    @Value("${chess.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${chess.cors.max-age:3600}")
    private long maxAge;

    /**
     * Configure CORS mappings for all API endpoints
     * @param registry CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.split(","))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }
}
