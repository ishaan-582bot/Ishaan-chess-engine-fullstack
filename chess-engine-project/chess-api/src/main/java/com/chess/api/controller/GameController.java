package com.chess.api.controller;

import com.chess.api.dto.MoveRequest;
import com.chess.api.dto.MoveResponse;
import com.chess.api.dto.NewGameRequest;
import com.chess.api.dto.NewGameResponse;
import com.chess.api.service.ChessEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Game Controller
 * 
 * REST endpoints for game management:
 * - Creating new games
 * - Making moves
 * - Retrieving game state
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Game Management", description = "Endpoints for creating and managing chess games")
public class GameController {

    private final ChessEngineService chessEngineService;

    /**
     * Create a new chess game
     * 
     * @param request Game configuration (optional)
     * @return New game response with gameId and initial position
     */
    @PostMapping("/new")
    @Operation(
            summary = "Create a new chess game",
            description = "Creates a new game with optional configuration. Returns game ID and initial position."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Game created successfully",
                    content = @Content(schema = @Schema(implementation = NewGameResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "429", description = "Too many concurrent games")
    })
    public ResponseEntity<NewGameResponse> createNewGame(
            @RequestBody(required = false) NewGameRequest request) {
        
        log.info("Creating new game with request: {}", request);
        
        NewGameResponse response = chessEngineService.createNewGame(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Make a move in the current game
     * 
     * @param request Move request with gameId, from, to squares
     * @return Move response with updated position and game status
     */
    @PostMapping("/move")
    @Operation(
            summary = "Make a move",
            description = "Executes a move in the specified game. Validates the move and updates game state."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Move executed successfully",
                    content = @Content(schema = @Schema(implementation = MoveResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid move or request"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
            @ApiResponse(responseCode = "409", description = "Game is already over")
    })
    public ResponseEntity<MoveResponse> makeMove(
            @Valid @RequestBody MoveRequest request) {
        
        log.info("Making move in game {}: {} -> {}", 
                request.getGameId(), request.getFrom(), request.getTo());
        
        MoveResponse response = chessEngineService.makeMove(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current game state
     * 
     * @param gameId Game ID
     * @return Current game state
     */
    @GetMapping("/state")
    @Operation(
            summary = "Get game state",
            description = "Retrieves the current state of a game including position, legal moves, and status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Game state retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    public ResponseEntity<NewGameResponse> getGameState(
            @Parameter(description = "Game ID") 
            @RequestParam String gameId) {
        
        log.info("Getting game state for: {}", gameId);
        
        NewGameResponse response = chessEngineService.getGameState(gameId);
        
        return ResponseEntity.ok(response);
    }
}
