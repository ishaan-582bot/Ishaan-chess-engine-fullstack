package com.chess.api.service;

import com.chess.api.dto.*;
import com.chess.api.engine.SimpleChessEngine;
import com.chess.api.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Chess Engine Service
 * 
 * Core service for chess game operations:
 * - Game creation and management
 * - Move execution and validation
 * - Engine move calculation
 * - Position evaluation
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChessEngineService {

    private final GameStateManager gameStateManager;

    @Value("${chess.engine.max-depth:10}")
    private int maxEngineDepth;

    @Value("${chess.engine.min-depth:1}")
    private int minEngineDepth;

    // ============================================================================
    // GAME MANAGEMENT
    // ============================================================================

    /**
     * Create a new chess game
     * 
     * @param request Game configuration
     * @return New game response
     */
    public NewGameResponse createNewGame(NewGameRequest request) {
        if (request == null) {
            request = NewGameRequest.builder().build();
        }

        // Clamp engine depth to valid range
        int engineDepth = clamp(request.getEngineDepth(), minEngineDepth, maxEngineDepth);

        GameState gameState = gameStateManager.createGame(
                request.getFen(),
                request.getMode(),
                request.getPlayerColor(),
                engineDepth
        );

        // Get legal moves for initial position
        SimpleChessEngine engine = new SimpleChessEngine();
        engine.loadPosition(gameState.getFen());
        List<String> legalMoves = engine.generateLegalMoves();

        log.info("Created new game: {} with depth {}", gameState.getGameId(), engineDepth);

        return NewGameResponse.builder()
                .gameId(gameState.getGameId())
                .fen(gameState.getFen())
                .sideToMove(gameState.getSideToMove())
                .legalMoves(legalMoves)
                .gameStatus(mapGameStatus(engine))
                .halfmoveClock(gameState.getHalfmoveClock())
                .fullmoveNumber(gameState.getFullmoveNumber())
                .castlingRights(gameState.getCastlingRights())
                .enPassantTarget(gameState.getEnPassantTarget())
                .build();
    }

    /**
     * Get current game state
     * 
     * @param gameId Game ID
     * @return Game state response
     */
    public NewGameResponse getGameState(String gameId) {
        GameState gameState = gameStateManager.getGame(gameId);

        SimpleChessEngine engine = new SimpleChessEngine();
        engine.loadPosition(gameState.getFen());
        List<String> legalMoves = engine.generateLegalMoves();

        return NewGameResponse.builder()
                .gameId(gameState.getGameId())
                .fen(gameState.getFen())
                .sideToMove(gameState.getSideToMove())
                .legalMoves(legalMoves)
                .gameStatus(mapGameStatus(engine))
                .halfmoveClock(gameState.getHalfmoveClock())
                .fullmoveNumber(gameState.getFullmoveNumber())
                .castlingRights(gameState.getCastlingRights())
                .enPassantTarget(gameState.getEnPassantTarget())
                .build();
    }

    // ============================================================================
    // MOVE EXECUTION
    // ============================================================================

    /**
     * Make a move in a game
     * 
     * @param request Move request
     * @return Move response
     */
    public MoveResponse makeMove(MoveRequest request) {
        GameState gameState = gameStateManager.getGame(request.getGameId());

        // Check if game is over
        if (gameState.isGameOver()) {
            throw new GameOverException(request.getGameId(), gameState.getGameResult());
        }

        // Load position in engine
        SimpleChessEngine engine = new SimpleChessEngine();
        engine.loadPosition(gameState.getFen());

        // Validate move is legal
        List<String> legalMoves = engine.generateLegalMoves();
        String uciMove = request.getFrom() + request.getTo() + 
                (request.getPromotion() != null ? request.getPromotion() : "");
        
        boolean isLegal = legalMoves.stream()
                .anyMatch(m -> m.equals(uciMove) || m.startsWith(request.getFrom() + request.getTo()));
        
        if (!isLegal) {
            throw new InvalidMoveException(request.getFrom(), request.getTo(), "Move is not legal");
        }

        // Detect capture before making move
        String capturedPiece = engine.getPieceAt(
                request.getTo().charAt(0) - 'a',
                8 - (request.getTo().charAt(1) - '0')
        );

        // Make the move
        boolean moveMade = engine.makeMove(request.getFrom(), request.getTo(), request.getPromotion());
        if (!moveMade) {
            throw new InvalidMoveException(request.getFrom(), request.getTo(), "Failed to execute move");
        }

        // Update game state
        String newFen = engine.getFen();
        gameState.updatePosition(newFen);

        // Get new legal moves
        List<String> newLegalMoves = engine.generateLegalMoves();

        // Check game status
        boolean inCheck = engine.isInCheck();
        boolean checkmate = engine.isCheckmate();
        boolean stalemate = engine.isStalemate();

        // Update game status if ended
        if (checkmate) {
            String winner = gameState.getSideToMove().equals("white") ? "Black" : "White";
            gameState.endGame(winner + " wins by checkmate");
        } else if (stalemate) {
            gameState.endGame("Draw by stalemate");
        }

        log.info("Move made in game {}: {} -> {}", 
                request.getGameId(), request.getFrom(), request.getTo());

        return MoveResponse.builder()
                .gameId(gameState.getGameId())
                .move(MoveResponse.MoveInfo.builder()
                        .from(request.getFrom())
                        .to(request.getTo())
                        .san(engine.getLastMoveSan())
                        .promotion(request.getPromotion())
                        .build())
                .fen(newFen)
                .sideToMove(gameState.getSideToMove())
                .legalMoves(newLegalMoves)
                .gameStatus(MoveResponse.GameStatus.builder()
                        .inCheck(inCheck)
                        .checkmate(checkmate)
                        .stalemate(stalemate)
                        .draw(stalemate)
                        .description(getStatusDescription(inCheck, checkmate, stalemate))
                        .build())
                .halfmoveClock(gameState.getHalfmoveClock())
                .fullmoveNumber(gameState.getFullmoveNumber())
                .capturedPiece(capturedPiece)
                .check(inCheck)
                .checkmate(checkmate)
                .stalemate(stalemate)
                .build();
    }

    // ============================================================================
    // ENGINE MOVES
    // ============================================================================

    /**
     * Calculate engine's best move
     * 
     * @param request Engine move request
     * @return Engine move response
     */
    public EngineMoveResponse calculateEngineMove(EngineMoveRequest request) {
        GameState gameState = gameStateManager.getGame(request.getGameId());

        // Check if game is over
        if (gameState.isGameOver()) {
            throw new GameOverException(request.getGameId(), gameState.getGameResult());
        }

        // Clamp depth to valid range
        int depth = clamp(request.getDepth(), minEngineDepth, maxEngineDepth);

        // Create engine and load position
        SimpleChessEngine engine = new SimpleChessEngine();
        engine.loadPosition(gameState.getFen());

        // Calculate best move
        long startTime = System.currentTimeMillis();
        SimpleChessEngine.SearchResult result = engine.search(depth);
        long timeMs = System.currentTimeMillis() - startTime;

        if (result.bestMove == null) {
            throw new InvalidMoveException("No legal moves available");
        }

        // Execute the engine's move
        String capturedPiece = engine.getPieceAt(
                result.bestMove.toCol,
                result.bestMove.toRow
        );
        
        engine.makeMove(result.bestMove);
        String newFen = engine.getFen();
        
        // Update game state AFTER engine search
        gameState.updatePosition(newFen);

        // Get new legal moves
        List<String> newLegalMoves = engine.generateLegalMoves();

        // Check game status
        boolean inCheck = engine.isInCheck();
        boolean checkmate = engine.isCheckmate();
        boolean stalemate = engine.isStalemate();

        if (checkmate) {
            String winner = gameState.getSideToMove().equals("white") ? "Black" : "White";
            gameState.endGame(winner + " wins by checkmate");
        } else if (stalemate) {
            gameState.endGame("Draw by stalemate");
        }

        log.info("Engine move calculated for game {}: {} (score: {}, nodes: {}, time: {}ms)",
                request.getGameId(), result.bestMove.toUci(), result.score, result.nodes, timeMs);

        return EngineMoveResponse.builder()
                .bestMove(EngineMoveResponse.MoveInfo.builder()
                        .from(result.bestMove.fromUci().substring(0, 2))
                        .to(result.bestMove.fromUci().substring(2, 4))
                        .san(result.bestMove.san)
                        .promotion(result.bestMove.promotion)
                        .build())
                .position(EngineMoveResponse.PositionInfo.builder()
                        .fen(newFen)
                        .sideToMove(gameState.getSideToMove())
                        .legalMoves(newLegalMoves)
                        .gameStatus(createGameStatusMap(inCheck, checkmate, stalemate))
                        .halfmoveClock(gameState.getHalfmoveClock())
                        .fullmoveNumber(gameState.getFullmoveNumber())
                        .build())
                .evaluation(EngineMoveResponse.EvaluationInfo.builder()
                        .score(result.score)
                        .scoreFromSideToMove(result.score)
                        .assessment(getAssessment(result.score))
                        .material(0)
                        .positional(result.score)
                        .build())
                .searchStats(EngineMoveResponse.SearchStats.builder()
                        .depth(depth)
                        .nodesSearched(result.nodes)
                        .timeMs(timeMs)
                        .nps(timeMs > 0 ? (result.nodes * 1000) / timeMs : 0)
                        .build())
                .pv(result.pv != null ? result.pv : Collections.emptyList())
                .build();
    }

    // ============================================================================
    // POSITION ANALYSIS
    // ============================================================================

    /**
     * Get legal moves for a position
     * 
     * @param fen FEN string
     * @return Legal moves response
     */
    public LegalMovesResponse getLegalMoves(String fen) {
        SimpleChessEngine engine = new SimpleChessEngine();
        
        try {
            engine.loadPosition(fen);
        } catch (Exception e) {
            throw new InvalidFenException(fen, e.getMessage());
        }

        List<String> legalMoves = engine.generateLegalMoves();

        return LegalMovesResponse.builder()
                .fen(fen)
                .sideToMove(engine.getSideToMove() == SimpleChessEngine.WHITE ? "white" : "black")
                .moveCount(legalMoves.size())
                .legalMoves(legalMoves)
                .build();
    }

    /**
     * Evaluate a position
     * 
     * @param fen FEN string
     * @return Evaluation response
     */
    public EvaluationResponse evaluatePosition(String fen) {
        // Create NEW engine instance (doesn't modify game state)
        SimpleChessEngine engine = new SimpleChessEngine();
        
        try {
            engine.loadPosition(fen);
        } catch (Exception e) {
            throw new InvalidFenException(fen, e.getMessage());
        }

        int score = engine.evaluate();
        String assessment = getAssessment(score);

        return EvaluationResponse.builder()
                .fen(fen)
                .score(score)
                .scoreFromSideToMove(score)
                .assessment(assessment)
                .material(0)
                .positional(score)
                .details(EvaluationResponse.EvaluationDetails.builder()
                        .material(0)
                        .pieceSquare(score)
                        .pawnStructure(0)
                        .kingSafety(0)
                        .mobility(0)
                        .build())
                .build();
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Clamp a value to a range
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Map engine status to game status
     */
    private NewGameResponse.GameStatus mapGameStatus(SimpleChessEngine engine) {
        boolean inCheck = engine.isInCheck();
        boolean checkmate = engine.isCheckmate();
        boolean stalemate = engine.isStalemate();

        return NewGameResponse.GameStatus.builder()
                .inCheck(inCheck)
                .checkmate(checkmate)
                .stalemate(stalemate)
                .draw(stalemate)
                .description(getStatusDescription(inCheck, checkmate, stalemate))
                .build();
    }

    /**
     * Get status description
     */
    private String getStatusDescription(boolean inCheck, boolean checkmate, boolean stalemate) {
        if (checkmate) return "Checkmate!";
        if (stalemate) return "Stalemate - Draw!";
        if (inCheck) return "Check!";
        return "Game in progress";
    }

    /**
     * Create game status map
     */
    private Map<String, Object> createGameStatusMap(boolean inCheck, boolean checkmate, boolean stalemate) {
        Map<String, Object> status = new HashMap<>();
        status.put("inCheck", inCheck);
        status.put("checkmate", checkmate);
        status.put("stalemate", stalemate);
        status.put("draw", stalemate);
        status.put("description", getStatusDescription(inCheck, checkmate, stalemate));
        return status;
    }

    /**
     * Get position assessment from score
     */
    private String getAssessment(int score) {
        int absScore = Math.abs(score);
        if (absScore < 30) return "Equal position";
        if (absScore < 70) return score > 0 ? "White is slightly better" : "Black is slightly better";
        if (absScore < 150) return score > 0 ? "White has advantage" : "Black has advantage";
        if (absScore < 300) return score > 0 ? "White has decisive advantage" : "Black has decisive advantage";
        return score > 0 ? "White is winning" : "Black is winning";
    }
}
