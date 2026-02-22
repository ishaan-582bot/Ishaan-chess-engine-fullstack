/**
 * GameState - Manages chess game state, history, and draw detection
 * 
 * Features:
 * - Complete game state tracking
 * - Move history with navigation
 * - Undo functionality (up to 10 moves)
 * - Captured pieces tracking
 * - Draw detection (50-move rule, repetition, insufficient material)
 * - Position hash caching
 * - LocalStorage persistence
 * 
 * @author Chess Frontend Developer
 * @version 1.0.1 - Fixed undo legalMoves restoration and captured pieces logic
 */

class GameState {
    constructor() {
        this.reset();
    }

    /**
     * Reset game state to initial values
     */
    reset() {
        this.gameId = null;
        this.fen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
        this.sideToMove = 'white';
        this.moveHistory = [];
        this.stateHistory = []; // For undo functionality
        this.capturedByWhite = [];
        this.capturedByBlack = [];
        this.positionHashes = []; // For repetition detection
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        this.legalMoves = []; // Current legal moves - CRITICAL for undo restoration
        this.gameStatus = {
            inCheck: false,
            checkmate: false,
            stalemate: false,
            draw: false,
            description: 'Game in progress'
        };
        this.castlingRights = { K: true, Q: true, k: true, q: true };
        this.enPassantTarget = null;
        this.currentMoveIndex = -1; // For history navigation
        this.maxUndos = 10;
    }

    // ============================================================================
    // STATE MANAGEMENT
    // ============================================================================

    /**
     * Initialize a new game
     * @param {Object} gameData - Data from API new game response
     */
    initGame(gameData) {
        this.reset();
        this.gameId = gameData.gameId;
        this.fen = gameData.fen;
        this.sideToMove = gameData.sideToMove;
        this.legalMoves = gameData.legalMoves || []; // Store initial legal moves
        this.gameStatus = gameData.gameStatus || this.gameStatus;
        
        // Parse initial FEN for counters
        this.parseFen(this.fen);
        
        // Store initial position hash
        this.addPositionHash(this.fen);
        
        // Save initial state
        this.saveState();
        
        // Persist to localStorage
        this.persist();
    }

    /**
     * Update state after a move
     * @param {Object} moveData - Data from API move response
     * @param {string} from - Source square
     * @param {string} to - Destination square
     * @param {string|null} capturedPiece - Captured piece (if any)
     */
    updateAfterMove(moveData, from, to, capturedPiece = null) {
        // FIX: Capture the moving side BEFORE we update sideToMove
        const movingSide = this.sideToMove; // "white" or "black" - who is moving NOW

        // Store state before move for undo
        this.saveState();

        // Update FEN and status
        this.fen = moveData.fen;
        this.sideToMove = moveData.sideToMove;
        this.gameStatus = moveData.gameStatus;
        this.halfmoveClock = moveData.halfmoveClock || 0;
        this.fullmoveNumber = moveData.fullmoveNumber || 1;
        this.legalMoves = moveData.legalMoves || []; // CRITICAL: Store new legal moves

        // FIX: Track captured pieces using movingSide (before the flip)
        if (capturedPiece) {
            if (movingSide === 'white') {
                // White just moved, captured black piece
                this.capturedByWhite.push(capturedPiece.toLowerCase());
            } else {
                // Black just moved, captured white piece
                this.capturedByBlack.push(capturedPiece.toUpperCase());
            }
        }

        // Add to move history
        const moveRecord = {
            moveNumber: Math.floor(this.moveHistory.length / 2) + 1,
            white: movingSide === 'white' ? moveData.move?.san : null,
            black: movingSide === 'black' ? moveData.move?.san : null,
            from,
            to,
            fen: moveData.fen,
            capturedPiece
        };
        this.moveHistory.push(moveRecord);
        this.currentMoveIndex = this.moveHistory.length - 1;

        // Track position for repetition
        this.addPositionHash(this.fen);

        // Parse new FEN
        this.parseFen(this.fen);

        // Persist
        this.persist();
    }

