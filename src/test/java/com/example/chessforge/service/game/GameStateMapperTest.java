package com.example.chessforge.service.game;

import com.example.chessforge.service.game.dto.GameStateResponse;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.game.GameTermination;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStateMapperTest {

    @Mock
    private Game game;

    @Mock
    private User whitePlayer;

    @Mock
    private User blackPlayer;

    @Mock
    private User drawOfferBy;

    private GameStateMapper mapper;

    @BeforeEach
    void setUp() {

        mapper =
                new GameStateMapper();
    }

    @Test
    void shouldMapGameToResponse() {

        TimeControl timeControl =
                TimeControl.values()[0];

        LocalDateTime turnStartedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        0
                );

        LocalDateTime turnExpiresAt =
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        5
                );

        when(game.getId())
                .thenReturn(10L);

        when(game.getWhitePlayer())
                .thenReturn(whitePlayer);

        when(game.getBlackPlayer())
                .thenReturn(blackPlayer);

        when(whitePlayer.getId())
                .thenReturn(1L);

        when(whitePlayer.getUsername())
                .thenReturn("white");

        when(blackPlayer.getId())
                .thenReturn(2L);

        when(blackPlayer.getUsername())
                .thenReturn("black");

        when(game.getTimeControl())
                .thenReturn(timeControl);

        when(game.isRated())
                .thenReturn(true);

        when(game.getStatus())
                .thenReturn(GameStatus.FINISHED);

        when(game.getResult())
                .thenReturn(GameResult.WHITE_WIN);

        when(game.getTermination())
                .thenReturn(GameTermination.CHECKMATE);

        when(game.getCurrentFen())
                .thenReturn("test-fen");

        when(game.getWhiteTimeRemainingMillis())
                .thenReturn(100_000L);

        when(game.getBlackTimeRemainingMillis())
                .thenReturn(80_000L);

        when(game.getTurnStartedAt())
                .thenReturn(turnStartedAt);

        when(game.getTurnExpiresAt())
                .thenReturn(turnExpiresAt);

        when(game.getDrawOfferBy())
                .thenReturn(drawOfferBy);

        when(drawOfferBy.getId())
                .thenReturn(2L);

        when(game.getPgn())
                .thenReturn(
                        "1. e4 e5 2. Nf3 1-0"
                );

        GameStateResponse response =
                mapper.toResponse(
                        game
                );

        assertEquals(
                10L,
                response.gameId()
        );

        assertEquals(
                1L,
                response.whitePlayerId()
        );

        assertEquals(
                "white",
                response.whiteUsername()
        );

        assertEquals(
                2L,
                response.blackPlayerId()
        );

        assertEquals(
                "black",
                response.blackUsername()
        );

        assertEquals(
                timeControl,
                response.timeControl()
        );

        assertTrue(
                response.rated()
        );

        assertEquals(
                GameStatus.FINISHED,
                response.status()
        );

        assertEquals(
                GameResult.WHITE_WIN,
                response.result()
        );

        assertEquals(
                GameTermination.CHECKMATE,
                response.termination()
        );

        assertEquals(
                "test-fen",
                response.currentFen()
        );

        assertEquals(
                100_000L,
                response.whiteTimeRemainingMillis()
        );

        assertEquals(
                80_000L,
                response.blackTimeRemainingMillis()
        );

        assertEquals(
                turnStartedAt,
                response.turnStartedAt()
        );

        assertEquals(
                turnExpiresAt,
                response.turnExpiresAt()
        );

        assertEquals(
                2L,
                response.drawOfferByUserId()
        );

        assertEquals(
                "1. e4 e5 2. Nf3 1-0",
                response.pgn()
        );
    }

    @Test
    void shouldMapMissingDrawOfferToNull() {

        when(game.getWhitePlayer())
                .thenReturn(whitePlayer);

        when(game.getBlackPlayer())
                .thenReturn(blackPlayer);

        when(game.getDrawOfferBy())
                .thenReturn(null);

        GameStateResponse response =
                mapper.toResponse(
                        game
                );

        assertNull(
                response.drawOfferByUserId()
        );
    }
}