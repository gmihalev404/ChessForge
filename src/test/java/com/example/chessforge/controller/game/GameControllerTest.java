package com.example.chessforge.controller.game;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GameSummaryResponse;
import com.example.chessforge.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private User user;

    private GameController controller;

    @BeforeEach
    void setUp() {

        controller =
                new GameController(
                        gameService,
                        userService
                );
    }

    @Test
    void gamesShouldLoadAuthenticatedUsersGames() {

        Principal principal =
                () -> "testplayer";

        List<GameSummaryResponse> games =
                List.of();

        when(userService.findByUsername(
                "testplayer"
        )).thenReturn(
                Optional.of(user)
        );

        when(user.getUsername())
                .thenReturn(
                        "testplayer"
                );

        when(gameService.getGameSummariesForUser(
                user
        )).thenReturn(
                games
        );

        String result =
                controller.games(
                        principal,
                        model
                );

        assertEquals(
                "game/games",
                result
        );

        verify(model)
                .addAttribute(
                        "games",
                        games
                );

        verify(model)
                .addAttribute(
                        "username",
                        "testplayer"
                );
    }
}