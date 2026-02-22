package com.chess.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when maximum concurrent games limit is exceeded
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
public class GameLimitExceededException extends ChessApiException {

    public GameLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "GAME_LIMIT_EXCEEDED");
    }

    public GameLimitExceededException(int maxGames) {
        super(
            String.format("Maximum concurrent games limit (%d) exceeded. Please try again later.", maxGames),
            HttpStatus.TOO_MANY_REQUESTS,
            "GAME_LIMIT_EXCEEDED"
        );
    }
}
