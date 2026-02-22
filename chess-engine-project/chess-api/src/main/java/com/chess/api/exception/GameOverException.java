package com.chess.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when trying to make a move in a game that is already over
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
public class GameOverException extends ChessApiException {

    public GameOverException(String gameId) {
        super(
            String.format("Game %s is already over", gameId),
            HttpStatus.CONFLICT,
            "GAME_OVER"
        );
    }

    public GameOverException(String gameId, String result) {
        super(
            String.format("Game %s is already over: %s", gameId, result),
            HttpStatus.CONFLICT,
            "GAME_OVER"
        );
    }
}
