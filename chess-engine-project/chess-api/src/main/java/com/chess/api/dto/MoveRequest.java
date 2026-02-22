package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Move Request DTO
 * 
 * Request body for making a move in a game.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for making a chess move")
public class MoveRequest {

    @NotBlank(message = "Game ID is required")
    @Schema(description = "Unique game identifier", example = "game-123456", required = true)
    private String gameId;

    @NotBlank(message = "Source square is required")
    @Pattern(regexp = "^[a-h][1-8]$", message = "Invalid source square format")
    @Schema(description = "Source square in algebraic notation", example = "e2", required = true)
    private String from;

    @NotBlank(message = "Destination square is required")
    @Pattern(regexp = "^[a-h][1-8]$", message = "Invalid destination square format")
    @Schema(description = "Destination square in algebraic notation", example = "e4", required = true)
    private String to;

    @Pattern(regexp = "^[qrbnQRBN]?$", message = "Invalid promotion piece")
    @Schema(description = "Promotion piece (q=queen, r=rook, b=bishop, n=knight)", example = "q")
    private String promotion;
}
