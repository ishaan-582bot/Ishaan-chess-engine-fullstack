package com.chess.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid FEN string is provided
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
public class InvalidFenException extends ChessApiException {

    public InvalidFenException(String fen) {
        super(
            String.format("Invalid FEN string: %s", fen),
            HttpStatus.BAD_REQUEST,
            "INVALID_FEN"
        );
    }

    public InvalidFenException(String fen, String reason) {
        super(
            String.format("Invalid FEN string: %s - %s", fen, reason),
            HttpStatus.BAD_REQUEST,
            "INVALID_FEN"
        );
    }
}