    /**
     * Parse FEN string to extract game state
     * @param {string} fen - FEN string
     */
    parseFen(fen) {
        const parts = fen.split(' ');
        if (parts.length < 4) return;

        // Side to move
        this.sideToMove = parts[1] === 'w' ? 'white' : 'black';

        // Castling rights
        const castling = parts[2];
        this.castlingRights = {
            K: castling.includes('K'),
            Q: castling.includes('Q'),
            k: castling.includes('k'),
            q: castling.includes('q')
        };

        // En passant
        this.enPassantTarget = parts[3] !== '-' ? parts[3] : null;

        // Counters
        this.halfmoveClock = parseInt(parts[4]) || 0;
        this.fullmoveNumber = parseInt(parts[5]) || 1;
    }

    // ============================================================================
    // UNDO FUNCTIONALITY
    // ============================================================================

    /**
     * Save current state to history for undo
     * FIX: Now includes legalMoves for proper restoration
     */
    saveState() {
        const state = {
            fen: this.fen,
            sideToMove: this.sideToMove,
            legalMoves: this.legalMoves, // CRITICAL FIX: Save legalMoves
            moveHistory: [...this.moveHistory],
            capturedByWhite: [...this.capturedByWhite],
            capturedByBlack: [...this.capturedByBlack],
            positionHashes: [...this.positionHashes],
            halfmoveClock: this.halfmoveClock,
            fullmoveNumber: this.fullmoveNumber,
            gameStatus: { ...this.gameStatus },
            castlingRights: { ...this.castlingRights },
            enPassantTarget: this.enPassantTarget
        };

        this.stateHistory.push(state);

        // Limit history size
        if (this.stateHistory.length > this.maxUndos) {
            this.stateHistory.shift();
        }
    }

    /**
     * Undo the last move
     * FIX: Now restores legalMoves from previous state
     * @returns {Object|null} Previous state or null if can't undo
     */
    undo() {
        if (this.stateHistory.length === 0) return null;

        const previousState = this.stateHistory.pop();
        
        this.fen = previousState.fen;
        this.sideToMove = previousState.sideToMove;
        this.legalMoves = previousState.legalMoves || []; // CRITICAL FIX: Restore legalMoves
        this.moveHistory = previousState.moveHistory;
        this.capturedByWhite = previousState.capturedByWhite;
        this.capturedByBlack = previousState.capturedByBlack;
        this.positionHashes = previousState.positionHashes;
        this.halfmoveClock = previousState.halfmoveClock;
        this.fullmoveNumber = previousState.fullmoveNumber;
        this.gameStatus = previousState.gameStatus;
        this.castlingRights = previousState.castlingRights;
        this.enPassantTarget = previousState.enPassantTarget;
        this.currentMoveIndex = this.moveHistory.length - 1;

        this.persist();
        return previousState;
    }

    /**
     * Check if undo is available
     * @returns {boolean}
     */
    canUndo() {
        return this.stateHistory.length > 0;
    }

    // ============================================================================
    // MOVE HISTORY NAVIGATION
    // ============================================================================

    /**
     * Navigate to a specific move in history
     * @param {number} index - Move index
     * @returns {Object|null} Move at that index
     */
    goToMove(index) {
        if (index < 0 || index >= this.moveHistory.length) return null;
        this.currentMoveIndex = index;
        return this.moveHistory[index];
    }

    /**
     * Go to the first move
     * @returns {Object|null}
     */
    goToStart() {
        return this.goToMove(0);
    }

    /**
     * Go to the last move
     * @returns {Object|null}
     */
    goToEnd() {
        return this.goToMove(this.moveHistory.length - 1);
    }

    /**
     * Go to previous move
     * @returns {Object|null}
     */
    goToPrevious() {
        return this.goToMove(this.currentMoveIndex - 1);
    }

    /**
     * Go to next move
     * @returns {Object|null}
     */
    goToNext() {
        return this.goToMove(this.currentMoveIndex + 1);
    }

    // ============================================================================
    // DRAW DETECTION
    // ============================================================================

    /**
     * Add position hash for repetition detection
     * @param {string} fen - FEN string
     */
    addPositionHash(fen) {
        // Use only position part of FEN (ignore move counters)
        const positionPart = fen.split(' ').slice(0, 4).join(' ');
        const hash = this.simpleHash(positionPart);
        this.positionHashes.push(hash);
    }

    /**
     * Simple hash function for position
     * @param {string} str - String to hash
     * @returns {number} Hash value
     */
    simpleHash(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return hash;
    }

    /**
     * Check for threefold repetition
     * @returns {boolean}
     */
    isThreefoldRepetition() {
        if (this.positionHashes.length < 3) return false;
        
        const currentHash = this.positionHashes[this.positionHashes.length - 1];
        let count = 0;
        
        for (const hash of this.positionHashes) {
            if (hash === currentHash) count++;
        }
        
        return count >= 3;
    }

