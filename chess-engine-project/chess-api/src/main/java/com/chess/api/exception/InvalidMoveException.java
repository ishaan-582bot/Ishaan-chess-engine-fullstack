package com.chess.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid move is attempted
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
public class InvalidMoveException extends ChessApiException {

    public InvalidMoveException(String move) {
        super(
            String.format("Invalid move: %s", move),
            HttpStatus.BAD_REQUEST,
            "INVALID_MOVE"
        );
    }

    public InvalidMoveException(String from, String to) {
        super(
            String.format("Invalid move: %s to %s", from, to),
            HttpStatus.BAD_REQUEST,
            "INVALID_MOVE"
        );
    }

    public InvalidMoveException(String from, String to, String reason) {
        super(
            String.format("Invalid move: %s to %s - %s", from, to, reason),
            HttpStatus.BAD_REQUEST,
            "INVALID_MOVE"
        );
    }
}
