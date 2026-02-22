package com.chess.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a game is not found
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
public class GameNotFoundException extends ChessApiException {

    public GameNotFoundException(String gameId) {
        super(
            String.format("Game not found: %s", gameId),
            HttpStatus.NOT_FOUND,
            "GAME_NOT_FOUND"
        );
    }

    public GameNotFoundException(String gameId, String reason) {
        super(
            String.format("Game not found: %s - %s", gameId, reason),
            HttpStatus.NOT_FOUND,
            "GAME_NOT_FOUND"
        );
    }
}