    /**
     * Check if 50-move rule applies
     * @returns {boolean}
     */
    isFiftyMoveRule() {
        return this.halfmoveClock >= 100;
    }

    /**
     * Check for insufficient material
     * @returns {boolean}
     */
    hasInsufficientMaterial() {
        const position = this.fen.split(' ')[0];
        
        // Count pieces
        let whitePieces = { K: 0, N: 0, B: 0 };
        let blackPieces = { K: 0, N: 0, B: 0 };
        
        for (const char of position) {
            if (char === 'K') whitePieces.K++;
            else if (char === 'N') whitePieces.N++;
            else if (char === 'B') whitePieces.B++;
            else if (char === 'k') blackPieces.K++;
            else if (char === 'n') blackPieces.N++;
            else if (char === 'b') blackPieces.B++;
        }

        // King vs King
        if (whitePieces.N === 0 && whitePieces.B === 0 && 
            blackPieces.N === 0 && blackPieces.B === 0) {
            return true;
        }

        // King and minor piece vs King
        if ((whitePieces.N === 1 || whitePieces.B === 1) && 
            blackPieces.N === 0 && blackPieces.B === 0) {
            return true;
        }
        if ((blackPieces.N === 1 || blackPieces.B === 1) && 
            whitePieces.N === 0 && whitePieces.B === 0) {
            return true;
        }

        return false;
    }

    /**
     * Get draw reason if applicable
     * @returns {string|null}
     */
    getDrawReason() {
        if (this.isThreefoldRepetition()) return 'threefold repetition';
        if (this.isFiftyMoveRule()) return '50-move rule';
        if (this.hasInsufficientMaterial()) return 'insufficient material';
        return null;
    }

    /**
     * Check if game should be declared a draw
     * @returns {boolean}
     */
    shouldDeclareDraw() {
        return this.isFiftyMoveRule() || this.hasInsufficientMaterial();
    }

    // ============================================================================
    // CAPTURED PIECES
    // ============================================================================

    /**
     * Get material balance from captured pieces
     * @returns {number} Positive = white ahead, negative = black ahead
     */
    getMaterialBalance() {
        const values = { p: 1, n: 3, b: 3, r: 5, q: 9 };
        
        let whiteScore = 0;
        let blackScore = 0;
        
        this.capturedByWhite.forEach(p => {
            whiteScore += values[p] || 0;
        });
        
        this.capturedByBlack.forEach(p => {
            blackScore += values[p.toLowerCase()] || 0;
        });
        
        return whiteScore - blackScore;
    }

    /**
     * Get captured pieces display string
     * @param {string} color - 'white' or 'black'
     * @returns {string}
     */
    getCapturedDisplay(color) {
        const pieces = color === 'white' ? this.capturedByWhite : this.capturedByBlack;
        const pieceMap = {
            'p': '♟', 'n': '♞', 'b': '♝', 'r': '♜', 'q': '♛',
            'P': '♙', 'N': '♘', 'B': '♗', 'R': '♖', 'Q': '♕'
        };
        return pieces.map(p => pieceMap[p] || p).join(' ');
    }

    // ============================================================================
    // LOCALSTORAGE PERSISTENCE
    // ============================================================================

    /**
     * Save game state to localStorage
     */
    persist() {
        try {
            const data = {
                gameId: this.gameId,
                fen: this.fen,
                sideToMove: this.sideToMove,
                legalMoves: this.legalMoves, // Include legalMoves in persistence
                moveHistory: this.moveHistory,
                capturedByWhite: this.capturedByWhite,
                capturedByBlack: this.capturedByBlack,
                positionHashes: this.positionHashes,
                halfmoveClock: this.halfmoveClock,
                fullmoveNumber: this.fullmoveNumber,
                gameStatus: this.gameStatus,
                timestamp: Date.now()
            };
            localStorage.setItem('chessGameState', JSON.stringify(data));
        } catch (error) {
            console.warn('Failed to persist game state:', error);
        }
    }

