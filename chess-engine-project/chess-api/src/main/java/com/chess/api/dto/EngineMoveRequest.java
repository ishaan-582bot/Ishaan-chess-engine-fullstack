package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Engine Move Request DTO
 * 
 * Request body for calculating the engine's best move.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for engine move calculation")
public class EngineMoveRequest {

    @NotBlank(message = "Game ID is required")
    @Schema(description = "Unique game identifier", example = "game-123456", required = true)
    private String gameId;

    @Min(value = 1, message = "Depth must be at least 1")
    @Max(value = 10, message = "Depth cannot exceed 10")
    @Schema(description = "Search depth (1-10). Higher = stronger but slower", 
            example = "5", defaultValue = "5")
    private int depth = 5;
}
