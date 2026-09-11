package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveApplierTest {

    private MoveApplier moveApplier;

    @BeforeEach
    void setUp() {

        moveApplier =
                new MoveApplier();
    }

    @Test
    void doublePawnMoveShouldUpdateGameState() {

        GameState state =
                GameState.initial();

        moveApplier.apply(
                state,
                Move.normal(
                        "e2",
                        "e4"
                )
        );

        assertTrue(
                state.getBoard()
                        .isEmpty(
                                Square.fromAlgebraic("e2")
                        )
        );

        assertEquals(
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                ),
                state.getBoard()
                        .getPiece(
                                Square.fromAlgebraic("e4")
                        )
        );

        assertEquals(
                PieceColor.BLACK,
                state.getSideToMove()
        );

        assertEquals(
                Square.fromAlgebraic("e3"),
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
    void blackMoveShouldIncrementFullMoveNumber() {

        GameState state =
                GameState.initial();

        moveApplier.apply(
                state,
                Move.normal(
                        "e2",
                        "e4"
                )
        );

        moveApplier.apply(
                state,
                Move.normal(
                        "e7",
                        "e5"
                )
        );

        assertEquals(
                PieceColor.WHITE,
                state.getSideToMove()
        );

        assertEquals(
                2,
                state.getFullMoveNumber()
        );

        assertEquals(
                Square.fromAlgebraic("e6"),
                state.getEnPassantTarget()
        );
    }

    @Test
    void nonPawnNonCaptureShouldIncrementHalfMoveClock() {

        GameState state =
                GameState.initial();

        moveApplier.apply(
                state,
                Move.normal(
                        "g1",
                        "f3"
                )
        );

        assertEquals(
                1,
                state.getHalfMoveClock()
        );

        assertNull(
                state.getEnPassantTarget()
        );
    }

    @Test
    void captureShouldResetHalfMoveClock() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("d4"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("d8"),
                new Piece(
                        PieceType.KNIGHT,
                        PieceColor.BLACK
                )
        );

        GameState state =
                new GameState(
                        board,
                        PieceColor.WHITE,
                        false,
                        false,
                        false,
                        false,
                        null,
                        12,
                        1
                );

        moveApplier.apply(
                state,
                Move.normal(
                        "d4",
                        "d8"
                )
        );

        assertEquals(
                0,
                state.getHalfMoveClock()
        );

        assertEquals(
                new Piece(
                        PieceType.ROOK,
                        PieceColor.WHITE
                ),
                board.getPiece(
                        Square.fromAlgebraic("d8")
                )
        );
    }

    @Test
    void promotionShouldReplacePawnWithSelectedPiece() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("e7"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                )
        );

        GameState state =
                new GameState(
                        board,
                        PieceColor.WHITE,
                        false,
                        false,
                        false,
                        false,
                        null,
                        4,
                        1
                );

        moveApplier.apply(
                state,
                Move.promotion(
                        Square.fromAlgebraic("e7"),
                        Square.fromAlgebraic("e8"),
                        PieceType.QUEEN
                )
        );

        assertTrue(
                board.isEmpty(
                        Square.fromAlgebraic("e7")
                )
        );

        assertEquals(
                new Piece(
                        PieceType.QUEEN,
                        PieceColor.WHITE
                ),
                board.getPiece(
                        Square.fromAlgebraic("e8")
                )
        );

        assertEquals(
                0,
                state.getHalfMoveClock()
        );
    }

    @Test
    void enPassantShouldRemoveCapturedPawn() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("e5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("d5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.BLACK
                )
        );

        GameState state =
                new GameState(
                        board,
                        PieceColor.WHITE,
                        false,
                        false,
                        false,
                        false,
                        Square.fromAlgebraic("d6"),
                        0,
                        10
                );

        Move move =
                new Move(
                        Square.fromAlgebraic("e5"),
                        Square.fromAlgebraic("d6"),
                        MoveType.EN_PASSANT,
                        null
                );

        moveApplier.apply(
                state,
                move
        );

        assertTrue(
                board.isEmpty(
                        Square.fromAlgebraic("e5")
                )
        );

        assertTrue(
                board.isEmpty(
                        Square.fromAlgebraic("d5")
                )
        );

        assertEquals(
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                ),
                board.getPiece(
                        Square.fromAlgebraic("d6")
                )
        );

        assertNull(
                state.getEnPassantTarget()
        );
    }

    @Test
    void kingMoveShouldRemoveBothCastlingRights() {

        GameState state =
                GameState.initial();

        /*
         * Clear e2 so the move is structurally possible.
         */
        state.getBoard()
                .clearSquare(
                        Square.fromAlgebraic("e2")
                );

        moveApplier.apply(
                state,
                Move.normal(
                        "e1",
                        "e2"
                )
        );

        assertFalse(
                state.isWhiteKingSideCastlingAllowed()
        );

        assertFalse(
                state.isWhiteQueenSideCastlingAllowed()
        );
    }

    @Test
    void rookMoveShouldRemoveOnlyItsCastlingRight() {

        GameState state =
                GameState.initial();

        state.getBoard()
                .clearSquare(
                        Square.fromAlgebraic("h2")
                );

        moveApplier.apply(
                state,
                Move.normal(
                        "h1",
                        "h2"
                )
        );

        assertFalse(
                state.isWhiteKingSideCastlingAllowed()
        );

        assertTrue(
                state.isWhiteQueenSideCastlingAllowed()
        );
    }

    @Test
    void kingSideCastlingShouldMoveKingAndRook() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("e1"),
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("h1"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.WHITE
                )
        );

        GameState state =
                new GameState(
                        board,
                        PieceColor.WHITE,
                        true,
                        false,
                        false,
                        false,
                        null,
                        0,
                        1
                );

        Move castle =
                new Move(
                        Square.fromAlgebraic("e1"),
                        Square.fromAlgebraic("g1"),
                        MoveType.CASTLE_KING_SIDE,
                        null
                );

        moveApplier.apply(
                state,
                castle
        );

        assertTrue(
                board.isEmpty(
                        Square.fromAlgebraic("e1")
                )
        );

        assertTrue(
                board.isEmpty(
                        Square.fromAlgebraic("h1")
                )
        );

        assertEquals(
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                ),
                board.getPiece(
                        Square.fromAlgebraic("g1")
                )
        );

        assertEquals(
                new Piece(
                        PieceType.ROOK,
                        PieceColor.WHITE
                ),
                board.getPiece(
                        Square.fromAlgebraic("f1")
                )
        );

        assertFalse(
                state.isWhiteKingSideCastlingAllowed()
        );

        assertFalse(
                state.isWhiteQueenSideCastlingAllowed()
        );
    }
}