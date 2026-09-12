package com.example.chessforge.service.game;

import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.service.game.dto.GameSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSummaryMapperTest {

    private GameSummaryMapper mapper;

    @BeforeEach
    void setUp() {

        mapper =
                new GameSummaryMapper();
    }

    @Test
    void toResponseShouldMapGameWhenUserIsWhite() {

        User white =
                createUser(
                        1L,
                        "white"
                );

        User black =
                createUser(
                        2L,
                        "black"
                );

        Game game =
                Game.builder()
                        .whitePlayer(white)
                        .blackPlayer(black)
                        .timeControl(
                                TimeControl.values()[0]
                        )
                        .rated(true)
                        .status(
                                GameStatus.IN_PROGRESS
                        )
                        .build();

        setId(game, 10L);

        GameSummaryResponse result =
                mapper.toResponse(
                        game,
                        white
                );

        assertEquals(
                10L,
                result.gameId()
        );

        assertEquals(
                2L,
                result.opponentId()
        );

        assertEquals(
                "black",
                result.opponentUsername()
        );

        assertTrue(
                result.white()
        );

        assertTrue(
                result.rated()
        );

        assertEquals(
                GameStatus.IN_PROGRESS,
                result.status()
        );
    }

    @Test
    void toResponseShouldMapGameWhenUserIsBlack() {

        User white =
                createUser(
                        1L,
                        "white"
                );

        User black =
                createUser(
                        2L,
                        "black"
                );

        Game game =
                Game.builder()
                        .whitePlayer(white)
                        .blackPlayer(black)
                        .timeControl(
                                TimeControl.values()[0]
                        )
                        .rated(false)
                        .status(
                                GameStatus.WAITING
                        )
                        .build();

        setId(game, 20L);

        GameSummaryResponse result =
                mapper.toResponse(
                        game,
                        black
                );

        assertEquals(
                20L,
                result.gameId()
        );

        assertEquals(
                1L,
                result.opponentId()
        );

        assertEquals(
                "white",
                result.opponentUsername()
        );

        assertFalse(
                result.white()
        );

        assertFalse(
                result.rated()
        );
    }

    private User createUser(
            Long id,
            String username
    ) {

        User user =
                User.builder()
                        .username(username)
                        .build();

        setId(
                user,
                id
        );

        return user;
    }
    private void setId(
            BaseEntity entity,
            Long id
    ) {

        try {

            var field =
                    BaseEntity.class
                            .getDeclaredField(
                                    "id"
                            );

            field.setAccessible(
                    true
            );

            field.set(
                    entity,
                    id
            );

        } catch (ReflectiveOperationException exception) {

            throw new RuntimeException(
                    exception
            );
        }
    }
}