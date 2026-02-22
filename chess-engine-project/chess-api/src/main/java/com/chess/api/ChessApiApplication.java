package com.chess.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Chess Engine REST API Application
 * 
 * Main entry point for the Spring Boot chess engine backend.
 * Provides REST endpoints for game management, move execution,
 * engine calculations, and position analysis.
 * 
 * Features:
 * - Game state management with automatic cleanup
 * - Chess engine with alpha-beta search
 * - Position evaluation and legal move generation
 * - CORS support for frontend integration
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class ChessApiApplication {

    /**
     * Application entry point
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ChessApiApplication.class, args);
    }
}
