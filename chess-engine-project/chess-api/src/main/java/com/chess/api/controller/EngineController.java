package com.chess.api.controller;

import com.chess.api.dto.EngineMoveRequest;
import com.chess.api.dto.EngineMoveResponse;
import com.chess.api.service.ChessEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Engine Controller
 * 
 * REST endpoints for chess engine operations:
 * - Calculating best moves
 * - Search statistics
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@RestController
@RequestMapping("/engine")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chess Engine", description = "Endpoints for engine move calculation and analysis")
public class EngineController {

    private final ChessEngineService chessEngineService;

    /**
     * Get the engine's best move for the current position
     * 
     * @param request Engine move request with gameId and optional depth
     * @return Engine move response with best move and evaluation
     */
    @PostMapping("/move")
    @Operation(
            summary = "Get engine's best move",
            description = "Calculates the engine's best move using alpha-beta search. " +
                    "Updates the game state with the engine's move."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Engine move calculated successfully",
                    content = @Content(schema = @Schema(implementation = EngineMoveResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
            @ApiResponse(responseCode = "409", description = "Game is already over")
    })
    public ResponseEntity<EngineMoveResponse> getEngineMove(
            @Valid @RequestBody EngineMoveRequest request) {
        
        log.info("Getting engine move for game {} with depth {}", 
                request.getGameId(), request.getDepth());
        
        EngineMoveResponse response = chessEngineService.calculateEngineMove(request);
        
        return ResponseEntity.ok(response);
    }
}
