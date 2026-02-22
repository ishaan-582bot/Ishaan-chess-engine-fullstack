/**
 * ChessBoard - Interactive Chess Board Component
 * 
 * Features:
 * - Pure JavaScript (no external dependencies)
 * - Drag and drop piece movement
 * - Click-to-move support
 * - Square highlighting (selected, legal moves, last move, check)
 * - Arrow drawing for hints
 * - Responsive design
 * - Unicode piece rendering
 * 
 * @author Chess Frontend Developer
 * @version 1.0.0
 */

class ChessBoard {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) {
            throw new Error(`Container element with id '${containerId}' not found`);
        }

        this.options = {
            onMove: options.onMove || (() => {}),
            onSquareClick: options.onSquareClick || (() => {}),
            onDragStart: options.onDragStart || (() => true),
            onDragEnd: options.onDragEnd || (() => {}),
            confirmMoves: options.confirmMoves || false,
            showCoordinates: options.showCoordinates !== false,
            flipped: false
        };

        this.squares = {};
        this.pieces = {};
        this.selectedSquare = null;
        this.draggedPiece = null;
        this.dragOffset = { x: 0, y: 0 };
        this.lastMove = null;
        this.flipped = this.options.flipped;

        this.init();
    }

    // ============================================================================
    // INITIALIZATION
    // ============================================================================

    init() {
        this.createBoard();
        this.attachEventListeners();
        this.setPosition('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1');
    }

    /**
     * Create the board HTML structure
     */
    createBoard() {
        this.container.innerHTML = '';
        this.container.className = 'chessboard';
        
        // Create 8x8 grid
        for (let row = 0; row < 8; row++) {
            for (let col = 0; col < 8; col++) {
                const square = document.createElement('div');
                const file = String.fromCharCode(97 + col); // a-h
                const rank = 8 - row; // 8-1
                const squareName = file + rank;
                
                square.className = `square ${(row + col) % 2 === 0 ? 'light' : 'dark'}`;
                square.dataset.square = squareName;
                
                // Add coordinates
                if (this.options.showCoordinates) {
                    if (col === 0) {
                        const coord = document.createElement('span');
                        coord.className = 'coordinate rank';
                        coord.textContent = rank;
                        square.appendChild(coord);
                    }
                    if (row === 7) {
                        const coord = document.createElement('span');
                        coord.className = 'coordinate file';
                        coord.textContent = file;
                        square.appendChild(coord);
                    }
                }
                
                this.container.appendChild(square);
                this.squares[squareName] = square;
            }
        }
    }

    /**
     * Attach event listeners for drag and drop
     */
    attachEventListeners() {
        // Mouse events
        this.container.addEventListener('mousedown', this.handleMouseDown.bind(this));
        document.addEventListener('mousemove', this.handleMouseMove.bind(this));
        document.addEventListener('mouseup', this.handleMouseUp.bind(this));
        
        // Touch events
        this.container.addEventListener('touchstart', this.handleTouchStart.bind(this), { passive: false });
        document.addEventListener('touchmove', this.handleTouchMove.bind(this), { passive: false });
        document.addEventListener('touchend', this.handleTouchEnd.bind(this));
        
        // Prevent context menu on pieces
        this.container.addEventListener('contextmenu', (e) => {
            if (e.target.classList.contains('piece')) {
                e.preventDefault();
            }
        });
    }

    // ============================================================================
    // POSITION MANAGEMENT
    // ============================================================================

    /**
     * Set board position from FEN string
     * @param {string} fen - FEN string
     */
    setPosition(fen) {
        // Clear existing pieces
        this.clearPieces();
        
        // Parse FEN position part
        const position = fen.split(' ')[0];
        const ranks = position.split('/');
        
        for (let row = 0; row < 8; row++) {
            let col = 0;
            for (const char of ranks[row]) {
                if (/\d/.test(char)) {
                    col += parseInt(char);
                } else {
                    const file = String.fromCharCode(97 + col);
                    const rank = 8 - row;
                    const square = file + rank;
                    this.placePiece(square, char);
                    col++;
                }
            }
        }
    }

    /**
     * Clear all pieces from the board
     */
    clearPieces() {
        Object.values(this.pieces).forEach(piece => piece.remove());
        this.pieces = {};
    }

    /**
     * Place a piece on a square
     * @param {string} square - Square name (e.g., 'e4')
     * @param {string} pieceCode - Piece code (e.g., 'P', 'n', 'Q')
     */
    placePiece(square, pieceCode) {
        const piece = document.createElement('div');
        piece.className = `piece ${pieceCode === pieceCode.toUpperCase() ? 'white' : 'black'}`;
        piece.textContent = this.getPieceUnicode(pieceCode);
        piece.dataset.piece = pieceCode;
        piece.dataset.square = square;
        
        this.squares[square].appendChild(piece);
        this.pieces[square] = piece;
    }

    /**
     * Get Unicode character for a piece
     * @param {string} pieceCode - Piece code
     * @returns {string} Unicode character
     */
    getPieceUnicode(pieceCode) {
        const pieces = {
            'K': '♔', 'Q': '♕', 'R': '♖', 'B': '♗', 'N': '♘', 'P': '♙',
            'k': '♚', 'q': '♛', 'r': '♜', 'b': '♝', 'n': '♞', 'p': '♟'
        };
        return pieces[pieceCode] || '';
    }

    /**
     * Get piece at a square
     * @param {string} square - Square name
     * @returns {string|null} Piece code or null
     */
    getPieceAt(square) {
        const piece = this.pieces[square];
        return piece ? piece.dataset.piece : null;
    }

    // ============================================================================
    // DRAG AND DROP
    // ============================================================================

    handleMouseDown(e) {
        if (e.button !== 0) return; // Only left click
        
        const piece = e.target.closest('.piece');
        if (!piece) {
            // Clicked on empty square
            const square = e.target.closest('.square');
            if (square) {
                this.handleSquareClick(square.dataset.square);
            }
            return;
        }
        
        const square = piece.dataset.square;
        
        // Check if drag is allowed
        if (!this.options.onDragStart(square)) return;
        
        this.startDrag(piece, e.clientX, e.clientY);
    }

    handleTouchStart(e) {
        const piece = e.target.closest('.piece');
        if (!piece) {
            const square = e.target.closest('.square');
            if (square) {
                this.handleSquareClick(square.dataset.square);
            }
            return;
        }
        
        const square = piece.dataset.square;
        if (!this.options.onDragStart(square)) return;
        
        const touch = e.touches[0];
        this.startDrag(piece, touch.clientX, touch.clientY);
        e.preventDefault();
    }

    startDrag(piece, clientX, clientY) {
        this.draggedPiece = piece;
        const rect = piece.getBoundingClientRect();
        
        this.dragOffset = {
            x: clientX - rect.left,
            y: clientY - rect.top
        };
        
        piece.classList.add('dragging');
        piece.style.position = 'fixed';
        piece.style.left = (clientX - this.dragOffset.x) + 'px';
        piece.style.top = (clientY - this.dragOffset.y) + 'px';
        piece.style.zIndex = '1000';
        piece.style.pointerEvents = 'none';
        
        this.selectSquare(piece.dataset.square);
    }

    handleMouseMove(e) {
        if (!this.draggedPiece) return;
        this.updateDragPosition(e.clientX, e.clientY);
    }

    handleTouchMove(e) {
        if (!this.draggedPiece) return;
        const touch = e.touches[0];
        this.updateDragPosition(touch.clientX, touch.clientY);
        e.preventDefault();
    }

    updateDragPosition(clientX, clientY) {
        if (!this.draggedPiece) return;
        
        this.draggedPiece.style.left = (clientX - this.dragOffset.x) + 'px';
        this.draggedPiece.style.top = (clientY - this.dragOffset.y) + 'px';
    }

    handleMouseUp(e) {
        if (!this.draggedPiece) return;
        this.endDrag(e.clientX, e.clientY);
    }

    handleTouchEnd(e) {
        if (!this.draggedPiece) return;
        
        // Get last touch position
        const touch = e.changedTouches[0];
        this.endDrag(touch.clientX, touch.clientY);
    }

    endDrag(clientX, clientY) {
        const piece = this.draggedPiece;
        const fromSquare = piece.dataset.square;
        
        // Find target square
        const element = document.elementFromPoint(clientX, clientY);
        const targetSquare = element?.closest('.square')?.dataset.square;
        
        // Reset piece style
        piece.classList.remove('dragging');
        piece.style.position = '';
        piece.style.left = '';
        piece.style.top = '';
        piece.style.zIndex = '';
        piece.style.pointerEvents = '';
        
        this.draggedPiece = null;
        
        // Execute move if valid target
        if (targetSquare && targetSquare !== fromSquare) {
            this.options.onMove(fromSquare, targetSquare);
        }
        
        this.options.onDragEnd();
    }

    // ============================================================================
    // SQUARE INTERACTION
    // ============================================================================

    handleSquareClick(square) {
        // If a piece is selected, try to move
        if (this.selectedSquare && this.selectedSquare !== square) {
            const piece = this.getPieceAt(this.selectedSquare);
            if (piece) {
                this.options.onMove(this.selectedSquare, square);
                this.deselectSquare();
                return;
            }
        }
        
        // Select the square
        this.selectSquare(square);
        this.options.onSquareClick(square);
    }

    selectSquare(square) {
        this.deselectSquare();
        this.selectedSquare = square;
        if (this.squares[square]) {
            this.squares[square].classList.add('selected');
        }
    }

    deselectSquare() {
        if (this.selectedSquare && this.squares[this.selectedSquare]) {
            this.squares[this.selectedSquare].classList.remove('selected');
        }
        this.selectedSquare = null;
    }

    // ============================================================================
    // HIGHLIGHTING
    // ============================================================================

    /**
     * Highlight legal move destinations
     * @param {string[]} squares - Array of square names
     */
    highlightLegalMoves(squares) {
        this.clearLegalMoves();
        squares.forEach(square => {
            if (this.squares[square]) {
                this.squares[square].classList.add('legal-move');
            }
        });
    }

    clearLegalMoves() {
        Object.values(this.squares).forEach(square => {
            square.classList.remove('legal-move');
        });
    }

    /**
     * Highlight last move
     * @param {string} from - Source square
     * @param {string} to - Destination square
     */
    highlightLastMove(from, to) {
        this.clearLastMove();
        if (this.squares[from]) this.squares[from].classList.add('last-move');
        if (this.squares[to]) this.squares[to].classList.add('last-move');
        this.lastMove = { from, to };
    }

    clearLastMove() {
        if (this.lastMove) {
            if (this.squares[this.lastMove.from]) {
                this.squares[this.lastMove.from].classList.remove('last-move');
            }
            if (this.squares[this.lastMove.to]) {
                this.squares[this.lastMove.to].classList.remove('last-move');
            }
        }
        this.lastMove = null;
    }

    /**
     * Highlight king in check
     * @param {string} square - King's square
     */
    highlightCheck(square) {
        this.clearCheck();
        if (this.squares[square]) {
            this.squares[square].classList.add('check');
        }
    }

    clearCheck() {
        Object.values(this.squares).forEach(square => {
            square.classList.remove('check');
        });
    }

    /**
     * Highlight hint move
     * @param {string} from - Source square
     * @param {string} to - Destination square
     */
    highlightHint(from, to) {
        this.clearHints();
        if (this.squares[from]) this.squares[from].classList.add('hint-from');
        if (this.squares[to]) this.squares[to].classList.add('hint-to');
    }

    clearHints() {
        Object.values(this.squares).forEach(square => {
            square.classList.remove('hint-from', 'hint-to');
        });
    }

    // ============================================================================
    // ARROW DRAWING
    // ============================================================================

    /**
     * Draw an arrow between two squares
     * @param {string} from - Source square
     * @param {string} to - Destination square
     */
    drawArrow(from, to) {
        const arrowOverlay = document.getElementById('arrow-overlay');
        if (!arrowOverlay) return;
        
        const fromSquare = this.squares[from];
        const toSquare = this.squares[to];
        if (!fromSquare || !toSquare) return;
        
        const fromRect = fromSquare.getBoundingClientRect();
        const toRect = toSquare.getBoundingClientRect();
        const containerRect = this.container.getBoundingClientRect();
        
        const fromX = fromRect.left - containerRect.left + fromRect.width / 2;
        const fromY = fromRect.top - containerRect.top + fromRect.height / 2;
        const toX = toRect.left - containerRect.left + toRect.width / 2;
        const toY = toRect.top - containerRect.top + toRect.height / 2;
        
        const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        arrow.classList.add('arrow');
        arrow.style.position = 'absolute';
        arrow.style.top = '0';
        arrow.style.left = '0';
        arrow.style.width = '100%';
        arrow.style.height = '100%';
        arrow.style.pointerEvents = 'none';
        arrow.style.zIndex = '50';
        
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', fromX);
        line.setAttribute('y1', fromY);
        line.setAttribute('x2', toX);
        line.setAttribute('y2', toY);
        line.setAttribute('stroke', 'rgba(255, 170, 0, 0.8)');
        line.setAttribute('stroke-width', '6');
        line.setAttribute('stroke-linecap', 'round');
        
        arrow.appendChild(line);
        arrowOverlay.appendChild(arrow);
    }

    clearArrows() {
        const arrowOverlay = document.getElementById('arrow-overlay');
        if (arrowOverlay) {
            arrowOverlay.innerHTML = '';
        }
    }

    // ============================================================================
    // ANIMATIONS
    // ============================================================================

    /**
     * Animate an illegal move attempt
     * @param {string} square - Square to animate
     */
    animateIllegalMove(square) {
        const piece = this.pieces[square];
        if (!piece) return;
        
        piece.classList.add('illegal-move');
        setTimeout(() => {
            piece.classList.remove('illegal-move');
        }, 300);
    }

    // ============================================================================
    // BOARD CONTROLS
    // ============================================================================

    /**
     * Flip the board
     */
    flip() {
        this.flipped = !this.flipped;
        this.container.classList.toggle('flipped', this.flipped);
        
        // Re-render pieces
        const fen = this.getCurrentFen();
        this.setPosition(fen);
    }

    /**
     * Get current position as FEN (simplified)
     * @returns {string} FEN string
     */
    getCurrentFen() {
        let fen = '';
        
        for (let row = 0; row < 8; row++) {
            let emptyCount = 0;
            for (let col = 0; col < 8; col++) {
                const file = String.fromCharCode(97 + col);
                const rank = 8 - row;
                const square = file + rank;
                const piece = this.getPieceAt(square);
                
                if (piece) {
                    if (emptyCount > 0) {
                        fen += emptyCount;
                        emptyCount = 0;
                    }
                    fen += piece;
                } else {
                    emptyCount++;
                }
            }
            if (emptyCount > 0) {
                fen += emptyCount;
            }
            if (row < 7) fen += '/';
        }
        
        return fen;
    }
}

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { ChessBoard };
}
