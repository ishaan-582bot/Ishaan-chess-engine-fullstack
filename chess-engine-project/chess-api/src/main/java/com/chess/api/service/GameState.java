package com.chess.api.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Game State
 * 
 * Represents the state of a chess game including:
 * - Position (FEN)
 * - Game metadata
 * - Activity tracking for expiration
 * - Game result tracking
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Slf4j
public class GameState {

    private String gameId;
    private String fen;
    private String sideToMove;
    private String mode;
    private String playerColor;
    private int engineDepth;
    private int halfmoveClock;
    private int fullmoveNumber;
    private Map<String, Boolean> castlingRights;
    private String enPassantTarget;
    
    private boolean gameOver;
    private String gameResult;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;

    /**
     * Create a new game state
     */
    public GameState() {
        this.castlingRights = new HashMap<>();
        this.castlingRights.put("K", true);
        this.castlingRights.put("Q", true);
        this.castlingRights.put("k", true);
        this.castlingRights.put("q", true);
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Check if it's the engine's turn
     * 
     * @return true if engine should move
     */
    public boolean isEngineTurn() {
        if (gameOver) return false;
        
        // In human vs engine mode, engine plays opposite color
        if ("human_vs_engine".equals(mode)) {
            return !sideToMove.equals(playerColor);
        }
        
        return false;
    }

    /**
     * Update position after a move
     * 
     * @param newFen New FEN string
     */
    public void updatePosition(String newFen) {
        this.fen = newFen;
        this.sideToMove = sideToMove.equals("white") ? "black" : "white";
        this.lastActivity = LocalDateTime.now();
        parseFen(newFen);
    }

    /**
     * End the game
     * 
     * @param result Game result description
     */
    public void endGame(String result) {
        this.gameOver = true;
        this.gameResult = result;
        this.lastActivity = LocalDateTime.now();
        log.info("Game {} ended: {}", gameId, result);
    }

    /**
     * Check if the game has expired
     * 
     * @param timeoutMinutes Timeout in minutes
     * @return true if expired
     */
    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * Parse FEN to extract counters and rights
     */
    private void parseFen(String fen) {
        String[] parts = fen.split(" ");
        if (parts.length >= 5) {
            this.halfmoveClock = Integer.parseInt(parts[4]);
            this.fullmoveNumber = Integer.parseInt(parts[5]);
        }
        
        if (parts.length >= 3) {
            String castling = parts[2];
            this.castlingRights.put("K", castling.contains("K"));
            this.castlingRights.put("Q", castling.contains("Q"));
            this.castlingRights.put("k", castling.contains("k"));
            this.castlingRights.put("q", castling.contains("q"));
        }
        
        if (parts.length >= 4) {
            this.enPassantTarget = "-".equals(parts[3]) ? null : parts[3];
        }
    }
}
