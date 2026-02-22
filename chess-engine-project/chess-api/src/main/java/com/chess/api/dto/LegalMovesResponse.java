package com.chess.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Legal Moves Response DTO
 * 
 * Response containing all legal moves for a position.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Legal moves response")
public class LegalMovesResponse {

    @Schema(description = "FEN string of the position", 
            example = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    private String fen;

    @Schema(description = "Side to move", example = "white")
    private String sideToMove;

    @Schema(description = "Number of legal moves", example = "20")
    private int moveCount;

    @Schema(description = "Legal moves in UCI format", 
            example = "[\"a2a3\", \"a2a4\", \"b2b3\", \"b2b4\", \"c2c3\", \"c2c4\", ...]")
    private List<String> legalMoves;
}
