package com.chess.api.engine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Simple Chess Engine
 * 
 * A complete chess engine implementation with:
 * - FEN parsing and generation
 * - Legal move generation
 * - Alpha-beta search with transposition table
 * - Position evaluation with piece-square tables
 * - Make/unmake move support
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Slf4j
public class SimpleChessEngine {

    // Piece constants
    public static final int EMPTY = 0;
    public static final int PAWN = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK = 4;
    public static final int QUEEN = 5;
    public static final int KING = 6;

    // Color constants
    public static final int WHITE = 1;
    public static final int BLACK = -1;

    // Piece values (centipawns)
    private static final int[] PIECE_VALUES = { 0, 100, 320, 330, 500, 900, 20000 };

    // Piece-square tables (from white's perspective)
    private static final int[][] PAWN_TABLE = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        { 5,  5, 10, 25, 25, 10,  5,  5},
        { 0,  0,  0, 20, 20,  0,  0,  0},
        { 5, -5,-10,  0,  0,-10, -5,  5},
        { 5, 10, 10,-20,-20, 10, 10,  5},
        { 0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_TABLE = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_TABLE = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_TABLE = {
        {  0,  0,  0,  0,  0,  0,  0,  0},
        {  5, 10, 10, 10, 10, 10, 10,  5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        {  0,  0,  0,  5,  5,  0,  0,  0}
    };

    private static final int[][] QUEEN_TABLE = {
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    private static final int[][] KING_MIDDLE_TABLE = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    private static final int[][] KING_ENDGAME_TABLE = {
        {-50,-40,-30,-20,-20,-30,-40,-50},
        {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30},
        {-50,-30,-30,-30,-30,-30,-30,-50}
    };

    // Board representation
    private int[][] board = new int[8][8];
    private int sideToMove = WHITE;
    private int halfmoveClock = 0;
    private int fullmoveNumber = 1;
    
    // Castling rights
    private boolean whiteKingSideCastle = true;
    private boolean whiteQueenSideCastle = true;
    private boolean blackKingSideCastle = true;
    private boolean blackQueenSideCastle = true;
    
    // En passant
    private int enPassantCol = -1;
    
    // Move history
    private String lastMoveSan = "";

    // Transposition table
    private Map<Long, TranspositionEntry> transpositionTable = new HashMap<>();
    private long zobristKey = 0;
    private long[][][] zobristTable = new long[8][8][13];
    private long zobristSide;
    private long[] zobristCastling = new long[4];
    private long[] zobristEnPassant = new long[8];

    // Move generation
    private static final int[] KNIGHT_OFFSETS = {-17, -15, -10, -6, 6, 10, 15, 17};
    private static final int[] KING_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};

    /**
     * Initialize Zobrist hashing
     */
    public SimpleChessEngine() {
        Random random = new Random(123456789L);
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                for (int piece = 0; piece < 13; piece++) {
                    zobristTable[row][col][piece] = random.nextLong();
                }
            }
        }
        
        zobristSide = random.nextLong();
        for (int i = 0; i < 4; i++) {
            zobristCastling[i] = random.nextLong();
        }
        for (int i = 0; i < 8; i++) {
            zobristEnPassant[i] = random.nextLong();
        }
    }

    // ============================================================================
    // FEN HANDLING
    // ============================================================================

