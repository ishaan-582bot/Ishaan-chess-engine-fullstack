# Chess Engine Project

A complete chess engine implementation with a Spring Boot REST API backend and a vanilla JavaScript frontend.

## Project Structure

```
chess-engine-project/
├── chess-api/              # Spring Boot Backend
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/chess/api/
│       │   │   ├── ChessApiApplication.java
│       │   │   ├── config/
│       │   │   │   ├── CorsConfig.java
│       │   │   │   └── OpenApiConfig.java
│       │   │   ├── controller/
│       │   │   │   ├── EngineController.java
│       │   │   │   ├── GameController.java
│       │   │   │   ├── HealthController.java
│       │   │   │   └── PositionController.java
│       │   │   ├── dto/
│       │   │   │   ├── EngineMoveRequest.java
│       │   │   │   ├── EngineMoveResponse.java
│       │   │   │   ├── ErrorResponse.java
│       │   │   │   ├── EvaluationResponse.java
│       │   │   │   ├── LegalMovesResponse.java
│       │   │   │   ├── MoveRequest.java
│       │   │   │   ├── MoveResponse.java
│       │   │   │   ├── NewGameRequest.java
│       │   │   │   └── NewGameResponse.java
│       │   │   ├── engine/
│       │   │   │   └── SimpleChessEngine.java
│       │   │   ├── exception/
│       │   │   │   ├── ChessApiException.java
│       │   │   │   ├── GameLimitExceededException.java
│       │   │   │   ├── GameNotFoundException.java
│       │   │   │   ├── GameOverException.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   ├── InvalidFenException.java
│       │   │   │   └── InvalidMoveException.java
│       │   │   └── service/
│       │   │       ├── ChessEngineService.java
│       │   │       ├── GameState.java
│       │   │       └── GameStateManager.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           └── java/com/chess/api/
│               ├── ChessApiApplicationTests.java
│               └── controller/GameControllerTest.java
│
└── chess-frontend/         # Vanilla JavaScript Frontend
    ├── index.html
    ├── styles.css
    └── js/
        ├── ChessApiClient.js
        ├── AudioManager.js
        ├── ChessBoard.js
        ├── GameState.js
        └── app.js
```

## Features

### Backend (Spring Boot)

- **Complete Chess Engine**: Alpha-beta search with transposition tables
- **REST API**: Full game management, move execution, and position analysis
- **Move Generation**: Legal move validation with FEN support
- **Position Evaluation**: Piece-square tables and material counting
- **Game State Management**: Automatic cleanup of expired games
- **CORS Configuration**: Ready for frontend integration
- **API Documentation**: Swagger UI at `/api/swagger-ui.html`

### Frontend (Vanilla JavaScript)

- **Interactive Board**: Drag-and-drop piece movement
- **Game Controls**: New game, undo, flip board, hints
- **Engine Integration**: Auto-play mode with adjustable depth
- **Move History**: Navigate through game history
- **Sound Effects**: Web Audio API synthesis
- **Responsive Design**: Works on desktop and mobile
- **Keyboard Shortcuts**: Arrow keys, Ctrl+Z, Space

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Modern web browser

### Backend Setup

1. Navigate to the backend directory:
```bash
cd chess-api
```

2. Build and run:
```bash
mvn clean package
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api`

API Documentation: `http://localhost:8080/api/swagger-ui.html`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd chess-frontend
```

2. Serve the files using any static server:
```bash
# Using Python 3
python -m http.server 3000

# Using Node.js npx
npx serve .

# Using PHP
php -S localhost:3000
```

3. Open `http://localhost:3000` in your browser

## API Endpoints

### Game Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/game/new` | Create a new game |
| POST | `/api/game/move` | Make a move |
| GET | `/api/game/state` | Get game state |

### Engine

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/engine/move` | Get engine's best move |

### Position Analysis

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/position/legal-moves` | Get legal moves for position |
| GET | `/api/position/eval` | Evaluate position |
| GET | `/api/position/validate` | Validate FEN string |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |

## Configuration

### Backend (`application.properties`)

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# CORS
chess.cors.allowed-origins=http://localhost:3000,http://localhost:5173

# Engine
chess.engine.default-depth=5
chess.engine.max-depth=10

# Game Management
chess.game.timeout-minutes=30
chess.game.max-concurrent=1000
```

### Frontend

Edit `js/app.js` to configure the API base URL:

```javascript
this.apiBaseUrl = 'http://localhost:8080/api';
```

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| ← → | Navigate move history |
| ↑ | Go to start |
| ↓ | Go to end |
| Space | Toggle auto-engine |
| Ctrl+Z | Undo last move |

## Engine Strength

The engine uses:
- Alpha-beta pruning with quiescence search
- Transposition table for move ordering
- Piece-square tables for evaluation
- MVV-LVA move ordering

Estimated ELO: ~1200-1400 depending on depth setting

## Browser Compatibility

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## License

MIT License

## Credits

Built with:
- Spring Boot 3.2.0
- Java 17
- Vanilla JavaScript (ES6+)
- CSS3 with custom properties
