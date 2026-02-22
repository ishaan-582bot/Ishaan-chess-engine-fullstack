/**
 * ChessApiClient - REST API Client for Chess Engine Backend
 * 
 * Handles all communication with the Spring Boot backend:
 * - Game creation and management
 * - Move execution
 * - Engine move calculation
 * - Position evaluation
 * - Legal moves retrieval
 * 
 * @author Chess Frontend Developer
 * @version 1.0.0
 */

/**
 * Custom API Error class for better error handling
 */
class ApiError extends Error {
    constructor(message, status, code) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.code = code;
    }

    getUserMessage() {
        switch (this.status) {
            case 400:
                return `Invalid request: ${this.message}`;
            case 404:
                return 'Game not found. Please start a new game.';
            case 409:
                return 'Game is already over.';
            case 429:
                return 'Too many games. Please wait a moment.';
            case 0:
                return 'Cannot connect to server. Please check if the backend is running.';
            default:
                return this.message || 'An unexpected error occurred';
        }
    }
}

class ChessApiClient {
    constructor(baseUrl = 'http://localhost:8080/api') {
        this.baseUrl = baseUrl;
        this.maxRetries = 3;
        this.retryDelay = 1000; // Initial retry delay in ms
    }

    // ============================================================================
    // HTTP REQUEST METHODS
    // ============================================================================

    /**
     * Make an HTTP request with retry logic
     * @param {string} endpoint - API endpoint (relative to baseUrl)
     * @param {Object} options - Fetch options
     * @returns {Promise<Object>} Response data
     */
    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            mode: 'cors',
            credentials: 'same-origin'
        };

        const fetchOptions = { ...defaultOptions, ...options };
        
        if (options.body && typeof options.body === 'object') {
            fetchOptions.body = JSON.stringify(options.body);
        }

        let lastError;
        
        for (let attempt = 0; attempt < this.maxRetries; attempt++) {
            try {
                const response = await fetch(url, fetchOptions);
                
                // Handle CORS errors (status 0)
                if (response.status === 0) {
                    throw new ApiError(
                        'CORS error - server may not be configured for cross-origin requests',
                        0,
                        'CORS_ERROR'
                    );
                }

                // Parse response
                const data = await response.json().catch(() => null);

                if (!response.ok) {
                    throw new ApiError(
                        data?.message || data?.error || `HTTP ${response.status}`,
                        response.status,
                        data?.errorCode || `HTTP_${response.status}`
                    );
                }

                return data;
                
            } catch (error) {
                lastError = error;
                
                // Don't retry on 4xx errors (client errors)
                if (error.status >= 400 && error.status < 500 && error.status !== 0) {
                    throw error;
                }
                
                // Exponential backoff: 1000ms, 2000ms, 4000ms
                if (attempt < this.maxRetries - 1) {
                    const delay = this.retryDelay * Math.pow(2, attempt);
                    await this.sleep(delay);
                }
            }
        }

        throw lastError;
    }

    /**
     * Sleep utility for retry delays
     * @param {number} ms - Milliseconds to sleep
     * @returns {Promise<void>}
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    // ============================================================================
    // GAME ENDPOINTS
    // ============================================================================

    /**
     * Create a new game
     * @param {Object} config - Game configuration
     * @param {string} config.mode - Game mode (human_vs_engine, human_vs_human)
     * @param {string} config.playerColor - Player color (white, black)
     * @param {number} config.engineDepth - Engine search depth (1-10)
     * @param {string} config.fen - Optional starting FEN
     * @returns {Promise<Object>} Game data with gameId, fen, legalMoves
     */
    async createGame(config = {}) {
        return this.request('/game/new', {
            method: 'POST',
            body: config
        });
    }

    /**
     * Make a move in the current game
     * @param {string} gameId - Game ID
     * @param {string} from - Source square (e.g., 'e2')
     * @param {string} to - Destination square (e.g., 'e4')
     * @param {string} promotion - Promotion piece (q, r, b, n) - optional
     * @returns {Promise<Object>} Move result with updated position
     */
    async makeMove(gameId, from, to, promotion = null) {
        return this.request('/game/move', {
            method: 'POST',
            body: {
                gameId,
                from,
                to,
                promotion
            }
        });
    }

    /**
     * Get current game state
     * @param {string} gameId - Game ID
     * @returns {Promise<Object>} Current game state
     */
    async getGameState(gameId) {
        return this.request(`/game/state?gameId=${encodeURIComponent(gameId)}`);
    }

    // ============================================================================
    // ENGINE ENDPOINTS
    // ============================================================================

    /**
     * Get engine's best move
     * @param {string} gameId - Game ID
     * @param {number} depth - Search depth (1-10)
     * @returns {Promise<Object>} Engine move with evaluation
     */
    async getEngineMove(gameId, depth = 5) {
        return this.request('/engine/move', {
            method: 'POST',
            body: {
                gameId,
                depth
            }
        });
    }

    // ============================================================================
    // POSITION ENDPOINTS
    // ============================================================================

    /**
     * Get legal moves for a position
     * @param {string} fen - FEN string
     * @returns {Promise<Object>} Legal moves list
     */
    async getLegalMoves(fen) {
        return this.request(`/position/legal-moves?fen=${encodeURIComponent(fen)}`);
    }

    /**
     * Evaluate a position
     * @param {string} fen - FEN string
     * @returns {Promise<Object>} Position evaluation
     */
    async evaluatePosition(fen) {
        return this.request(`/position/eval?fen=${encodeURIComponent(fen)}`);
    }

    /**
     * Validate a FEN string
     * @param {string} fen - FEN string to validate
     * @returns {Promise<Object>} Validation result
     */
    async validateFen(fen) {
        return this.request(`/position/validate?fen=${encodeURIComponent(fen)}`);
    }

    // ============================================================================
    // HEALTH ENDPOINT
    // ============================================================================

    /**
     * Ping the server to check connectivity
     * @returns {Promise<boolean>} True if server is reachable
     */
    async ping() {
        try {
            const response = await fetch(`${this.baseUrl}/health`, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                mode: 'cors'
            });
            return response.ok;
        } catch (error) {
            return false;
        }
    }

    /**
     * Get server health status
     * @returns {Promise<Object>} Health status
     */
    async getHealth() {
        return this.request('/health');
    }
}

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { ChessApiClient, ApiError };
}
