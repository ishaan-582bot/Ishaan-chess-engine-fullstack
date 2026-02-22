package com.chess.api.service;

import com.chess.api.exception.GameLimitExceededException;
import com.chess.api.exception.GameNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game State Manager
 * 
 * Manages all active chess games:
 * - Game creation with ID generation
 * - Game retrieval and updates
 * - Automatic cleanup of expired games
 * - Thread-safe concurrent access
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Component
@Slf4j
public class GameStateManager {

    // Thread-safe map for game storage
    private final ConcurrentHashMap<String, GameState> games = new ConcurrentHashMap<>();

    @Value("${chess.game.timeout-minutes:30}")
    private int gameTimeoutMinutes;

    @Value("${chess.game.max-concurrent:1000}")
    private int maxConcurrentGames;

    /**
     * Create a new game
     * 
     * @param fen Starting FEN (null for standard position)
     * @param mode Game mode
     * @param playerColor Player color
     * @param engineDepth Engine search depth
     * @return New game state
     */
    public GameState createGame(String fen, String mode, String playerColor, int engineDepth) {
        // Check max concurrent games limit
        if (games.size() >= maxConcurrentGames) {
            throw new GameLimitExceededException(maxConcurrentGames);
        }

        GameState gameState = new GameState();
        gameState.setGameId(generateGameId());
        gameState.setFen(fen != null ? fen : "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        gameState.setSideToMove("white");
        gameState.setMode(mode != null ? mode : "human_vs_engine");
        gameState.setPlayerColor(playerColor != null ? playerColor : "white");
        gameState.setEngineDepth(engineDepth);
        gameState.setCreatedAt(LocalDateTime.now());
        gameState.setLastActivity(LocalDateTime.now());

        games.put(gameState.getGameId(), gameState);

        log.info("Created game {}. Total games: {}", gameState.getGameId(), games.size());

        return gameState;
    }

    /**
     * Get a game by ID
     * 
     * @param gameId Game ID
     * @return Game state
     * @throws GameNotFoundException if game not found or expired
     */
    public GameState getGame(String gameId) {
        GameState game = games.get(gameId);
        
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }

        // Check if game has expired
        if (game.isExpired(gameTimeoutMinutes)) {
            games.remove(gameId);
            throw new GameNotFoundException(gameId, "Game has expired due to inactivity");
        }

        // Update activity
        game.setLastActivity(LocalDateTime.now());

        return game;
    }

    /**
     * Remove a game
     * 
     * @param gameId Game ID
     */
    public void removeGame(String gameId) {
        games.remove(gameId);
        log.info("Removed game {}. Total games: {}", gameId, games.size());
    }

    /**
     * Get total number of active games
     * 
     * @return Game count
     */
    public int getGameCount() {
        return games.size();
    }

    /**
     * Generate a unique game ID
     * 
     * @return Game ID
     */
    private String generateGameId() {
        return "game-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Scheduled cleanup of expired games
     * Runs every 5 minutes (300000ms)
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredGames() {
        int removed = 0;
        
        for (String gameId : games.keySet()) {
            GameState game = games.get(gameId);
            if (game != null && game.isExpired(gameTimeoutMinutes)) {
                games.remove(gameId);
                removed++;
            }
        }

        if (removed > 0) {
            log.info("Cleaned up {} expired games. Total games: {}", removed, games.size());
        }
    }
}
