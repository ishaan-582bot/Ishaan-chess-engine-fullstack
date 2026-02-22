package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evaluation Response DTO
 * 
 * Response containing position evaluation.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Position evaluation response")
public class EvaluationResponse {

    @Schema(description = "FEN string of evaluated position", 
            example = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
    private String fen;

    @Schema(description = "Score in centipawns (positive = white advantage)", example = "30")
    private int score;

    @Schema(description = "Score from side-to-move perspective", example = "-30")
    private int scoreFromSideToMove;

    @Schema(description = "Position assessment", example = "White is slightly better")
    private String assessment;

    @Schema(description = "Material balance in centipawns", example = "0")
    private int material;

    @Schema(description = "Positional score in centipawns", example = "30")
    private int positional;

    @Schema(description = "Detailed breakdown of evaluation factors")
    private EvaluationDetails details;

    /**
     * Detailed evaluation breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationDetails {
        @Schema(description = "Material score", example = "0")
        private int material;

        @Schema(description = "Piece-square table score", example = "15")
        private int pieceSquare;

        @Schema(description = "Pawn structure score", example = "5")
        private int pawnStructure;

        @Schema(description = "King safety score", example = "10")
        private int kingSafety;

        @Schema(description = "Mobility score", example = "0")
        private int mobility;
    }
}
