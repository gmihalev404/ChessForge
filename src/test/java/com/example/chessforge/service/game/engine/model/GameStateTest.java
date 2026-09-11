package com.example.chessforge.service.game.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void initialStateShouldContainInitialChessState() {

        GameState state =
                GameState.initial();

        assertEquals(
                PieceColor.WHITE,
                state.getSideToMove()
        );

        assertTrue(
                state.isWhiteKingSideCastlingAllowed()
        );

        assertTrue(
                state.isWhiteQueenSideCastlingAllowed()
        );

        assertTrue(
                state.isBlackKingSideCastlingAllowed()
        );

        assertTrue(
                state.isBlackQueenSideCastlingAllowed()
        );

        assertNull(
                state.getEnPassantTarget()
        );

        assertEquals(
                0,
                state.getHalfMoveClock()
        );

        assertEquals(
                1,
                state.getFullMoveNumber()
        );

        assertEquals(
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                ),
                state.getBoard()
                        .getPiece(
                                Square.fromAlgebraic(
                                        "e1"
                                )
                        )
        );
    }

    @Test
    void shouldSwitchSideToMove() {

        GameState state =
                GameState.initial();

        state.switchSideToMove();

        assertEquals(
                PieceColor.BLACK,
                state.getSideToMove()
        );

        state.switchSideToMove();

        assertEquals(
                PieceColor.WHITE,
                state.getSideToMove()
        );
    }

    @Test
    void copiedStateShouldHaveIndependentBoard() {

        GameState original =
                GameState.initial();

        GameState copy =
                original.copy();

        copy.getBoard()
                .clearSquare(
                        Square.fromAlgebraic(
                                "e2"
                        )
                );

        assertTrue(
                copy.getBoard()
                        .isEmpty(
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        )
        );

        assertFalse(
                original.getBoard()
                        .isEmpty(
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        )
        );
    }

    @Test
    void copiedStateShouldPreserveStateValues() {

        GameState state =
                GameState.initial();

        state.setSideToMove(
                PieceColor.BLACK
        );

        state.setWhiteKingSideCastlingAllowed(
                false
        );

        state.setEnPassantTarget(
                Square.fromAlgebraic(
                        "e3"
                )
        );

        state.setHalfMoveClock(7);
        state.setFullMoveNumber(12);

        GameState copy =
                state.copy();

        assertEquals(
                PieceColor.BLACK,
                copy.getSideToMove()
        );

        assertFalse(
                copy.isWhiteKingSideCastlingAllowed()
        );

        assertEquals(
                Square.fromAlgebraic("e3"),
                copy.getEnPassantTarget()
        );

        assertEquals(
                7,
                copy.getHalfMoveClock()
        );

        assertEquals(
                12,
                copy.getFullMoveNumber()
        );
    }

    @Test
    void halfMoveClockShouldIncrementAndReset() {

        GameState state =
                GameState.initial();

        state.incrementHalfMoveClock();
        state.incrementHalfMoveClock();

        assertEquals(
                2,
                state.getHalfMoveClock()
        );

        state.resetHalfMoveClock();

        assertEquals(
                0,
                state.getHalfMoveClock()
        );
    }

    @Test
    void shouldRejectNegativeHalfMoveClock() {

        GameState state =
                GameState.initial();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        state.setHalfMoveClock(
                                -1
                        )
        );
    }

    @Test
    void shouldRejectFullMoveNumberBelowOne() {

        GameState state =
                GameState.initial();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        state.setFullMoveNumber(
                                0
                        )
        );
    }
}