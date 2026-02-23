/**
 * Chess Engine Web Application - Main Entry Point
 * 
 * This is the main application controller that coordinates:
 * - ChessBoard: Board rendering and piece movement
 * - ChessApiClient: Backend API communication
 * - GameState: Game state management and history
 * - AudioManager: Sound effects
 * 
 * @author Chess Frontend Developer
 * @version 1.0.1 - Fixed auto-engine race condition and promotion timer
 */

class ChessApp {
    constructor() {
        // API Configuration - Dynamic based on environment
        const isLocalhost = window.location.hostname === 'localhost' || 
                           window.location.hostname === '127.0.0.1';
        this.apiBaseUrl = isLocalhost
            ? 'http://localhost:8080/api'
            : 'https://ishaan-chess-engine-fullstack.onrender.com/api';
        
        this.api = new ChessApiClient(this.apiBaseUrl);
        
        // Game state
        this.gameState = new GameState();
        this.board = null;
        this.audio = new AudioManager();
        
        // Settings
        this.settings = {
            engineDepth: 5,
            playerColor: 'white',
            confirmMoves: false,
            soundEnabled: true,
            showCoordinates: true,
            coordinateTraining: false,
            autoPlayEngine: true
        };
        
        // State machine
        this.appState = 'idle'; // idle, loading, thinking, gameOver
        this.engineThinking = false;
        this.legalMoves = [];
        this.hintMoves = [];
        
        // Promotion state
        this.pendingPromotion = null;
        this.promotionTimeout = null;
        this.promotionInterval = null;
        
        // DOM element references
        this.elements = {};
        
        this.init();
    }

    /**
     * Initialize the application
     */
    async init() {
        this.cacheElements();
        this.initBoard();
        this.attachEventListeners();
        this.loadSettings();
        
        // Check API connection
        await this.checkConnection();
        
        // Try to restore saved game
        if (this.gameState.loadFromStorage()) {
            this.restoreGame();
        } else {
            // Start new game
            await this.newGame();
        }
        
        // Setup global error handler
        this.setupErrorHandling();
        
        // Initialize audio on first user interaction
        document.addEventListener('click', () => {
            this.audio.init();
        }, { once: true });
    }

    /**
     * Cache DOM element references
     */
    cacheElements() {
        this.elements = {
            // Connection status
            connectionStatus: document.getElementById('connection-status'),
            statusText: document.querySelector('.status-text'),
            reconnectBtn: document.getElementById('reconnect-btn'),
            
            // Board
            chessboard: document.getElementById('chessboard'),
            arrowOverlay: document.getElementById('arrow-overlay'),
            
            // Game status
            gameStatusDisplay: document.getElementById('game-status-display'),
            statusLabel: document.querySelector('.status-label'),
            
            // Evaluation
            evalScore: document.getElementById('eval-score'),
            evalBar: document.getElementById('eval-bar'),
            evalAssessment: document.getElementById('eval-assessment'),
            evalDepth: document.getElementById('eval-depth'),
            
            // Engine status
            engineStatus: document.getElementById('engine-status'),
            cancelEngineBtn: document.getElementById('cancel-engine-btn'),
            
            // Move history
            moveHistory: document.getElementById('move-history'),
            
            // Captured pieces
            capturedByWhite: document.getElementById('captured-by-white'),
            capturedByBlack: document.getElementById('captured-by-black'),
            
            // Game info
            fiftyMoveCounter: document.getElementById('fifty-move-counter'),
            repetitionCounter: document.getElementById('repetition-counter'),
            
            // Controls
            newGameBtn: document.getElementById('new-game-btn'),
            undoBtn: document.getElementById('undo-btn'),
            flipBoardBtn: document.getElementById('flip-board-btn'),
            hintBtn: document.getElementById('hint-btn'),
            autoEngineBtn: document.getElementById('auto-engine-btn'),
            
            // Hint display
            hintDisplay: document.getElementById('hint-display'),
            hintMoves: document.getElementById('hint-moves'),
            
            // FEN
            fenInput: document.getElementById('fen-input'),
            loadFenBtn: document.getElementById('load-fen-btn'),
            
            // Game actions
            offerDrawBtn: document.getElementById('offer-draw-btn'),
            resignBtn: document.getElementById('resign-btn'),
            exportPgnBtn: document.getElementById('export-pgn-btn'),
            
            // Promotion modal
            promotionModal: document.getElementById('promotion-modal'),
            
            // Settings modal
            settingsModal: document.getElementById('settings-modal'),
            settingsBtn: document.getElementById('settings-btn'),
            saveSettingsBtn: document.getElementById('save-settings-btn'),
            engineDepth: document.getElementById('engine-depth'),
            playerColor: document.getElementById('player-color'),
            confirmMoves: document.getElementById('confirm-moves'),
            soundEnabled: document.getElementById('sound-enabled'),
            showCoordinates: document.getElementById('show-coordinates'),
            coordinateTraining: document.getElementById('coordinate-training'),
            
            // Help modal
            helpModal: document.getElementById('help-modal'),
            helpBtn: document.getElementById('help-btn'),
            
            // Toast
            errorToast: document.getElementById('error-toast'),
            errorMessage: document.getElementById('error-message'),
            
            // Loading
            loadingOverlay: document.getElementById('loading-overlay')
        };
    }

