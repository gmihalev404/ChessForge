package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveGeneratorTest {

    private MoveGenerator moveGenerator;

    @BeforeEach
    void setUp() {

        moveGenerator =
                new MoveGenerator();
    }

    // =========================================================
    // KNIGHT - EMPTY BOARD
    // =========================================================

    @Test
    void knightInCenterShouldHaveEightMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                8,
                moves.size()
        );

        assertTargets(
                moves,
                "b3",
                "b5",
                "c2",
                "c6",
                "e2",
                "e6",
                "f3",
                "f5"
        );
    }

    @Test
    void knightInCornerShouldHaveTwoMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "a1"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                2,
                moves.size()
        );

        assertTargets(
                moves,
                "b3",
                "c2"
        );
    }

    // =========================================================
    // KNIGHT - PIECES
    // =========================================================

    @Test
    void knightShouldNotMoveOntoFriendlyPiece() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        Square target =
                Square.fromAlgebraic(
                        "f5"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        target,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertFalse(
                containsTarget(
                        moves,
                        "f5"
                )
        );

        assertEquals(
                7,
                moves.size()
        );
    }

    @Test
    void knightShouldCaptureEnemyPiece() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        Square target =
                Square.fromAlgebraic(
                        "f5"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        target,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTrue(
                containsTarget(
                        moves,
                        "f5"
                )
        );

        assertEquals(
                8,
                moves.size()
        );
    }

    @Test
    void piecesBetweenKnightAndDestinationShouldNotMatter() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.WHITE
                        )
                );

        /*
         * Surround the knight with pieces.
         * A knight must still be able to jump.
         */
        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("d5"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("d3"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic("c4"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                8,
                moves.size()
        );
    }

    // =========================================================
    // BISHOP
    // =========================================================

    @Test
    void bishopInCenterShouldGenerateAllDiagonalMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.BISHOP,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                13,
                moves.size()
        );

        assertTargets(
                moves,
                "e5",
                "f6",
                "g7",
                "h8",

                "c5",
                "b6",
                "a7",

                "e3",
                "f2",
                "g1",

                "c3",
                "b2",
                "a1"
        );
    }

    @Test
    void bishopShouldStopBeforeFriendlyPiece() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.BISHOP,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "f6"
                        ),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTrue(
                containsTarget(
                        moves,
                        "e5"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "f6"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "g7"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "h8"
                )
        );
    }

    @Test
    void bishopShouldCaptureEnemyPieceAndStopBehindIt() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.BISHOP,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "f6"
                        ),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTrue(
                containsTarget(
                        moves,
                        "e5"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "f6"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "g7"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "h8"
                )
        );
    }

    // =========================================================
    // ROOK
    // =========================================================

    @Test
    void rookInCenterShouldGenerateAllStraightMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                14,
                moves.size()
        );

        assertTargets(
                moves,
                "a4",
                "b4",
                "c4",
                "e4",
                "f4",
                "g4",
                "h4",

                "d1",
                "d2",
                "d3",
                "d5",
                "d6",
                "d7",
                "d8"
        );
    }

    @Test
    void rookShouldRespectFriendlyAndEnemyBlockingPieces() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.WHITE
                        )
                );

        /*
         * Friendly piece to the right.
         */
        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "f4"
                        ),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        /*
         * Enemy piece above.
         */
        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "d6"
                        ),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        // Before friendly piece.
        assertTrue(
                containsTarget(
                        moves,
                        "e4"
                )
        );

        // Friendly piece itself.
        assertFalse(
                containsTarget(
                        moves,
                        "f4"
                )
        );

        // Behind friendly piece.
        assertFalse(
                containsTarget(
                        moves,
                        "g4"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "h4"
                )
        );

        // Before enemy.
        assertTrue(
                containsTarget(
                        moves,
                        "d5"
                )
        );

        // Enemy can be captured.
        assertTrue(
                containsTarget(
                        moves,
                        "d6"
                )
        );

        // Cannot continue through enemy.
        assertFalse(
                containsTarget(
                        moves,
                        "d7"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "d8"
                )
        );
    }

    // =========================================================
    // QUEEN
    // =========================================================

    @Test
    void queenInCenterShouldGenerateStraightAndDiagonalMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.QUEEN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        /*
         * Rook moves   = 14
         * Bishop moves = 13
         *
         * Total = 27
         */
        assertEquals(
                27,
                moves.size()
        );

        assertTrue(
                containsTarget(
                        moves,
                        "d8"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "h4"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "h8"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "a1"
                )
        );
    }
    // =========================================================
    // KING
    // =========================================================

    @Test
    void kingInCenterShouldHaveEightMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                8,
                moves.size()
        );

        assertTargets(
                moves,
                "c3",
                "d3",
                "e3",
                "c4",
                "e4",
                "c5",
                "d5",
                "e5"
        );
    }

    @Test
    void kingInCornerShouldHaveThreeMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "a1"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                3,
                moves.size()
        );

        assertTargets(
                moves,
                "a2",
                "b1",
                "b2"
        );
    }

    @Test
    void kingShouldNotMoveOntoFriendlyPiece() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "e5"
                        ),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertFalse(
                containsTarget(
                        moves,
                        "e5"
                )
        );

        assertEquals(
                7,
                moves.size()
        );
    }

    @Test
    void kingShouldCaptureEnemyPiece() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KING,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "e5"
                        ),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTrue(
                containsTarget(
                        moves,
                        "e5"
                )
        );

        assertEquals(
                8,
                moves.size()
        );
    }

    // =========================================================
    // PAWN - MOVEMENT
    // =========================================================

    @Test
    void whitePawnShouldMoveOneOrTwoSquaresFromStartingRank() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e2"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTargets(
                moves,
                "e3",
                "e4"
        );
    }

    @Test
    void whitePawnOutsideStartingRankShouldMoveOnlyOneSquare() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTargets(
                moves,
                "e5"
        );
    }

    @Test
    void blackPawnShouldMoveTowardLowerRanks() {

        GameState state =
                emptyState(
                        PieceColor.BLACK
                );

        Square from =
                Square.fromAlgebraic(
                        "e7"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTargets(
                moves,
                "e6",
                "e5"
        );
    }

    @Test
    void pawnShouldNotMoveWhenPieceBlocksIt() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e2"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "e3"
                        ),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTrue(
                moves.isEmpty()
        );
    }

    @Test
    void pawnShouldMoveOneSquareWhenDoubleMoveDestinationIsBlocked() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e2"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "e4"
                        ),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTargets(
                moves,
                "e3"
        );
    }

    @Test
    void whitePawnShouldCaptureEnemyPieceDiagonally() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "d5"
                        ),
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "f5"
                        ),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTargets(
                moves,
                "e5",
                "d5",
                "f5"
        );
    }

    @Test
    void pawnShouldNotCaptureFriendlyPieceDiagonally() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "d5"
                        ),
                        new Piece(
                                PieceType.BISHOP,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertFalse(
                containsTarget(
                        moves,
                        "d5"
                )
        );

        assertTrue(
                containsTarget(
                        moves,
                        "e5"
                )
        );
    }

    @Test
    void pawnShouldNotMoveDiagonallyToEmptySquare() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertFalse(
                containsTarget(
                        moves,
                        "d5"
                )
        );

        assertFalse(
                containsTarget(
                        moves,
                        "f5"
                )
        );
    }

    @Test
    void whitePawnShouldGenerateFourPromotionMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e7"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                4,
                moves.size()
        );

        assertTrue(
                moves.stream()
                        .allMatch(move ->
                                move.type()
                                        == MoveType.PROMOTION
                        )
        );

        assertPromotionOptions(
                moves,
                "e8"
        );
    }

    @Test
    void pawnCaptureOnLastRankShouldGeneratePromotionMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "e7"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "f8"
                        ),
                        new Piece(
                                PieceType.ROOK,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        /*
         * 4 moves e8=...
         * +
         * 4 moves exf8=...
         */
        assertEquals(
                8,
                moves.size()
        );

        assertPromotionOptions(
                moves,
                "e8"
        );

        assertPromotionOptions(
                moves,
                "f8"
        );
    }

    @Test
    void blackPawnShouldPromoteOnFirstRank() {

        GameState state =
                emptyState(
                        PieceColor.BLACK
                );

        Square from =
                Square.fromAlgebraic(
                        "e2"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                4,
                moves.size()
        );

        assertPromotionOptions(
                moves,
                "e1"
        );
    }

    @Test
    void whitePawnShouldGenerateEnPassantMove() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square whitePawn =
                Square.fromAlgebraic(
                        "e5"
                );

        state.getBoard()
                .setPiece(
                        whitePawn,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        /*
         * Black pawn has just played d7-d5.
         */
        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                "d5"
                        ),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.BLACK
                        )
                );

        state.setEnPassantTarget(
                Square.fromAlgebraic(
                        "d6"
                )
        );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                whitePawn
                        );

        Move enPassant =
                moves.stream()
                        .filter(move ->
                                move.to()
                                        .equals(
                                                Square.fromAlgebraic(
                                                        "d6"
                                                )
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                MoveType.EN_PASSANT,
                enPassant.type()
        );
    }

    @Test
    void pawnShouldNotGenerateEnPassantWithoutCapturablePawn() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square whitePawn =
                Square.fromAlgebraic(
                        "e5"
                );

        state.getBoard()
                .setPiece(
                        whitePawn,
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        state.setEnPassantTarget(
                Square.fromAlgebraic(
                        "d6"
                )
        );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                whitePawn
                        );

        assertFalse(
                moves.stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.EN_PASSANT
                        )
        );
    }

    // =========================================================
    // TURN
    // =========================================================

    @Test
    void shouldNotGenerateMovesForOpponentPiece() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertTrue(
                moves.isEmpty()
        );
    }

    @Test
    void shouldGenerateBlackKnightMovesWhenBlackIsToMove() {

        GameState state =
                emptyState(
                        PieceColor.BLACK
                );

        Square from =
                Square.fromAlgebraic(
                        "d4"
                );

        state.getBoard()
                .setPiece(
                        from,
                        new Piece(
                                PieceType.KNIGHT,
                                PieceColor.BLACK
                        )
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        assertEquals(
                8,
                moves.size()
        );
    }

    // =========================================================
    // EMPTY SQUARE
    // =========================================================

    @Test
    void emptySquareShouldHaveNoMoves() {

        GameState state =
                emptyState(
                        PieceColor.WHITE
                );

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "d4"
                                )
                        );

        assertTrue(
                moves.isEmpty()
        );
    }

    // =========================================================
    // INITIAL POSITION
    // =========================================================

    @Test
    void whiteKnightOnB1ShouldHaveTwoMovesInitially() {

        GameState state =
                GameState.initial();

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "b1"
                                )
                        );

        /*
         * d2 contains our pawn.
         *
         * Legal pseudo destinations:
         * a3
         * c3
         */
        assertEquals(
                2,
                moves.size()
        );

        assertTargets(
                moves,
                "a3",
                "c3"
        );
    }

    @Test
    void slidingPiecesShouldInitiallyBeBlockedByOwnPawns() {

        GameState state =
                GameState.initial();

        assertTrue(
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "c1"
                                )
                        )
                        .isEmpty()
        );

        assertTrue(
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "a1"
                                )
                        )
                        .isEmpty()
        );

        assertTrue(
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "d1"
                                )
                        )
                        .isEmpty()
        );
    }

    @Test
    void whiteKingShouldInitiallyHaveNoPseudoLegalMoves() {

        GameState state =
                GameState.initial();

        List<Move> moves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                Square.fromAlgebraic(
                                        "e1"
                                )
                        );

        assertTrue(
                moves.isEmpty()
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

    private void assertTargets(
            List<Move> moves,
            String... expectedTargets
    ) {

        assertEquals(
                expectedTargets.length,
                moves.size()
        );

        for (String target :
                expectedTargets) {

            assertTrue(
                    containsTarget(
                            moves,
                            target
                    ),
                    "Expected move to "
                            + target
            );
        }
    }

    private void assertPromotionOptions(
            List<Move> moves,
            String target
    ) {

        Square targetSquare =
                Square.fromAlgebraic(
                        target
                );

        assertTrue(
                moves.stream()
                        .anyMatch(move ->
                                move.to().equals(targetSquare)
                                        && move.promotion()
                                        == PieceType.QUEEN
                        )
        );

        assertTrue(
                moves.stream()
                        .anyMatch(move ->
                                move.to().equals(targetSquare)
                                        && move.promotion()
                                        == PieceType.ROOK
                        )
        );

        assertTrue(
                moves.stream()
                        .anyMatch(move ->
                                move.to().equals(targetSquare)
                                        && move.promotion()
                                        == PieceType.BISHOP
                        )
        );

        assertTrue(
                moves.stream()
                        .anyMatch(move ->
                                move.to().equals(targetSquare)
                                        && move.promotion()
                                        == PieceType.KNIGHT
                        )
        );
    }
}