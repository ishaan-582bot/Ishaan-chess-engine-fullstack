package com.chess.api.controller;

import com.chess.api.dto.EvaluationResponse;
import com.chess.api.dto.LegalMovesResponse;
import com.chess.api.service.ChessEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Position Controller
 * 
 * REST endpoints for position analysis:
 * - Legal moves generation
 * - Position evaluation
 * - FEN validation
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@RestController
@RequestMapping("/position")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Position Analysis", description = "Endpoints for position evaluation and legal moves")
public class PositionController {

    private final ChessEngineService chessEngineService;

    /**
     * Get all legal moves for a position
     * 
     * @param fen FEN string representing the position
     * @return List of legal moves in UCI format
     */
    @GetMapping("/legal-moves")
    @Operation(
            summary = "Get legal moves",
            description = "Returns all legal moves for the given position in UCI format (e.g., 'e2e4')"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Legal moves retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LegalMovesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid FEN string")
    })
    public ResponseEntity<LegalMovesResponse> getLegalMoves(
            @Parameter(description = "FEN string representing the position")
            @RequestParam String fen) {
        
        log.debug("Getting legal moves for FEN: {}", fen);
        
        LegalMovesResponse response = chessEngineService.getLegalMoves(fen);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluate a position
     * 
     * @param fen FEN string representing the position
     * @return Position evaluation score and assessment
     */
    @GetMapping("/eval")
    @Operation(
            summary = "Evaluate position",
            description = "Evaluates the given position and returns a score in centipawns. " +
                    "Positive scores favor white, negative scores favor black."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Position evaluated successfully",
                    content = @Content(schema = @Schema(implementation = EvaluationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid FEN string")
    })
    public ResponseEntity<EvaluationResponse> evaluatePosition(
            @Parameter(description = "FEN string representing the position")
            @RequestParam String fen) {
        
        log.debug("Evaluating position: {}", fen);
        
        EvaluationResponse response = chessEngineService.evaluatePosition(fen);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a FEN string
     * 
     * @param fen FEN string to validate
     * @return Validation result
     */
    @GetMapping("/validate")
    @Operation(
            summary = "Validate FEN string",
            description = "Validates whether the given FEN string represents a legal chess position"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FEN is valid"),
            @ApiResponse(responseCode = "400", description = "FEN is invalid")
    })
    public ResponseEntity<Map<String, Object>> validateFen(
            @Parameter(description = "FEN string to validate")
            @RequestParam String fen) {
        
        log.debug("Validating FEN: {}", fen);
        
        try {
            chessEngineService.getLegalMoves(fen);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "fen", fen
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }
}
