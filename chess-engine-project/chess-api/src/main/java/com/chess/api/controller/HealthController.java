package com.chess.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Controller
 * 
 * Provides health check endpoints for monitoring and load balancing.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@RestController
@RequestMapping("/health")
@Slf4j
@Tag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthController {

    private final LocalDateTime startTime = LocalDateTime.now();

    /**
     * Health check endpoint
     * 
     * @return Health status information
     */
    @GetMapping
    @Operation(
            summary = "Health check",
            description = "Returns the current health status of the API"
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("uptime", calculateUptime());
        health.put("service", "chess-api");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with memory info
     * 
     * @return Detailed health information
     */
    @GetMapping("/detailed")
    @Operation(
            summary = "Detailed health check",
            description = "Returns detailed health information including memory usage"
    )
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("uptime", calculateUptime());
        health.put("service", "chess-api");
        health.put("version", "1.0.0");
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("free", runtime.freeMemory() / 1024 / 1024 + " MB");
        memory.put("total", runtime.totalMemory() / 1024 / 1024 + " MB");
        memory.put("max", runtime.maxMemory() / 1024 / 1024 + " MB");
        health.put("memory", memory);
        
        return ResponseEntity.ok(health);
    }

    /**
     * Calculate uptime since application start
     * 
     * @return Uptime string
     */
    private String calculateUptime() {
        java.time.Duration duration = java.time.Duration.between(
                startTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
