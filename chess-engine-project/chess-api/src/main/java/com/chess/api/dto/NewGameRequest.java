package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * New Game Request DTO
 * 
 * Request body for creating a new chess game.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for creating a new chess game")
public class NewGameRequest {

    @Pattern(regexp = "^(rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1)?$", 
            message = "Invalid FEN string")
    @Schema(description = "Starting position FEN (optional, defaults to standard starting position)",
            example = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    private String fen;

    @Pattern(regexp = "^(human_vs_engine|human_vs_human)$", message = "Invalid game mode")
    @Schema(description = "Game mode", example = "human_vs_engine", defaultValue = "human_vs_engine")
    private String mode = "human_vs_engine";

    @Pattern(regexp = "^(white|black)$", message = "Player color must be 'white' or 'black'")
    @Schema(description = "Player color (when playing against engine)", 
            example = "white", defaultValue = "white")
    private String playerColor = "white";

    @Min(value = 1, message = "Engine depth must be at least 1")
    @Max(value = 10, message = "Engine depth cannot exceed 10")
    @Schema(description = "Engine search depth (1-10)", example = "5", defaultValue = "5")
    private int engineDepth = 5;
}