    /**
     * Initialize the chess board
     */
    initBoard() {
        this.board = new ChessBoard('chessboard', {
            onMove: this.handleMove.bind(this),
            onSquareClick: this.handleSquareClick.bind(this),
            onDragStart: this.handleDragStart.bind(this),
            onDragEnd: this.handleDragEnd.bind(this),
            confirmMoves: this.settings.confirmMoves
        });
    }

    /**
     * Attach event listeners
     */
    attachEventListeners() {
        // Control buttons
        this.elements.newGameBtn.addEventListener('click', () => this.newGame());
        this.elements.undoBtn.addEventListener('click', () => this.undoMove());
        this.elements.flipBoardBtn.addEventListener('click', () => this.flipBoard());
        this.elements.hintBtn.addEventListener('click', () => this.showHint());
        this.elements.autoEngineBtn.addEventListener('click', () => this.toggleAutoEngine());
        
        // FEN loading
        this.elements.loadFenBtn.addEventListener('click', () => this.loadFenPosition());
        this.elements.fenInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.loadFenPosition();
        });
        
        // Game actions
        this.elements.offerDrawBtn.addEventListener('click', () => this.offerDraw());
        this.elements.resignBtn.addEventListener('click', () => this.resign());
        this.elements.exportPgnBtn.addEventListener('click', () => this.exportPGN());
        
        // Cancel engine
        this.elements.cancelEngineBtn.addEventListener('click', () => this.cancelEngine());
        
        // Reconnect
        this.elements.reconnectBtn.addEventListener('click', () => this.checkConnection());
        
        // Settings modal
        this.elements.settingsBtn.addEventListener('click', () => this.openSettings());
        this.elements.saveSettingsBtn.addEventListener('click', () => this.saveSettings());
        
        // Help modal
        this.elements.helpBtn.addEventListener('click', () => this.openHelp());
        
        // Modal close buttons
        document.querySelectorAll('.modal-close').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.target.closest('.modal').classList.add('hidden');
            });
        });
        
        // Promotion pieces
        document.querySelectorAll('.promotion-piece').forEach(btn => {
            btn.addEventListener('click', (e) => this.handlePromotion(e.target.dataset.piece));
        });
        
        // Toast close
        document.querySelector('.toast-close')?.addEventListener('click', () => {
            this.elements.errorToast.classList.add('hidden');
        });
        
        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => this.handleKeyDown(e));
        
        // Global error handling
        window.addEventListener('unhandledrejection', (e) => {
            console.error('Unhandled promise rejection:', e.reason);
            this.showError('An unexpected error occurred. Please try again.');
        });
    }

    // ============================================================================
    // CONNECTION MANAGEMENT
    // ============================================================================

    /**
     * Check API connection status
     */
    async checkConnection() {
        this.setConnectionStatus('connecting');
        
        try {
            const connected = await this.api.ping();
            if (connected) {
                this.setConnectionStatus('connected');
                setTimeout(() => {
                    this.elements.connectionStatus.classList.add('hidden');
                }, 2000);
            } else {
                this.setConnectionStatus('error');
            }
        } catch (error) {
            this.setConnectionStatus('error');
        }
    }

    setConnectionStatus(status) {
        const el = this.elements.connectionStatus;
        el.classList.remove('connecting', 'connected', 'error', 'hidden');
        
        switch (status) {
            case 'connecting':
                el.classList.add('connecting');
                this.elements.statusText.textContent = 'Connecting to server...';
                this.elements.reconnectBtn.classList.add('hidden');
                break;
            case 'connected':
                el.classList.add('connected');
                this.elements.statusText.textContent = 'Connected!';
                this.elements.reconnectBtn.classList.add('hidden');
                break;
            case 'error':
                el.classList.add('error');
                this.elements.statusText.textContent = 'Server connection failed';
                this.elements.reconnectBtn.classList.remove('hidden');
                break;
        }
    }

    // ============================================================================
    // GAME MANAGEMENT
    // ============================================================================

    /**
     * Start a new game
     */
    async newGame() {
        this.setLoading(true);
        this.setAppState('loading');
        
        try {
            const gameData = await this.api.createGame({
                mode: 'human_vs_engine',
                playerColor: this.settings.playerColor,
                engineDepth: this.settings.engineDepth
            });
            
            this.gameState.initGame(gameData);
            this.legalMoves = gameData.legalMoves || [];
            
            this.board.setPosition(gameData.fen);
            this.updateUI();
            this.clearHighlights();
            
            this.setAppState('idle');
            this.showToast('New game started!', 'success');
            
            // If player is black, trigger engine move
            if (this.settings.playerColor === 'black' && this.settings.autoPlayEngine) {
                setTimeout(() => this.requestEngineMove(), 500);
            }
            
        } catch (error) {
            this.handleError(error);
            this.setAppState('idle');
        } finally {
            this.setLoading(false);
        }
    }

    /**
     * Restore a saved game
     */
    restoreGame() {
        this.board.setPosition(this.gameState.fen);
        this.legalMoves = this.gameState.legalMoves || []; // Restore legalMoves from saved state
        this.updateUI();
        this.showToast('Game restored!', 'success');
    }

    /**
     * Load a position from FEN
     */
    async loadFenPosition() {
        const fen = this.elements.fenInput.value.trim();
        if (!fen) {
            this.showError('Please enter a FEN string');
            return;
        }
        
        this.setLoading(true);
        
        try {
            // Validate FEN first
            const validation = await this.api.validateFen(fen);
            if (!validation.valid) {
                this.showError('Invalid FEN string');
                return;
            }
            
            // Create new game with FEN
            const gameData = await this.api.createGame({
                fen: fen,
                mode: 'human_vs_engine',
                playerColor: this.settings.playerColor,
                engineDepth: this.settings.engineDepth
            });
            
            this.gameState.initGame(gameData);
            this.legalMoves = gameData.legalMoves || [];
            
            this.board.setPosition(gameData.fen);
            this.updateUI();
            this.clearHighlights();
            
            this.showToast('Position loaded!', 'success');
            
        } catch (error) {
            this.handleError(error);
        } finally {
            this.setLoading(false);
        }
    }

    // ============================================================================
    // MOVE HANDLING
    // ============================================================================

    /**
     * Handle piece movement
     * CRITICAL FIX: Uses moveData.sideToMove for engine turn check to avoid race condition
     */
    async handleMove(from, to, promotion = null) {
        if (this.appState === 'thinking' || this.appState === 'gameOver') return;
        if (!this.gameState.gameId) return;
        
        // Check if it's player's turn
        const isPlayerTurn = this.isPlayerTurn();
        if (!isPlayerTurn) {
            this.board.animateIllegalMove(from);
            this.audio.playIllegal();
            return;
        }
        
        // Check if move is legal
        const uciMove = from + to + (promotion || '');
        if (!this.legalMoves.includes(uciMove) && !this.legalMoves.includes(from + to)) {
            this.board.animateIllegalMove(from);
            this.audio.playIllegal();
            return;
        }
        
        // Check for pawn promotion
        const piece = this.board.getPieceAt(from);
        if (piece && piece.toLowerCase() === 'p') {
            const toRow = parseInt(to[1]);
            if ((piece === 'P' && toRow === 8) || (piece === 'p' && toRow === 1)) {
                if (!promotion) {
                    this.showPromotionModal(from, to);
                    return;
                }
            }
        }
        
        this.setAppState('thinking');
        this.setLoading(true);
        
        try {
            // Detect capture before making move
            const capturedPiece = this.board.getPieceAt(to);
            
            const moveData = await this.api.makeMove(
                this.gameState.gameId,
                from,
                to,
                promotion
            );
            
            // Update game state
            this.gameState.updateAfterMove(moveData, from, to, capturedPiece);
            this.legalMoves = moveData.legalMoves || [];
            
            // Update board
            this.board.setPosition(moveData.fen);
            this.board.highlightLastMove(from, to);
            
            // Play sound
            if (capturedPiece) {
                this.audio.playCapture();
            } else if (moveData.move?.san?.includes('O-O')) {
                this.audio.playCastle();
            } else if (promotion) {
                this.audio.playPromotion();
            } else {
                this.audio.playMove();
            }
            
            // Check for check
            if (moveData.gameStatus?.inCheck) {
                const kingSquare = this.gameState.getKingSquare(moveData.sideToMove);
                this.board.highlightCheck(kingSquare);
                this.audio.playCheck();
            } else {
                this.board.clearCheck();
            }
            
            this.updateUI();
            
            // Check for game end
            if (moveData.gameStatus?.checkmate || moveData.gameStatus?.stalemate) {
                this.handleGameEnd(moveData.gameStatus);
                return;
            }
            
            // Check for draw conditions
            if (this.gameState.shouldDeclareDraw()) {
                const reason = this.gameState.getDrawReason();
                this.handleGameEnd({ description: `Draw by ${reason}` });
                return;
            }
            
            // CRITICAL FIX: Use newSideToMove from response to determine engine turn
            // This avoids race condition where gameState.sideToMove hasn't updated yet
            const newSideToMove = moveData.sideToMove; // "white" or "black" after the move
            const isNowEngineTurn = newSideToMove !== this.settings.playerColor;
            
            if (this.settings.autoPlayEngine && isNowEngineTurn && 
                !moveData.gameStatus?.checkmate && !moveData.gameStatus?.stalemate) {
                setTimeout(() => this.requestEngineMove(), 500);
            }
            
        } catch (error) {
            this.handleError(error);
            this.board.animateIllegalMove(from);
        } finally {
            this.setLoading(false);
            this.setAppState('idle');
        }
    }

    /**
     * Handle square click (for legal move highlighting)
     */
    async handleSquareClick(square) {
        const piece = this.board.getPieceAt(square);
        
        // Only show legal moves for player's pieces
        if (piece && this.isPlayerPiece(piece)) {
            // Fetch legal moves for this position
            try {
                const response = await this.api.getLegalMoves(this.gameState.fen);
                const movesFromSquare = response.legalMoves
                    .filter(m => m.startsWith(square))
                    .map(m => m.substring(2, 4));
                
                this.board.highlightLegalMoves(movesFromSquare);
            } catch (error) {
                console.warn('Failed to fetch legal moves:', error);
            }
        }
    }

    /**
     * Handle drag start
     */
    handleDragStart(square) {
        const piece = this.board.getPieceAt(square);
        return piece && this.isPlayerPiece(piece) && this.isPlayerTurn();
    }

    handleDragEnd() {
        // Cleanup if needed
    }

    /**
     * Show promotion modal
     * FIX: Added clearPromotionTimers() call and countdown display
     */
    showPromotionModal(from, to) {
        this.pendingPromotion = { from, to };
        this.elements.promotionModal.classList.remove('hidden');
        
        // Clear any existing timers
        this.clearPromotionTimers();
        
        // Auto-select queen after 3 seconds with countdown
        let secondsLeft = 3;
        const countdownEl = document.getElementById('promotion-countdown');
        if (countdownEl) countdownEl.textContent = secondsLeft;
        
        this.promotionInterval = setInterval(() => {
            secondsLeft--;
            if (countdownEl) countdownEl.textContent = secondsLeft;
            
            if (secondsLeft <= 0) {
                this.clearPromotionTimers();
                if (this.pendingPromotion) {
                    this.handlePromotion('q');
                }
            }
        }, 1000);
    }

    /**
     * CRITICAL FIX: Clear promotion timers to prevent auto-selection after manual choice
     */
    clearPromotionTimers() {
        if (this.promotionTimeout) {
            clearTimeout(this.promotionTimeout);
            this.promotionTimeout = null;
        }
        if (this.promotionInterval) {
            clearInterval(this.promotionInterval);
            this.promotionInterval = null;
        }
    }

    /**
     * Handle promotion piece selection
     * FIX: Now clears timers before processing
     */
    handlePromotion(piece) {
        this.clearPromotionTimers(); // CRITICAL: Stop auto-select
        this.elements.promotionModal.classList.add('hidden');
        
        if (this.pendingPromotion) {
            const { from, to } = this.pendingPromotion;
            this.pendingPromotion = null;
            this.handleMove(from, to, piece);
        }
    }

    // ============================================================================
    // ENGINE MOVES
    // ============================================================================

    /**
     * Request engine move
     */
    async requestEngineMove() {
        if (this.appState === 'gameOver') return;
        if (!this.gameState.gameId) return;
        
        this.setAppState('thinking');
        this.engineThinking = true;
        this.elements.engineStatus.classList.remove('hidden');
        
        try {
            const engineData = await this.api.getEngineMove(
                this.gameState.gameId,
                this.settings.engineDepth
            );
            
            const move = engineData.bestMove;
            const capturedPiece = this.board.getPieceAt(move.to);
            
            // Update game state
            this.gameState.updateAfterMove(engineData, move.from, move.to, capturedPiece);
            this.legalMoves = engineData.position?.legalMoves || [];
            
            // Update board
            this.board.setPosition(engineData.position.fen);
            this.board.highlightLastMove(move.from, move.to);
            this.board.drawArrow(move.from, move.to);
            setTimeout(() => this.board.clearArrows(), 2000);
            
            // Play sound
            if (capturedPiece) {
                this.audio.playCapture();
            } else if (move.san?.includes('O-O')) {
                this.audio.playCastle();
            } else {
                this.audio.playMove();
            }
            
            // Update evaluation display
            this.updateEvaluation(engineData.evaluation, engineData.searchStats);
            
            // Check for check
            if (this.gameState.gameStatus?.inCheck) {
                const kingSquare = this.gameState.getKingSquare(this.gameState.sideToMove);
                this.board.highlightCheck(kingSquare);
                this.audio.playCheck();
            } else {
                this.board.clearCheck();
            }
            
            this.updateUI();
            
            // Check for game end
            if (this.gameState.gameStatus?.checkmate || this.gameState.gameStatus?.stalemate) {
                this.handleGameEnd(this.gameState.gameStatus);
            }
            
        } catch (error) {
            this.handleError(error);
        } finally {
            this.engineThinking = false;
            this.elements.engineStatus.classList.add('hidden');
            this.setAppState('idle');
        }
    }

    /**
     * Cancel engine thinking
     */
    cancelEngine() {
        // Note: Actual cancellation would require backend support
        // For now, we just hide the UI
        this.engineThinking = false;
        this.elements.engineStatus.classList.add('hidden');
        this.setAppState('idle');
    }

    /**
     * Toggle auto-play engine
     */
    toggleAutoEngine() {
        this.settings.autoPlayEngine = !this.settings.autoPlayEngine;
        this.elements.autoEngineBtn.classList.toggle('toggle-on', this.settings.autoPlayEngine);
        this.elements.autoEngineBtn.classList.toggle('toggle-off', !this.settings.autoPlayEngine);
        this.elements.autoEngineBtn.innerHTML = 
            `<span class="btn-icon">🤖</span> Auto: ${this.settings.autoPlayEngine ? 'ON' : 'OFF'}`;
        
        this.saveSettings();
    }

    // ============================================================================
    // UNDO
    // ============================================================================

    /**
     * Undo last move
     * FIX: Now properly restores legalMoves from gameState
     */
    undoMove() {
        if (!this.gameState.canUndo()) return;
        
        const previousState = this.gameState.undo();
        if (previousState) {
            this.board.setPosition(previousState.fen);
            this.legalMoves = previousState.legalMoves || []; // Restore legalMoves
            this.board.clearLastMove();
            this.board.clearCheck();
            this.board.clearHints();
            this.updateUI();
            this.showToast('Move undone', 'success');
        }
    }

    // ============================================================================
    // HINTS
    // ============================================================================

    /**
     * Show top 3 engine moves as hints
     */
    async showHint() {
        if (!this.gameState.gameId) return;
        
        this.setLoading(true);
        
        try {
            // Get evaluation which includes best line
            const evalData = await this.api.evaluatePosition(this.gameState.fen);
            
            // For now, just show the best move
            const hintData = await this.api.getEngineMove(
                this.gameState.gameId,
                Math.min(4, this.settings.engineDepth)
            );
            
            const move = hintData.bestMove;
            this.board.highlightHint(move.from, move.to);
            this.board.drawArrow(move.from, move.to);
            
            // Show hint display
            this.elements.hintDisplay.classList.remove('hidden');
            this.elements.hintMoves.innerHTML = `
                <div class="hint-move">
                    <span class="hint-move-rank">1.</span>
                    <span class="hint-move-san">${move.san}</span>
                    <span class="hint-move-eval">${this.formatEval(hintData.evaluation?.score)}</span>
                </div>
            `;
            
            setTimeout(() => {
                this.board.clearHints();
                this.board.clearArrows();
            }, 5000);
            
        } catch (error) {
            this.handleError(error);
        } finally {
            this.setLoading(false);
        }
    }

    // ============================================================================
    // GAME END HANDLING
    // ============================================================================

    handleGameEnd(status) {
        this.setAppState('gameOver');
        this.gameState.gameStatus = { ...this.gameState.gameStatus, ...status };
        
        const isWin = status.description?.includes('wins') && 
                      status.description?.includes(this.settings.playerColor === 'white' ? 'White' : 'Black');
        
        this.audio.playGameEnd(isWin);
        this.updateUI();
        
        // Show game end notification
        setTimeout(() => {
            alert(status.description || 'Game Over');
        }, 500);
    }

    offerDraw() {
        if (this.gameState.isThreefoldRepetition() || this.gameState.isFiftyMoveRule()) {
            this.handleGameEnd({ description: 'Draw by agreement' });
        } else {
            this.showError('Draw can only be claimed on threefold repetition or 50-move rule');
        }
    }

    resign() {
        if (confirm('Are you sure you want to resign?')) {
            const winner = this.settings.playerColor === 'white' ? 'Black' : 'White';
            this.handleGameEnd({ description: `${winner} wins by resignation` });
        }
    }

    // ============================================================================
    // UI UPDATES
    // ============================================================================

    updateUI() {
        // Update game status
        const status = this.gameState.gameStatus;
        this.elements.statusLabel.textContent = status.description || 'Game in progress';
        this.elements.statusLabel.className = 'status-label';
        if (status.inCheck) this.elements.statusLabel.classList.add('check');
        if (status.checkmate) this.elements.statusLabel.classList.add('checkmate');
        
        // Update move history
        this.updateMoveHistory();
        
        // Update captured pieces
        this.elements.capturedByWhite.textContent = this.gameState.getCapturedDisplay('white');
        this.elements.capturedByBlack.textContent = this.gameState.getCapturedDisplay('black');
        
        // Update game info
        this.elements.fiftyMoveCounter.textContent = this.gameState.halfmoveClock;
        
        // Count repetitions
        const currentHash = this.gameState.positionHashes[this.gameState.positionHashes.length - 1];
        const repetitions = this.gameState.positionHashes.filter(h => h === currentHash).length;
        this.elements.repetitionCounter.textContent = repetitions;
        
        // Update undo button
        this.elements.undoBtn.disabled = !this.gameState.canUndo();
        
        // Update FEN input
        this.elements.fenInput.value = this.gameState.fen;
    }

    updateMoveHistory() {
        const history = this.gameState.moveHistory;
        let html = '';
        
        for (let i = 0; i < history.length; i++) {
            const move = history[i];
            const isCurrent = i === this.gameState.currentMoveIndex;
            
            html += `<div class="move-row ${isCurrent ? 'current' : ''}">`;
            html += `<span class="move-number">${move.moveNumber}.</span>`;
            
            if (move.white) {
                html += `<span class="move-white ${isCurrent && move.white ? 'move-current' : ''}" data-index="${i}">${move.white}</span>`;
            }
            if (move.black) {
                html += `<span class="move-black ${isCurrent && move.black ? 'move-current' : ''}" data-index="${i}">${move.black}</span>`;
            }
            
            html += '</div>';
        }
        
        this.elements.moveHistory.innerHTML = html;
    }

    updateEvaluation(evaluation, stats) {
        if (!evaluation) return;
        
        const score = evaluation.score || 0;
        const centipawns = score / 100;
        
        this.elements.evalScore.textContent = 
            (centipawns > 0 ? '+' : '') + centipawns.toFixed(1);
        
        this.elements.evalAssessment.textContent = 
            evaluation.assessment || 'Equal position';
        
        // Update eval bar (50% is equal, 0% is black winning, 100% is white winning)
        // Clamp between 10% and 90% for visibility
        const normalizedScore = Math.max(-1000, Math.min(1000, score));
        const barPosition = 50 + (normalizedScore / 20);
        this.elements.evalBar.style.width = `${Math.max(10, Math.min(90, barPosition))}%`;
        
        if (stats) {
            this.elements.evalDepth.textContent = `Depth: ${stats.depth}`;
        }
    }

    // ============================================================================
    // BOARD CONTROLS
    // ============================================================================

    flipBoard() {
        this.board.flip();
        this.flipped = this.board.flipped;
    }

    clearHighlights() {
        this.board.deselectSquare();
        this.board.clearLegalMoves();
        this.board.clearLastMove();
        this.board.clearCheck();
        this.board.clearHints();
        this.board.clearArrows();
    }

    // ============================================================================
    // SETTINGS
    // ============================================================================

    openSettings() {
        this.elements.engineDepth.value = this.settings.engineDepth;
        this.elements.playerColor.value = this.settings.playerColor;
        this.elements.confirmMoves.checked = this.settings.confirmMoves;
        this.elements.soundEnabled.checked = this.settings.soundEnabled;
        this.elements.showCoordinates.checked = this.settings.showCoordinates;
        this.elements.coordinateTraining.checked = this.settings.coordinateTraining;
        
        this.elements.settingsModal.classList.remove('hidden');
    }

    saveSettings() {
        this.settings.engineDepth = parseInt(this.elements.engineDepth.value);
        this.settings.playerColor = this.elements.playerColor.value;
        this.settings.confirmMoves = this.elements.confirmMoves.checked;
        this.settings.soundEnabled = this.elements.soundEnabled.checked;
        this.settings.showCoordinates = this.elements.showCoordinates.checked;
        this.settings.coordinateTraining = this.elements.coordinateTraining.checked;
        
        this.audio.setEnabled(this.settings.soundEnabled);
        this.board.confirmMoves = this.settings.confirmMoves;
        
        // Apply coordinate training
        document.body.classList.toggle('coordinate-training', this.settings.coordinateTraining);
        
        // Apply show coordinates
        document.body.classList.toggle('hide-coordinates', !this.settings.showCoordinates);
        
        localStorage.setItem('chessSettings', JSON.stringify(this.settings));
        
        this.elements.settingsModal.classList.add('hidden');
        this.showToast('Settings saved!', 'success');
    }

    loadSettings() {
        try {
            const saved = localStorage.getItem('chessSettings');
            if (saved) {
                this.settings = { ...this.settings, ...JSON.parse(saved) };
            }
        } catch (error) {
            console.warn('Failed to load settings:', error);
        }
        
        // Apply loaded settings
        this.audio.setEnabled(this.settings.soundEnabled);
        this.board.confirmMoves = this.settings.confirmMoves;
        
        // Update UI
        this.elements.autoEngineBtn.classList.toggle('toggle-on', this.settings.autoPlayEngine);
        this.elements.autoEngineBtn.classList.toggle('toggle-off', !this.settings.autoPlayEngine);
        this.elements.autoEngineBtn.innerHTML = 
            `<span class="btn-icon">🤖</span> Auto: ${this.settings.autoPlayEngine ? 'ON' : 'OFF'}`;
    }

    // ============================================================================
    // KEYBOARD SHORTCUTS
    // ============================================================================

    handleKeyDown(e) {
        // Ignore if in input field
        if (e.target.tagName === 'INPUT') return;
        
        switch (e.key) {
            case 'ArrowLeft':
                this.gameState.goToPrevious();
                this.updateUI();
                break;
            case 'ArrowRight':
                this.gameState.goToNext();
                this.updateUI();
                break;
            case 'ArrowUp':
                this.gameState.goToStart();
                this.updateUI();
                break;
            case 'ArrowDown':
                this.gameState.goToEnd();
                this.updateUI();
                break;
            case ' ':
                e.preventDefault();
                this.toggleAutoEngine();
                break;
            case 'z':
                if (e.ctrlKey || e.metaKey) {
                    e.preventDefault();
                    this.undoMove();
                }
                break;
        }
    }

    // ============================================================================
    // PGN EXPORT
    // ============================================================================

    exportPGN() {
        const pgn = this.gameState.generatePGN({
            White: this.settings.playerColor === 'white' ? 'Player' : 'Engine',
            Black: this.settings.playerColor === 'black' ? 'Player' : 'Engine'
        });
        
        const blob = new Blob([pgn], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `chess-game-${new Date().toISOString().split('T')[0]}.pgn`;
        a.click();
        URL.revokeObjectURL(url);
        
        this.showToast('PGN exported!', 'success');
    }

    // ============================================================================
    // HELP
    // ============================================================================

    openHelp() {
        this.elements.helpModal.classList.remove('hidden');
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    isPlayerTurn() {
        return this.gameState.sideToMove === this.settings.playerColor;
    }

    isEngineTurn() {
        return !this.isPlayerTurn();
    }

    isPlayerPiece(piece) {
        const isWhitePiece = piece === piece.toUpperCase();
        return (this.settings.playerColor === 'white' && isWhitePiece) ||
               (this.settings.playerColor === 'black' && !isWhitePiece);
    }

    formatEval(score) {
        if (score === null || score === undefined) return '0.0';
        const cp = score / 100;
        return (cp > 0 ? '+' : '') + cp.toFixed(1);
    }

    setAppState(state) {
        this.appState = state;
        
        // Disable controls during thinking
        const disable = state === 'thinking' || state === 'loading';
        this.elements.newGameBtn.disabled = disable;
        this.elements.undoBtn.disabled = disable || !this.gameState.canUndo();
        this.elements.hintBtn.disabled = disable;
    }

    setLoading(loading) {
        this.elements.loadingOverlay.classList.toggle('hidden', !loading);
    }

    // ============================================================================
    // ERROR HANDLING
    // ============================================================================

    handleError(error) {
        console.error('Error:', error);
        
        let message = 'An error occurred';
        
        if (error instanceof ApiError) {
            message = error.getUserMessage();
        } else if (error.message) {
            message = error.message;
        }
        
        this.showError(message);
    }

    showError(message) {
        this.elements.errorMessage.textContent = message;
        this.elements.errorToast.classList.remove('hidden');
        
        setTimeout(() => {
            this.elements.errorToast.classList.add('hidden');
        }, 5000);
    }

    showToast(message, type = 'info') {
        // Simple toast implementation
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <span class="toast-icon">${type === 'success' ? '✓' : 'ℹ'}</span>
            <span class="toast-message">${message}</span>
        `;
        document.body.appendChild(toast);
        
        setTimeout(() => {
            toast.remove();
        }, 3000);
    }

    setupErrorHandling() {
        window.onerror = (msg, url, line, col, error) => {
            console.error('Global error:', { msg, url, line, col, error });
            return false;
        };
    }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.chessApp = new ChessApp();
});
