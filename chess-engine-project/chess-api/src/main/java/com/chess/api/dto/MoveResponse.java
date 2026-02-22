package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Move Response DTO
 * 
 * Response containing the result of a move.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing the result of a chess move")
public class MoveResponse {

    @Schema(description = "Unique game identifier", example = "game-123456")
    private String gameId;

    @Schema(description = "Move details")
    private MoveInfo move;

    @Schema(description = "New position FEN", 
            example = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
    private String fen;

    @Schema(description = "Side to move after the move", example = "black")
    private String sideToMove;

    @Schema(description = "Legal moves for the new position", 
            example = "[\"e7e5\", \"e7e6\", \"d7d5\", ...]")
    private List<String> legalMoves;

    @Schema(description = "Game status")
    private GameStatus gameStatus;

    @Schema(description = "Halfmove clock (for 50-move rule)", example = "0")
    private int halfmoveClock;

    @Schema(description = "Fullmove number", example = "1")
    private int fullmoveNumber;

    @Schema(description = "Captured piece (if any)", example = "p")
    private String capturedPiece;

    @Schema(description = "Whether the move was a check", example = "false")
    private boolean check;

    @Schema(description = "Whether the move was a checkmate", example = "false")
    private boolean checkmate;

    @Schema(description = "Whether the move resulted in stalemate", example = "false")
    private boolean stalemate;

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
     * Game status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameStatus {
        @Schema(description = "Whether the king is in check", example = "false")
        private boolean inCheck;

        @Schema(description = "Whether the game is checkmate", example = "false")
        private boolean checkmate;

        @Schema(description = "Whether the game is stalemate", example = "false")
        private boolean stalemate;

        @Schema(description = "Whether the game is a draw", example = "false")
        private boolean draw;

        @Schema(description = "Status description", example = "Game in progress")
        private String description;
    }
}
