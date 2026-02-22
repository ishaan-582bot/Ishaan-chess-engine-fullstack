package com.chess.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for Chess API
 * 
 * All custom exceptions extend this class for consistent error handling.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Getter
public class ChessApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ChessApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ChessApiException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
