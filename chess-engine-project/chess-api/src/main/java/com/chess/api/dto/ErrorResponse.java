package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Error Response DTO
 * 
 * Standard error response format for all API errors.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error code for programmatic handling", example = "INVALID_MOVE")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "The move e2e5 is not legal")
    private String message;

    @Schema(description = "Detailed error description", example = "The piece at e2 cannot move to e5")
    private String details;

    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;

    @Schema(description = "API path that caused the error", example = "/api/game/move")
    private String path;

    /**
     * Create an error response
     * 
     * @param status HTTP status code
     * @param errorCode Error code
     * @param message Error message
     * @return Error response
     */
    public static ErrorResponse of(int status, String errorCode, String message) {
        return ErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with details
     * 
     * @param status HTTP status code
     * @param errorCode Error code
     * @param message Error message
     * @param details Detailed description
     * @param path API path
     * @return Error response
     */
    public static ErrorResponse of(int status, String errorCode, String message, 
                                    String details, String path) {
        return ErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
