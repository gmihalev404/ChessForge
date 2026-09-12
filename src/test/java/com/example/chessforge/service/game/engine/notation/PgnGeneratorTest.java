package com.example.chessforge.service.game.engine.notation;

import com.example.chessforge.model.entity.game.GameMove;
import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.service.game.notation.PgnGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PgnGeneratorTest {

    private PgnGenerator pgnGenerator;

    @BeforeEach
    void setUp() {

        pgnGenerator =
                new PgnGenerator();
    }

    // =========================================================
    // MOVES
    // =========================================================

    @Test
    void shouldGeneratePgnForCompleteMovePairs() {

        List<GameMove> moves =
                List.of(
                        move(1, "e4"),
                        move(2, "e5"),
                        move(3, "Nf3"),
                        move(4, "Nc6"),
                        move(5, "Bb5"),
                        move(6, "a6")
                );

        String pgn =
                pgnGenerator.generate(
                        moves,
                        GameResult.WHITE_WIN
                );

        assertEquals(
                "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 1-0",
                pgn
        );
    }

    @Test
    void shouldGeneratePgnWhenGameEndsAfterWhiteMove() {

        List<GameMove> moves =
                List.of(
                        move(1, "e4"),
                        move(2, "e5"),
                        move(3, "Qh5"),
                        move(4, "Nc6"),
                        move(5, "Bc4"),
                        move(6, "Nf6"),
                        move(7, "Qxf7#")
                );

        String pgn =
                pgnGenerator.generate(
                        moves,
                        GameResult.WHITE_WIN
                );

        assertEquals(
                "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
                pgn
        );
    }

    // =========================================================
    // RESULTS
    // =========================================================

    @Test
    void shouldGenerateWhiteWinResult() {

        assertEquals(
                "1. e4 1-0",
                pgnGenerator.generate(
                        List.of(
                                move(1, "e4")
                        ),
                        GameResult.WHITE_WIN
                )
        );
    }

    @Test
    void shouldGenerateBlackWinResult() {

        assertEquals(
                "1. e4 e5 0-1",
                pgnGenerator.generate(
                        List.of(
                                move(1, "e4"),
                                move(2, "e5")
                        ),
                        GameResult.BLACK_WIN
                )
        );
    }

    @Test
    void shouldGenerateDrawResult() {

        assertEquals(
                "1. e4 e5 1/2-1/2",
                pgnGenerator.generate(
                        List.of(
                                move(1, "e4"),
                                move(2, "e5")
                        ),
                        GameResult.DRAW
                )
        );
    }

    @Test
    void shouldGenerateResultForGameWithoutMoves() {

        assertEquals(
                "1/2-1/2",
                pgnGenerator.generate(
                        List.of(),
                        GameResult.DRAW
                )
        );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void shouldRejectNullMoves() {

        assertThrows(
                NullPointerException.class,
                () ->
                        pgnGenerator.generate(
                                null,
                                GameResult.WHITE_WIN
                        )
        );
    }

    @Test
    void shouldRejectNullResult() {

        assertThrows(
                NullPointerException.class,
                () ->
                        pgnGenerator.generate(
                                List.of(),
                                null
                        )
        );
    }

    @Test
    void shouldRejectMoveWithoutSan() {

        GameMove move =
                GameMove.builder()
                        .plyNumber(1)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        pgnGenerator.generate(
                                List.of(move),
                                GameResult.WHITE_WIN
                        )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private GameMove move(
            int plyNumber,
            String san
    ) {

        return GameMove.builder()
                .plyNumber(plyNumber)
                .san(san)
                .build();
    }
}