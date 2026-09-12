package com.example.chessforge.controller.game;

import com.example.chessforge.controller.game.dto.MoveRequest;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GameStateResponse;
import com.example.chessforge.service.game.engine.model.PieceType;
import com.example.chessforge.service.game.realtime.GameRealtimePublisher;
import com.example.chessforge.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private UserService userService;

    @Mock
    private GameRealtimePublisher realtimePublisher;

    @Mock
    private User player;

    @Mock
    private GameStateResponse response;

    private GameWebSocketController controller;

    @BeforeEach
    void setUp() {

        controller =
                new GameWebSocketController(
                        gameService,
                        userService,
                        realtimePublisher
                );
    }

    @Test
    void makeMoveShouldProcessMoveAndBroadcastState() {

        Long gameId =
                10L;

        Principal principal =
                () -> "challenger";

        MoveRequest request =
                new MoveRequest(
                        "e7",
                        "e8",
                        PieceType.QUEEN
                );

        when(userService.findByUsername(
                "challenger"
        )).thenReturn(
                Optional.of(player)
        );

        when(gameService.makeMoveAndGetState(
                gameId,
                player,
                "e7",
                "e8",
                PieceType.QUEEN
        )).thenReturn(
                response
        );

        controller.makeMove(
                gameId,
                request,
                principal
        );

        verify(userService)
                .findByUsername(
                        "challenger"
                );

        verify(gameService)
                .makeMoveAndGetState(
                        gameId,
                        player,
                        "e7",
                        "e8",
                        PieceType.QUEEN
                );

        verify(realtimePublisher)
                .publish(
                        gameId,
                        response
                );
    }

    @Test
    void makeMoveShouldRejectMissingPrincipal() {

        MoveRequest request =
                new MoveRequest(
                        "e2",
                        "e4",
                        null
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        controller.makeMove(
                                10L,
                                request,
                                null
                        )
        );

        verifyNoInteractions(
                userService,
                gameService,
                realtimePublisher
        );
    }

    @Test
    void makeMoveShouldRejectMissingRequest() {

        Principal principal =
                () -> "challenger";

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        controller.makeMove(
                                10L,
                                null,
                                principal
                        )
        );

        verifyNoInteractions(
                userService,
                gameService,
                realtimePublisher
        );
    }

    @Test
    void makeMoveShouldRejectUnknownAuthenticatedUser() {

        Principal principal =
                () -> "unknown";

        MoveRequest request =
                new MoveRequest(
                        "e2",
                        "e4",
                        null
                );

        when(userService.findByUsername(
                "unknown"
        )).thenReturn(
                Optional.empty()
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        controller.makeMove(
                                10L,
                                request,
                                principal
                        )
        );

        verify(gameService, never())
                .makeMoveAndGetState(
                        anyLong(),
                        any(),
                        anyString(),
                        anyString(),
                        any()
                );

        verifyNoInteractions(
                realtimePublisher
        );
    }
}