    /**
     * Load game state from localStorage
     * @returns {boolean} True if state was loaded successfully
     */
    loadFromStorage() {
        try {
            const data = localStorage.getItem('chessGameState');
            if (!data) return false;

            const parsed = JSON.parse(data);
            
            // Validate data structure
            if (!parsed.gameId || !parsed.fen) {
                console.warn('Invalid stored game state');
                localStorage.removeItem('chessGameState');
                return false;
            }

            // Check if game is too old (24 hours)
            if (parsed.timestamp && Date.now() - parsed.timestamp > 24 * 60 * 60 * 1000) {
                console.log('Stored game expired');
                localStorage.removeItem('chessGameState');
                return false;
            }

            // Restore state
            this.gameId = parsed.gameId;
            this.fen = parsed.fen;
            this.sideToMove = parsed.sideToMove;
            this.legalMoves = parsed.legalMoves || []; // Restore legalMoves
            this.moveHistory = parsed.moveHistory || [];
            this.capturedByWhite = parsed.capturedByWhite || [];
            this.capturedByBlack = parsed.capturedByBlack || [];
            this.positionHashes = parsed.positionHashes || [];
            this.halfmoveClock = parsed.halfmoveClock || 0;
            this.fullmoveNumber = parsed.fullmoveNumber || 1;
            this.gameStatus = parsed.gameStatus || this.gameStatus;
            this.currentMoveIndex = this.moveHistory.length - 1;

            this.parseFen(this.fen);
            
            return true;
        } catch (error) {
            console.warn('Failed to load game state:', error);
            localStorage.removeItem('chessGameState');
            return false;
        }
    }

    /**
     * Clear saved game state
     */
    clearStorage() {
        localStorage.removeItem('chessGameState');
    }

    // ============================================================================
    // PGN GENERATION
    // ============================================================================

    /**
     * Generate PGN string from game
     * @param {Object} headers - Optional PGN headers
     * @returns {string}
     */
    generatePGN(headers = {}) {
        const defaultHeaders = {
            Event: 'Chess Engine Game',
            Site: 'Chess Engine Web App',
            Date: new Date().toISOString().split('T')[0].replace(/-/g, '.'),
            Round: '-',
            White: 'Player',
            Black: 'Engine',
            Result: this.getResult()
        };

        const allHeaders = { ...defaultHeaders, ...headers };
        
        let pgn = '';
        
        // Add headers
        for (const [key, value] of Object.entries(allHeaders)) {
            pgn += `[${key} "${value}"]\n`;
        }
        
        pgn += '\n';

        // Add moves
        for (let i = 0; i < this.moveHistory.length; i++) {
            const move = this.moveHistory[i];
            if (move.white) {
                pgn += `${move.moveNumber}. ${move.white}`;
                if (move.black) {
                    pgn += ` ${move.black}`;
                }
                pgn += ' ';
            }
        }

        // Add result
        pgn += allHeaders.Result;

        return pgn;
    }

    /**
     * Get game result for PGN
     * @returns {string}
     */
    getResult() {
        if (this.gameStatus.checkmate) {
            return this.sideToMove === 'white' ? '0-1' : '1-0';
        }
        if (this.gameStatus.stalemate || this.gameStatus.draw) {
            return '1/2-1/2';
        }
        return '*';
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Get the king's square for a given color
     * @param {string} color - 'white' or 'black'
     * @returns {string|null}
     */
    getKingSquare(color) {
        const king = color === 'white' ? 'K' : 'k';
        const position = this.fen.split(' ')[0];
        const ranks = position.split('/');
        
        for (let row = 0; row < 8; row++) {
            let col = 0;
            for (const char of ranks[row]) {
                if (/\d/.test(char)) {
                    col += parseInt(char);
                } else if (char === king) {
                    const files = 'abcdefgh';
                    return files[col] + (8 - row);
                } else {
                    col++;
                }
            }
        }
        
        return null;
    }

    /**
     * Check if a square is under attack
     * @param {string} square - Square to check
     * @returns {boolean}
     */
    isSquareAttacked(square) {
        // This would need the engine's attack detection
        // For now, return false (simplified)
        return false;
    }

    /**
     * Get game summary
     * @returns {Object}
     */
    getSummary() {
        return {
            gameId: this.gameId,
            moveCount: this.moveHistory.length,
            currentMove: this.currentMoveIndex + 1,
            sideToMove: this.sideToMove,
            status: this.gameStatus.description,
            materialBalance: this.getMaterialBalance(),
            canUndo: this.canUndo(),
            drawReason: this.getDrawReason()
        };
    }
}

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { GameState };
}
