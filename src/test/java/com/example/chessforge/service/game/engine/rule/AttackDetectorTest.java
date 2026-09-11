package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttackDetectorTest {

    private AttackDetector attackDetector;

    @BeforeEach
    void setUp() {

        attackDetector =
                new AttackDetector();
    }

    // =========================================================
    // PAWN
    // =========================================================

    @Test
    void whitePawnShouldAttackDiagonally() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        assertTrue(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("d5"),
                                PieceColor.WHITE
                        )
        );

        assertTrue(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("f5"),
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void pawnShouldNotAttackForwardSquare() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        assertFalse(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("e5"),
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void blackPawnShouldAttackTowardLowerRanks() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e5"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        assertTrue(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("d4"),
                                PieceColor.BLACK
                        )
        );

        assertTrue(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("f4"),
                                PieceColor.BLACK
                        )
        );
    }

    // =========================================================
    // KNIGHT
    // =========================================================

    @Test
    void knightShouldAttackInLShape() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("d4"),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        assertTrue(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("f5"),
                                PieceColor.BLACK
                        )
        );

        assertFalse(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("d5"),
                                PieceColor.BLACK
                        )
        );
    }

    // =========================================================
    // BISHOP
    // =========================================================

    @Test
    void bishopShouldAttackAlongDiagonal() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("b2"),
                        new Piece(
                                PieceType.BISHOP,
                                PieceColor.BLACK
                        )
                );

        assertTrue(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("e5"),
                                PieceColor.BLACK
                        )
        );
    }

    // =========================================================
    // QUEEN
    // =========================================================

    @Test
    void queenShouldAttackStraightAndDiagonally() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("d4"),
                        new Piece(
                                PieceType.QUEEN,
                                PieceColor.BLACK
                        )
                );

        assertTrue(
                attackDetector.isSquareAttacked(
                        state,
                        Square.fromAlgebraic("d8"),
                        PieceColor.BLACK
                )
        );

        assertTrue(
                attackDetector.isSquareAttacked(
                        state,
                        Square.fromAlgebraic("h8"),
                        PieceColor.BLACK
                )
        );
    }

    // =========================================================
    // KING
    // =========================================================

    @Test
    void kingShouldAttackAdjacentSquares() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.KING,
                                PieceColor.BLACK
                        )
                );

        assertTrue(
                attackDetector.isSquareAttacked(
                        state,
                        Square.fromAlgebraic("e5"),
                        PieceColor.BLACK
                )
        );

        assertFalse(
                attackDetector.isSquareAttacked(
                        state,
                        Square.fromAlgebraic("e6"),
                        PieceColor.BLACK
                )
        );
    }

    // =========================================================
    // BLOCKING
    // =========================================================

    @Test
    void slidingAttackShouldBeBlockedByPiece() {

        GameState state =
                emptyState();

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("a1"),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("a4"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        assertFalse(
                attackDetector
                        .isSquareAttacked(
                                state,
                                Square.fromAlgebraic("a8"),
                                PieceColor.BLACK
                        )
        );
    }

    // =========================================================
    // CHECKS
    // =========================================================

    @Test
    void whiteKingShouldBeInCheckFromBlackRook() {

        GameState state =
                emptyState();

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

        assertTrue(
                attackDetector.isInCheck(
                        state,
                        PieceColor.WHITE
                )
        );
    }

    @Test
    void blockedRookShouldNotGiveCheck() {

        GameState state =
                emptyState();

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
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.BISHOP,
                                PieceColor.WHITE
                        )
                );

        assertFalse(
                attackDetector.isInCheck(
                        state,
                        PieceColor.WHITE
                )
        );
    }

    @Test
    void knightShouldGiveCheck() {

        GameState state =
                emptyState();

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
                        Square.fromAlgebraic("f3"),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        assertTrue(
                attackDetector.isInCheck(
                        state,
                        PieceColor.WHITE
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private GameState emptyState() {

        return new GameState(
                new Board(),
                PieceColor.WHITE,
                false,
                false,
                false,
                false,
                null,
                0,
                1
        );
    }
}