package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * New Game Response DTO
 * 
 * Response containing the newly created game state.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing new game information")
public class NewGameResponse {

    @Schema(description = "Unique game identifier", example = "game-123456")
    private String gameId;

    @Schema(description = "Current position FEN", 
            example = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    private String fen;

    @Schema(description = "Side to move", example = "white")
    private String sideToMove;

    @Schema(description = "Legal moves for current position", 
            example = "[\"a2a3\", \"a2a4\", \"b2b3\", \"b2b4\", ...]")
    private List<String> legalMoves;

    @Schema(description = "Game status")
    private GameStatus gameStatus;

    @Schema(description = "Halfmove clock (for 50-move rule)", example = "0")
    private int halfmoveClock;

    @Schema(description = "Fullmove number", example = "1")
    private int fullmoveNumber;

    @Schema(description = "Castling rights", example = "{\"K\": true, \"Q\": true, \"k\": true, \"q\": true}")
    private Map<String, Boolean> castlingRights;

    @Schema(description = "En passant target square (if any)", example = "null")
    private String enPassantTarget;

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
