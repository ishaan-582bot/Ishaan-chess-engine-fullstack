package com.chess.api.controller;

import com.chess.api.dto.NewGameRequest;
import com.chess.api.dto.NewGameResponse;
import com.chess.api.service.ChessEngineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Game Controller Tests
 * 
 * Unit tests for GameController endpoints.
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChessEngineService chessEngineService;

    @Test
    void createNewGame_ShouldReturnCreated() throws Exception {
        // Given
        NewGameResponse response = NewGameResponse.builder()
                .gameId("game-test123")
                .fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                .sideToMove("white")
                .legalMoves(Arrays.asList("a2a3", "a2a4", "b2b3", "b2b4"))
                .build();

        when(chessEngineService.createNewGame(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/game/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(NewGameRequest.builder().build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("game-test123"))
                .andExpect(jsonPath("$.sideToMove").value("white"));
    }

    @Test
    void createNewGame_WithNullRequest_ShouldUseDefaults() throws Exception {
        // Given
        NewGameResponse response = NewGameResponse.builder()
                .gameId("game-test456")
                .fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                .sideToMove("white")
                .legalMoves(Arrays.asList("e2e4", "d2d4"))
                .build();

        when(chessEngineService.createNewGame(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/game/new")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").exists());
    }

    @Test
    void getGameState_ShouldReturnOk() throws Exception {
        // Given
        NewGameResponse response = NewGameResponse.builder()
                .gameId("game-test789")
                .fen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
                .sideToMove("black")
                .legalMoves(Arrays.asList("e7e5", "e7e6", "d7d5"))
                .build();

        when(chessEngineService.getGameState("game-test789")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/game/state")
                        .param("gameId", "game-test789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-test789"))
                .andExpect(jsonPath("$.sideToMove").value("black"));
    }
}
