package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Engine Move Response DTO
 * 
 * Response containing the engine's best move and analysis.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing the engine's best move and evaluation")
public class EngineMoveResponse {

    @Schema(description = "Best move in UCI format", example = "e2e4")
    private MoveInfo bestMove;

    @Schema(description = "Current position after engine move")
    private PositionInfo position;

    @Schema(description = "Position evaluation")
    private EvaluationInfo evaluation;

    @Schema(description = "Search statistics")
    private SearchStats searchStats;

    @Schema(description = "Principal variation (best line of play)")
    private List<String> pv;

    /**
     * Move information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoveInfo {
        @Schema(description = "Source square", example = "e2")
        private String from;

        @Schema(description = "Destination square", example = "e4")
        private String to;

        @Schema(description = "SAN notation", example = "e4")
        private String san;

        @Schema(description = "Promotion piece (if applicable)", example = "q")
        private String promotion;
    }

    /**
     * Position information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionInfo {
        @Schema(description = "FEN string", example = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        private String fen;

        @Schema(description = "Side to move", example = "black")
        private String sideToMove;

        @Schema(description = "Legal moves", example = "[\"e7e5\", \"e7e6\", \"d7d5\", ...]")
        private List<String> legalMoves;

        @Schema(description = "Game status")
        private Map<String, Object> gameStatus;

        @Schema(description = "Halfmove clock", example = "0")
        private int halfmoveClock;

        @Schema(description = "Fullmove number", example = "1")
        private int fullmoveNumber;
    }

    /**
     * Evaluation information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationInfo {
        @Schema(description = "Score in centipawns (positive = white advantage)", example = "30")
        private int score;

        @Schema(description = "Score from side-to-move perspective", example = "-30")
        private int scoreFromSideToMove;

        @Schema(description = "Position assessment", example = "White is slightly better")
        private String assessment;

        @Schema(description = "Material balance", example = "0")
        private int material;

        @Schema(description = "Positional score", example = "30")
        private int positional;
    }

    /**
     * Search statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchStats {
        @Schema(description = "Search depth reached", example = "5")
        private int depth;

        @Schema(description = "Nodes searched", example = "125000")
        private long nodesSearched;

        @Schema(description = "Search time in milliseconds", example = "250")
        private long timeMs;

        @Schema(description = "Nodes per second", example = "500000")
        private long nps;
    }
}
