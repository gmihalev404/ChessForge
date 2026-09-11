package com.example.chessforge.service.game.engine.notation;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenConverterTest {
    private FenConverter fenConverter;

    @BeforeEach
    void setUp() {

        fenConverter =
                new FenConverter();
    }

    @Test
    void initialPositionShouldProduceStandardFen() {

        GameState state =
                GameState.initial();

        assertEquals(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                fenConverter.toFen(state)
        );
    }

    @Test
    void positionAfterE2E4ShouldProduceCorrectFen() {

        GameState state =
                GameState.initial();

        MoveApplier moveApplier =
                new MoveApplier();

        moveApplier.apply(
                state,
                Move.normal(
                        "e2",
                        "e4"
                )
        );

        assertEquals(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                fenConverter.toFen(state)
        );
    }

    @Test
    void shouldParseInitialFen() {

        GameState state =
                fenConverter.fromFen(
                        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
                );

        assertEquals(
                PieceColor.WHITE,
                state.getSideToMove()
        );

        assertEquals(
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                ),
                state.getBoard().getPiece(
                        Square.fromAlgebraic("e1")
                )
        );

        assertEquals(
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
                ),
                state.getBoard().getPiece(
                        Square.fromAlgebraic("e8")
                )
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
    }

    @Test
    void fenRoundTripShouldPreservePosition() {

        String fen =
                "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";

        GameState state =
                fenConverter.fromFen(
                        fen
                );

        assertEquals(
                fen,
                fenConverter.toFen(
                        state
                )
        );
    }

    @Test
    void shouldHandleNoCastlingRights() {

        String fen =
                "8/8/8/8/8/8/4K3/7k w - - 17 42";

        GameState state =
                fenConverter.fromFen(
                        fen
                );

        assertFalse(
                state.isWhiteKingSideCastlingAllowed()
        );

        assertFalse(
                state.isWhiteQueenSideCastlingAllowed()
        );

        assertFalse(
                state.isBlackKingSideCastlingAllowed()
        );

        assertFalse(
                state.isBlackQueenSideCastlingAllowed()
        );

        assertEquals(
                fen,
                fenConverter.toFen(state)
        );
    }

    @Test
    void shouldParseEnPassantTarget() {

        GameState state =
                fenConverter.fromFen(
                        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
                );

        assertEquals(
                Square.fromAlgebraic("e3"),
                state.getEnPassantTarget()
        );
    }

    @Test
    void shouldRejectFenWithWrongNumberOfFields() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fenConverter.fromFen(
                        "8/8/8/8/8/8/8/8 w - -"
                )
        );
    }

    @Test
    void shouldRejectInvalidSideToMove() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fenConverter.fromFen(
                        "8/8/8/8/8/8/8/8 x - - 0 1"
                )
        );
    }

    @Test
    void shouldRejectInvalidBoardRank() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fenConverter.fromFen(
                        "9/8/8/8/8/8/8/8 w - - 0 1"
                )
        );
    }

    @Test
    void shouldRejectInvalidMoveCounters() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fenConverter.fromFen(
                        "8/8/8/8/8/8/8/8 w - - -1 0"
                )
        );
    }
}