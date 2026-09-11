package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegalMoveGeneratorTest {

    private LegalMoveGenerator legalMoveGenerator;

    @BeforeEach
    void setUp() {

        legalMoveGenerator =
                new LegalMoveGenerator(
                        new MoveGenerator(),
                        new MoveApplier(),
                        new AttackDetector()
                );
    }

    @Test
    void initialPositionShouldHaveTwentyLegalMoves() {

        GameState state =
                GameState.initial();

        List<Move> moves =
                legalMoveGenerator
                        .generateAllLegalMoves(
                                state
                        );

        assertEquals(
                20,
                moves.size()
        );
    }

    @Test
    void pinnedKnightShouldHaveNoLegalMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e1"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e2"),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e8"),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("a8"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        );

        assertTrue(
                moves.isEmpty()
        );
    }

    @Test
    void pinnedRookShouldMoveOnlyAlongPinLine() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e1"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e2"),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e8"),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("a8"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        );

        assertTrue(
                containsTarget(
                        moves,
                        "e3"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "e7"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "e8"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "d2"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "f2"
                )
        );
    }

    @Test
    void kingShouldNotMoveOntoAttackedSquare() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e1"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e8"),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("a8"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "e1"
                                )
                        );

        assertFalse(
                containsTarget(
                        moves,
                        "e2"
                )
        );
    }

    @Test
    void kingShouldNotMoveAdjacentToEnemyKing() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e2"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        );

        assertFalse(
                containsTarget(
                        moves,
                        "e3"
                )
        );
    }

    @Test
    void onlyMovesThatResolveCheckShouldRemainLegal() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e1"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("g1"),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e8"),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("a8"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "g1"
                                )
                        );

        assertEquals(
                1,
                moves.size()
        );

        assertTrue(
                containsTarget(
                        moves,
                        "e2"
                )
        );
    }

    @Test
    void enPassantShouldBeIllegalWhenItExposesOwnKing() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("e5"),
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("f5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("g5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("h5"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("a8"),
                new Piece(
                        PieceType.KING,
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
                        Square.fromAlgebraic("g6"),
                        0,
                        1
                );

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "f5"
                                )
                        );

        assertFalse(
                moves.stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.EN_PASSANT
                        )
        );
    }

    @Test
    void generatingLegalMovesShouldNotModifyOriginalState() {

        GameState state =
                GameState.initial();

        legalMoveGenerator
                .generateLegalMoves(
                        state,
                        Square.fromAlgebraic(
                                "e2"
                        )
                );

        assertEquals(
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                ),
                state.getBoard()
                        .getPiece(
                                Square.fromAlgebraic("e2")
                        )
        );

        assertTrue(
                state.getBoard()
                        .isEmpty(
                                Square.fromAlgebraic("e4")
                        )
        );

        assertEquals(
                PieceColor.WHITE,
                state.getSideToMove()
        );

        assertNull(
                state.getEnPassantTarget()
        );

        assertEquals(
                1,
                state.getFullMoveNumber()
        );
    }

    @Test
    void kingShouldCastleWhenPathIsSafe() {

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

        board.setPiece(
                Square.fromAlgebraic("a8"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
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

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "e1"
                                )
                        );

        assertTrue(
                moves.stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.CASTLE_KING_SIDE
                        )
        );
    }

    @Test
    void kingShouldNotCastleWhileInCheck() {

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

        board.setPiece(
                Square.fromAlgebraic("e8"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("a8"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
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

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic("e1")
                        );

        assertFalse(
                moves.stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.CASTLE_KING_SIDE
                        )
        );
    }

    @Test
    void kingShouldNotCastleThroughAttackedSquare() {

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

        /*
         * Attacks f1.
         */
        board.setPiece(
                Square.fromAlgebraic("f8"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("a8"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
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

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic("e1")
                        );

        assertFalse(
                moves.stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.CASTLE_KING_SIDE
                        )
        );
    }

    @Test
    void kingShouldNotCastleIntoCheck() {

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

        /*
         * Attacks g1.
         */
        board.setPiece(
                Square.fromAlgebraic("g8"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("a8"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
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

        List<Move> moves =
                legalMoveGenerator
                        .generateLegalMoves(
                                state,
                                Square.fromAlgebraic("e1")
                        );

        assertFalse(
                moves.stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.CASTLE_KING_SIDE
                        )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private GameState emptyState(
            PieceColor sideToMove
    ) {

        return new GameState(
                new Board(),
                sideToMove,
                false,
                false,
                false,
                false,
                null,
                0,
                1
        );
    }

    private boolean containsTarget(
            List<Move> moves,
            String target
    ) {

        Square square =
                Square.fromAlgebraic(
                        target
                );

        return moves.stream()
                .anyMatch(move ->
                        move.to()
                                .equals(square)
                );
    }
}