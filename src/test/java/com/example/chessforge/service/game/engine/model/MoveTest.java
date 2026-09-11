package com.example.chessforge.service.game.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    @Test
    void shouldCreateNormalMove() {

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        assertEquals(
                Square.fromAlgebraic("e2"),
                move.from()
        );

        assertEquals(
                Square.fromAlgebraic("e4"),
                move.to()
        );

        assertEquals(
                MoveType.NORMAL,
                move.type()
        );

        assertNull(
                move.promotion()
        );
    }

    @Test
    void shouldRejectMoveToSameSquare() {

        Square e4 =
                Square.fromAlgebraic(
                        "e4"
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Move(
                                e4,
                                e4
                        )
        );
    }

    @Test
    void shouldCreatePromotionMove() {

        Move move =
                Move.promotion(
                        Square.fromAlgebraic("e7"),
                        Square.fromAlgebraic("e8"),
                        PieceType.QUEEN
                );

        assertEquals(
                MoveType.PROMOTION,
                move.type()
        );

        assertEquals(
                PieceType.QUEEN,
                move.promotion()
        );
    }

    @Test
    void promotionShouldAllowAllValidPromotionPieces() {

        PieceType[] validPieces = {
                PieceType.QUEEN,
                PieceType.ROOK,
                PieceType.BISHOP,
                PieceType.KNIGHT
        };

        for (PieceType pieceType : validPieces) {

            assertDoesNotThrow(
                    () ->
                            Move.promotion(
                                    Square.fromAlgebraic("a7"),
                                    Square.fromAlgebraic("a8"),
                                    pieceType
                            )
            );
        }
    }

    @Test
    void promotionShouldRejectPawn() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Move.promotion(
                                Square.fromAlgebraic("a7"),
                                Square.fromAlgebraic("a8"),
                                PieceType.PAWN
                        )
        );
    }

    @Test
    void promotionShouldRejectKing() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Move.promotion(
                                Square.fromAlgebraic("a7"),
                                Square.fromAlgebraic("a8"),
                                PieceType.KING
                        )
        );
    }

    @Test
    void promotionShouldRequirePromotionPiece() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Move(
                                Square.fromAlgebraic("a7"),
                                Square.fromAlgebraic("a8"),
                                MoveType.PROMOTION,
                                null
                        )
        );
    }

    @Test
    void normalMoveShouldRejectPromotionPiece() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Move(
                                Square.fromAlgebraic("a2"),
                                Square.fromAlgebraic("a3"),
                                MoveType.NORMAL,
                                PieceType.QUEEN
                        )
        );
    }
}