    /**
     * Load position from FEN string
     * @param fen FEN string
     */
    public void loadPosition(String fen) {
        String[] parts = fen.split(" ");
        
        // Parse board
        String[] ranks = parts[0].split("/");
        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char c : ranks[row].toCharArray()) {
                if (Character.isDigit(c)) {
                    col += c - '0';
                } else {
                    board[row][col] = charToPiece(c);
                    col++;
                }
            }
        }
        
        // Parse side to move
        sideToMove = parts[1].equals("w") ? WHITE : BLACK;
        
        // Parse castling rights
        whiteKingSideCastle = parts[2].contains("K");
        whiteQueenSideCastle = parts[2].contains("Q");
        blackKingSideCastle = parts[2].contains("k");
        blackQueenSideCastle = parts[2].contains("q");
        
        // Parse en passant
        enPassantCol = -1;
        if (!parts[3].equals("-")) {
            enPassantCol = parts[3].charAt(0) - 'a';
        }
        
        // Parse counters
        halfmoveClock = Integer.parseInt(parts[4]);
        fullmoveNumber = Integer.parseInt(parts[5]);
        
        // Compute Zobrist key
        computeZobristKey();
    }

    /**
     * Get current position as FEN
     * @return FEN string
     */
    public String getFen() {
        StringBuilder fen = new StringBuilder();
        
        // Board
        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                if (board[row][col] == EMPTY) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(pieceToChar(board[row][col]));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 7) fen.append("/");
        }
        
        // Side to move
        fen.append(" ").append(sideToMove == WHITE ? "w" : "b");
        
        // Castling rights
        StringBuilder castling = new StringBuilder();
        if (whiteKingSideCastle) castling.append("K");
        if (whiteQueenSideCastle) castling.append("Q");
        if (blackKingSideCastle) castling.append("k");
        if (blackQueenSideCastle) castling.append("q");
        if (castling.length() == 0) castling.append("-");
        fen.append(" ").append(castling);
        
        // En passant
        if (enPassantCol >= 0) {
            int row = sideToMove == WHITE ? 5 : 2;
            fen.append(" ").append((char)('a' + enPassantCol)).append(row + 1);
        } else {
            fen.append(" -");
        }
        
        // Counters
        fen.append(" ").append(halfmoveClock);
        fen.append(" ").append(fullmoveNumber);
        
        return fen.toString();
    }

    private int charToPiece(char c) {
        int color = Character.isUpperCase(c) ? WHITE : BLACK;
        int piece;
        switch (Character.toLowerCase(c)) {
            case 'p': piece = PAWN; break;
            case 'n': piece = KNIGHT; break;
            case 'b': piece = BISHOP; break;
            case 'r': piece = ROOK; break;
            case 'q': piece = QUEEN; break;
            case 'k': piece = KING; break;
            default: piece = EMPTY;
        }
        return piece * color;
    }

    private char pieceToChar(int piece) {
        if (piece == EMPTY) return '1';
        int type = Math.abs(piece);
        int color = piece > 0 ? WHITE : BLACK;
        char c;
        switch (type) {
            case PAWN: c = 'p'; break;
            case KNIGHT: c = 'n'; break;
            case BISHOP: c = 'b'; break;
            case ROOK: c = 'r'; break;
            case QUEEN: c = 'q'; break;
            case KING: c = 'k'; break;
            default: c = '?';
        }
        return color == WHITE ? Character.toUpperCase(c) : c;
    }

    // ============================================================================
    // MOVE GENERATION
    // ============================================================================

    /**
     * Generate all legal moves
     * @return List of legal moves in UCI format
     */
    public List<String> generateLegalMoves() {
        List<Move> pseudoMoves = generatePseudoLegalMoves();
        List<String> legalMoves = new ArrayList<>();
        
        for (Move move : pseudoMoves) {
            if (isLegalMove(move)) {
                legalMoves.add(move.toUci());
            }
        }
        
        return legalMoves;
    }

    /**
     * Generate pseudo-legal moves (may leave king in check)
     */
    private List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece != EMPTY && (piece > 0) == (sideToMove == WHITE)) {
                    generateMovesForPiece(row, col, piece, moves);
                }
            }
        }
        
        return moves;
    }

    private void generateMovesForPiece(int row, int col, int piece, List<Move> moves) {
        int type = Math.abs(piece);
        
        switch (type) {
            case PAWN:
                generatePawnMoves(row, col, piece, moves);
                break;
            case KNIGHT:
                generateKnightMoves(row, col, piece, moves);
                break;
            case BISHOP:
                generateBishopMoves(row, col, piece, moves);
                break;
            case ROOK:
                generateRookMoves(row, col, piece, moves);
                break;
            case QUEEN:
                generateQueenMoves(row, col, piece, moves);
                break;
            case KING:
                generateKingMoves(row, col, piece, moves);
                break;
        }
    }

    private void generatePawnMoves(int row, int col, int piece, List<Move> moves) {
        int direction = piece > 0 ? -1 : 1;
        int startRow = piece > 0 ? 6 : 1;
        int promotionRow = piece > 0 ? 0 : 7;
        
        // Single push
        int newRow = row + direction;
        if (isValidSquare(newRow, col) && board[newRow][col] == EMPTY) {
            if (newRow == promotionRow) {
                for (char promo : new char[]{'q', 'r', 'b', 'n'}) {
                    moves.add(new Move(row, col, newRow, col, piece, EMPTY, promo));
                }
            } else {
                moves.add(new Move(row, col, newRow, col, piece, EMPTY, '\0'));
            }
            
            // Double push from start
            if (row == startRow) {
                newRow = row + 2 * direction;
                if (board[newRow][col] == EMPTY) {
                    moves.add(new Move(row, col, newRow, col, piece, EMPTY, '\0'));
                }
            }
        }
        
        // Captures
        for (int dc : new int[]{-1, 1}) {
            int newCol = col + dc;
            if (isValidSquare(newRow, newCol)) {
                int target = board[newRow][newCol];
                if (target != EMPTY && (target > 0) != (piece > 0)) {
                    if (newRow == promotionRow) {
                        for (char promo : new char[]{'q', 'r', 'b', 'n'}) {
                            moves.add(new Move(row, col, newRow, newCol, piece, target, promo));
                        }
                    } else {
                        moves.add(new Move(row, col, newRow, newCol, piece, target, '\0'));
                    }
                }
                
                // En passant
                if (newCol == enPassantCol && 
                    ((piece > 0 && row == 3) || (piece < 0 && row == 4))) {
                    moves.add(new Move(row, col, newRow, newCol, piece, 
                            (piece > 0 ? -PAWN : PAWN), '\0', true));
                }
            }
        }
    }

    private void generateKnightMoves(int row, int col, int piece, List<Move> moves) {
        int[][] offsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        
        for (int[] offset : offsets) {
            int newRow = row + offset[0];
            int newCol = col + offset[1];
            
            if (isValidSquare(newRow, newCol)) {
                int target = board[newRow][newCol];
                if (target == EMPTY || (target > 0) != (piece > 0)) {
                    moves.add(new Move(row, col, newRow, newCol, piece, target, '\0'));
                }
            }
        }
    }

    private void generateBishopMoves(int row, int col, int piece, List<Move> moves) {
        generateSlidingMoves(row, col, piece, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}, moves);
    }

    private void generateRookMoves(int row, int col, int piece, List<Move> moves) {
        generateSlidingMoves(row, col, piece, new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}, moves);
    }

    private void generateQueenMoves(int row, int col, int piece, List<Move> moves) {
        generateSlidingMoves(row, col, piece, new int[][]{
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
        }, moves);
    }

    private void generateSlidingMoves(int row, int col, int piece, int[][] directions, List<Move> moves) {
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            while (isValidSquare(newRow, newCol)) {
                int target = board[newRow][newCol];
                if (target == EMPTY) {
                    moves.add(new Move(row, col, newRow, newCol, piece, EMPTY, '\0'));
                } else {
                    if ((target > 0) != (piece > 0)) {
                        moves.add(new Move(row, col, newRow, newCol, piece, target, '\0'));
                    }
                    break;
                }
                newRow += dir[0];
                newCol += dir[1];
            }
        }
    }

    private void generateKingMoves(int row, int col, int piece, List<Move> moves) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                
                int newRow = row + dr;
                int newCol = col + dc;
                
                if (isValidSquare(newRow, newCol)) {
                    int target = board[newRow][newCol];
                    if (target == EMPTY || (target > 0) != (piece > 0)) {
                        moves.add(new Move(row, col, newRow, newCol, piece, target, '\0'));
                    }
                }
            }
        }
        
        // Castling
        if (piece > 0) {
            // White castling
            if (whiteKingSideCastle && canCastle(7, 4, 7, 7, WHITE)) {
                moves.add(new Move(row, col, 7, 6, piece, EMPTY, '\0', false, true));
            }
            if (whiteQueenSideCastle && canCastle(7, 4, 7, 0, WHITE)) {
                moves.add(new Move(row, col, 7, 2, piece, EMPTY, '\0', false, true));
            }
        } else {
            // Black castling
            if (blackKingSideCastle && canCastle(0, 4, 0, 7, BLACK)) {
                moves.add(new Move(row, col, 0, 6, piece, EMPTY, '\0', false, true));
            }
            if (blackQueenSideCastle && canCastle(0, 4, 0, 0, BLACK)) {
                moves.add(new Move(row, col, 0, 2, piece, EMPTY, '\0', false, true));
            }
        }
    }

    private boolean canCastle(int kingRow, int kingCol, int rookRow, int rookCol, int color) {
        // Check squares between king and rook are empty
        int step = rookCol > kingCol ? 1 : -1;
        for (int col = kingCol + step; col != rookCol; col += step) {
            if (board[kingRow][col] != EMPTY) return false;
        }
        
        // Check king is not in check
        if (isSquareAttacked(kingRow, kingCol, -color)) return false;
        
        // Check king doesn't pass through check
        if (isSquareAttacked(kingRow, kingCol + step, -color)) return false;
        
        return true;
    }

    private boolean isValidSquare(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // ============================================================================
    // MOVE EXECUTION
    // ============================================================================

    /**
     * Make a move from UCI notation
     * @param from Source square (e.g., "e2")
     * @param to Destination square (e.g., "e4")
     * @param promotion Promotion piece (optional)
     * @return true if move was made
     */
    public boolean makeMove(String from, String to, String promotion) {
        int fromCol = from.charAt(0) - 'a';
        int fromRow = 8 - (from.charAt(1) - '0');
        int toCol = to.charAt(0) - 'a';
        int toRow = 8 - (to.charAt(1) - '0');
        
        char promo = promotion != null && !promotion.isEmpty() ? 
                promotion.charAt(0) : '\0';
        
        Move move = new Move(fromRow, fromCol, toRow, toCol, 
                board[fromRow][fromCol], board[toRow][toCol], promo);
        
        return makeMove(move);
    }

    /**
     * Make a move
     * @param move Move to make
     * @return true if successful
     */
    public boolean makeMove(Move move) {
        // Update board
        board[move.toRow][move.toCol] = move.promotion != '\0' ? 
                (move.piece > 0 ? charToPiece(Character.toUpperCase(move.promotion)) : 
                        charToPiece(move.promotion)) : move.piece;
        board[move.fromRow][move.fromCol] = EMPTY;
        
        // Handle castling rook move
        if (move.isCastling) {
            if (move.toCol == 6) { // King side
                board[move.toRow][5] = board[move.toRow][7];
                board[move.toRow][7] = EMPTY;
            } else { // Queen side
                board[move.toRow][3] = board[move.toRow][0];
                board[move.toRow][0] = EMPTY;
            }
        }
        
        // Handle en passant capture
        if (move.isEnPassant) {
            int captureRow = move.piece > 0 ? move.toRow + 1 : move.toRow - 1;
            board[captureRow][move.toCol] = EMPTY;
        }
        
        // Update castling rights
        if (Math.abs(move.piece) == KING) {
            if (move.piece > 0) {
                whiteKingSideCastle = false;
                whiteQueenSideCastle = false;
            } else {
                blackKingSideCastle = false;
                blackQueenSideCastle = false;
            }
        }
        if (Math.abs(move.piece) == ROOK) {
            if (move.fromRow == 7 && move.fromCol == 7) whiteKingSideCastle = false;
            if (move.fromRow == 7 && move.fromCol == 0) whiteQueenSideCastle = false;
            if (move.fromRow == 0 && move.fromCol == 7) blackKingSideCastle = false;
            if (move.fromRow == 0 && move.fromCol == 0) blackQueenSideCastle = false;
        }
        
        // Update en passant
        if (Math.abs(move.piece) == PAWN && Math.abs(move.toRow - move.fromRow) == 2) {
            enPassantCol = move.fromCol;
        } else {
            enPassantCol = -1;
        }
        
        // Update halfmove clock
        if (Math.abs(move.piece) == PAWN || move.captured != EMPTY) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }
        
        // Update fullmove number
        if (sideToMove == BLACK) {
            fullmoveNumber++;
        }
        
        // Switch side
        sideToMove = -sideToMove;
        
        // Generate SAN
        lastMoveSan = generateSan(move);
        
        // Update Zobrist key
        computeZobristKey();
        
        return true;
    }

    private String generateSan(Move move) {
        StringBuilder san = new StringBuilder();
        
        int pieceType = Math.abs(move.piece);
        
        if (move.isCastling) {
            return move.toCol == 6 ? "O-O" : "O-O-O";
        }
        
        // Piece letter (except pawn)
        if (pieceType != PAWN) {
            san.append(" PNBRQK".charAt(pieceType));
        }
        
        // Disambiguation (simplified)
        // TODO: Add proper disambiguation
        
        // Capture
        if (move.captured != EMPTY || move.isEnPassant) {
            if (pieceType == PAWN) {
                san.append((char)('a' + move.fromCol));
            }
            san.append('x');
        }
        
        // Destination
        san.append((char)('a' + move.toCol));
        san.append(8 - move.toRow);
        
        // Promotion
        if (move.promotion != '\0') {
            san.append('=').append(Character.toUpperCase(move.promotion));
        }
        
        return san.toString();
    }

    // ============================================================================
    // LEGALITY CHECKING
    // ============================================================================

    /**
     * Check if a move is legal
     */
    private boolean isLegalMove(Move move) {
        // Make move temporarily
        int savedFrom = board[move.fromRow][move.fromCol];
        int savedTo = board[move.toRow][move.toCol];
        
        board[move.toRow][move.toCol] = move.promotion != '\0' ?
                (move.piece > 0 ? charToPiece(Character.toUpperCase(move.promotion)) :
                        charToPiece(move.promotion)) : move.piece;
        board[move.fromRow][move.fromCol] = EMPTY;
        
        // Handle en passant capture
        int savedEp = EMPTY;
        if (move.isEnPassant) {
            int captureRow = move.piece > 0 ? move.toRow + 1 : move.toRow - 1;
            savedEp = board[captureRow][move.toCol];
            board[captureRow][move.toCol] = EMPTY;
        }
        
        // Find king
        int kingRow = -1, kingCol = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == KING * move.piece) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
        }
        
        // Check if king is attacked
        boolean legal = !isSquareAttacked(kingRow, kingCol, -move.piece);
        
        // Restore board
        board[move.fromRow][move.fromCol] = savedFrom;
        board[move.toRow][move.toCol] = savedTo;
        
        if (move.isEnPassant) {
            int captureRow = move.piece > 0 ? move.toRow + 1 : move.toRow - 1;
            board[captureRow][move.toCol] = savedEp;
        }
        
        return legal;
    }

    /**
     * Check if a square is attacked by a color
     */
    private boolean isSquareAttacked(int row, int col, int byColor) {
        // Pawn attacks
        int pawnDir = byColor == WHITE ? -1 : 1;
        for (int dc : new int[]{-1, 1}) {
            int r = row - pawnDir;
            int c = col + dc;
            if (isValidSquare(r, c) && board[r][c] == byColor * PAWN) {
                return true;
            }
        }
        
        // Knight attacks
        int[][] knightOffsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        for (int[] offset : knightOffsets) {
            int r = row + offset[0];
            int c = col + offset[1];
            if (isValidSquare(r, c) && board[r][c] == byColor * KNIGHT) {
                return true;
            }
        }
        
        // King attacks
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = row + dr;
                int c = col + dc;
                if (isValidSquare(r, c) && board[r][c] == byColor * KING) {
                    return true;
                }
            }
        }
        
        // Sliding attacks (bishop, rook, queen)
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        for (int i = 0; i < 8; i++) {
            int[] dir = directions[i];
            boolean diagonal = dir[0] != 0 && dir[1] != 0;
            
            int r = row + dir[0];
            int c = col + dir[1];
            
            while (isValidSquare(r, c)) {
                int piece = board[r][c];
                if (piece != EMPTY) {
                    if (piece == byColor * QUEEN ||
                        (diagonal && piece == byColor * BISHOP) ||
                        (!diagonal && piece == byColor * ROOK)) {
                        return true;
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        
        return false;
    }

    // ============================================================================
    // GAME STATUS
    // ============================================================================

    /**
     * Check if current side is in check
     */
    public boolean isInCheck() {
        // Find king
        int kingRow = -1, kingCol = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == KING * sideToMove) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
        }
        
        return isSquareAttacked(kingRow, kingCol, -sideToMove);
    }

    /**
     * Check if current side is checkmated
     */
    public boolean isCheckmate() {
        return isInCheck() && generatePseudoLegalMoves().stream()
                .noneMatch(this::isLegalMove);
    }

    /**
     * Check if current side is stalemated
     */
    public boolean isStalemate() {
        return !isInCheck() && generatePseudoLegalMoves().stream()
                .noneMatch(this::isLegalMove);
    }

    // ============================================================================
    // EVALUATION
    // ============================================================================

    /**
     * Evaluate the current position
     * @return Score in centipawns (positive = white advantage)
     */
    public int evaluate() {
        int score = 0;
        int whiteMaterial = 0;
        int blackMaterial = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece != EMPTY) {
                    int type = Math.abs(piece);
                    int value = PIECE_VALUES[type];
                    int psq = getPieceSquareScore(type, row, col, piece > 0 ? WHITE : BLACK);
                    
                    if (piece > 0) {
                        score += value + psq;
                        whiteMaterial += value;
                    } else {
                        score -= value + psq;
                        blackMaterial += value;
                    }
                }
            }
        }
        
        // Return score from side-to-move's perspective
        return score * sideToMove;
    }

    /**
     * Get piece-square table score
     * CRITICAL: Black pieces use (7 - row) for table lookup
     */
    private int getPieceSquareScore(int type, int row, int col, int color) {
        // Flip row for black pieces
        int tableR = color == WHITE ? row : 7 - row;
        int tableC = col;
        
        switch (type) {
            case PAWN:
                return PAWN_TABLE[tableR][tableC];
            case KNIGHT:
                return KNIGHT_TABLE[tableR][tableC];
            case BISHOP:
                return BISHOP_TABLE[tableR][tableC];
            case ROOK:
                return ROOK_TABLE[tableR][tableC];
            case QUEEN:
                return QUEEN_TABLE[tableR][tableC];
            case KING:
                // Use endgame table if material is low
                return KING_MIDDLE_TABLE[tableR][tableC];
            default:
                return 0;
        }
    }

    // ============================================================================
    // SEARCH
    // ============================================================================

    /**
     * Search for best move using alpha-beta
     * @param depth Search depth
     * @return Search result
     */
    public SearchResult search(int depth) {
        nodesSearched = 0;
        
        List<Move> moves = generatePseudoLegalMoves();
        moves.removeIf(m -> !isLegalMove(m));
        
        if (moves.isEmpty()) {
            return new SearchResult(null, isInCheck() ? -30000 : 0, 0, null);
        }
        
        Move bestMove = null;
        int bestScore = -Integer.MAX_VALUE;
        
        // Order moves for better pruning
        orderMoves(moves);
        
        for (Move move : moves) {
            makeMove(move);
            int score = -alphaBeta(depth - 1, -Integer.MAX_VALUE, Integer.MAX_VALUE);
            unmakeMove(move);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return new SearchResult(bestMove, bestScore, nodesSearched, null);
    }

    private long nodesSearched = 0;

    private int alphaBeta(int depth, int alpha, int beta) {
        nodesSearched++;
        
        // Check transposition table
        TranspositionEntry entry = transpositionTable.get(zobristKey);
        if (entry != null && entry.key == zobristKey && entry.depth >= depth) {
            if (entry.flag == TranspositionEntry.EXACT) return entry.score;
            if (entry.flag == TranspositionEntry.LOWER && entry.score >= beta) return entry.score;
            if (entry.flag == TranspositionEntry.UPPER && entry.score <= alpha) return entry.score;
        }
        
        if (depth == 0) {
            return quiescence(alpha, beta);
        }
        
        List<Move> moves = generatePseudoLegalMoves();
        moves.removeIf(m -> !isLegalMove(m));
        
        if (moves.isEmpty()) {
            return isInCheck() ? -30000 + (10 - depth) : 0;
        }
        
        orderMoves(moves);
        
        int bestScore = -Integer.MAX_VALUE;
        int flag = TranspositionEntry.UPPER;
        
        for (Move move : moves) {
            makeMove(move);
            int score = -alphaBeta(depth - 1, -beta, -alpha);
            unmakeMove(move);
            
            if (score > bestScore) {
                bestScore = score;
                
                if (score > alpha) {
                    alpha = score;
                    flag = TranspositionEntry.EXACT;
                }
                
                if (alpha >= beta) {
                    flag = TranspositionEntry.LOWER;
                    break;
                }
            }
        }
        
        // Store in transposition table
        transpositionTable.put(zobristKey, new TranspositionEntry(
                zobristKey, depth, bestScore, flag));
        
        return bestScore;
    }

    private int quiescence(int alpha, int beta) {
        int standPat = evaluate();
        
        if (standPat >= beta) return beta;
        if (alpha < standPat) alpha = standPat;
        
        // Only search captures
        List<Move> captures = generatePseudoLegalMoves();
        captures.removeIf(m -> m.captured == EMPTY || !isLegalMove(m));
        
        orderMoves(captures);
        
        for (Move move : captures) {
            makeMove(move);
            int score = -quiescence(-beta, -alpha);
            unmakeMove(move);
            
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        
        return alpha;
    }

    private void orderMoves(List<Move> moves) {
        moves.sort((a, b) -> {
            int scoreA = moveScore(a);
            int scoreB = moveScore(b);
            return Integer.compare(scoreB, scoreA);
        });
    }

    private int moveScore(Move move) {
        int score = 0;
        
        // MVV-LVA for captures
        if (move.captured != EMPTY) {
            score += 10 * PIECE_VALUES[Math.abs(move.captured)] - 
                    PIECE_VALUES[Math.abs(move.piece)];
        }
        
        // Promotion bonus
        if (move.promotion != '\0') {
            score += PIECE_VALUES[Math.abs(charToPiece(move.promotion))];
        }
        
        return score;
    }

    // ============================================================================
    // UNMAKE MOVE
    // ============================================================================

    private void unmakeMove(Move move) {
        // Restore piece
        board[move.fromRow][move.fromCol] = move.piece;
        board[move.toRow][move.toCol] = move.captured;
        
        // Restore en passant capture
        if (move.isEnPassant) {
            int captureRow = move.piece > 0 ? move.toRow + 1 : move.toRow - 1;
            board[captureRow][move.toCol] = move.piece > 0 ? -PAWN : PAWN;
        }
        
        // Restore castling rook
        if (move.isCastling) {
            if (move.toCol == 6) { // King side
                board[move.toRow][7] = board[move.toRow][5];
                board[move.toRow][5] = EMPTY;
            } else { // Queen side
                board[move.toRow][0] = board[move.toRow][3];
                board[move.toRow][3] = EMPTY;
            }
        }
        
        // Switch side back
        sideToMove = -sideToMove;
        
        // Recompute Zobrist key
        computeZobristKey();
    }

    // ============================================================================
    // ZOBRIST HASHING
    // ============================================================================

    private void computeZobristKey() {
        zobristKey = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece != EMPTY) {
                    int index = (piece + 6); // Map -6..6 to 0..12
                    zobristKey ^= zobristTable[row][col][index];
                }
            }
        }
        
        if (sideToMove == BLACK) {
            zobristKey ^= zobristSide;
        }
        
        if (whiteKingSideCastle) zobristKey ^= zobristCastling[0];
        if (whiteQueenSideCastle) zobristKey ^= zobristCastling[1];
        if (blackKingSideCastle) zobristKey ^= zobristCastling[2];
        if (blackQueenSideCastle) zobristKey ^= zobristCastling[3];
        
        if (enPassantCol >= 0) {
            zobristKey ^= zobristEnPassant[enPassantCol];
        }
    }

    // ============================================================================
    // GETTERS
    // ============================================================================

    public int getSideToMove() {
        return sideToMove;
    }

    public String getLastMoveSan() {
        return lastMoveSan;
    }

    public String getPieceAt(int col, int row) {
        if (!isValidSquare(row, col)) return null;
        int piece = board[row][col];
        if (piece == EMPTY) return null;
        return String.valueOf(pieceToChar(piece));
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    @Data
    public static class Move {
        int fromRow, fromCol;
        int toRow, toCol;
        int piece;
        int captured;
        char promotion;
        boolean isEnPassant;
        boolean isCastling;
        String san;

        public Move(int fromRow, int fromCol, int toRow, int toCol, 
                    int piece, int captured, char promotion) {
            this(fromRow, fromCol, toRow, toCol, piece, captured, promotion, false, false);
        }

        public Move(int fromRow, int fromCol, int toRow, int toCol,
                    int piece, int captured, char promotion, boolean isEnPassant) {
            this(fromRow, fromCol, toRow, toCol, piece, captured, promotion, isEnPassant, false);
        }

        public Move(int fromRow, int fromCol, int toRow, int toCol,
                    int piece, int captured, char promotion, boolean isEnPassant, boolean isCastling) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
            this.piece = piece;
            this.captured = captured;
            this.promotion = promotion;
            this.isEnPassant = isEnPassant;
            this.isCastling = isCastling;
        }

        public String toUci() {
            StringBuilder uci = new StringBuilder();
            uci.append((char)('a' + fromCol));
            uci.append(8 - fromRow);
            uci.append((char)('a' + toCol));
            uci.append(8 - toRow);
            if (promotion != '\0') {
                uci.append(Character.toLowerCase(promotion));
            }
            return uci.toString();
        }

        public String fromUci() {
            StringBuilder uci = new StringBuilder();
            uci.append((char)('a' + fromCol));
            uci.append(8 - fromRow);
            return uci.toString();
        }
    }

    @Data
    public static class SearchResult {
        public Move bestMove;
        public int score;
        public long nodes;
        public List<String> pv;

        public SearchResult(Move bestMove, int score, long nodes, List<String> pv) {
            this.bestMove = bestMove;
            this.score = score;
            this.nodes = nodes;
            this.pv = pv;
        }
    }

    private static class TranspositionEntry {
        static final int EXACT = 0;
        static final int LOWER = 1;
        static final int UPPER = 2;

        long key;
        int depth;
        int score;
        int flag;

        TranspositionEntry(long key, int depth, int score, int flag) {
            this.key = key;
            this.depth = depth;
            this.score = score;
            this.flag = flag;
        }
    }
}
