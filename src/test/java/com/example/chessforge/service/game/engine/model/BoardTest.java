package com.example.chessforge.service.game.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void newBoardShouldBeEmpty() {

        Board board =
                new Board();

        for (int rank = 0;
             rank < 8;
             rank++) {

            for (int file = 0;
                 file < 8;
                 file++) {

                assertTrue(
                        board.isEmpty(
                                new Square(
                                        file,
                                        rank
                                )
                        )
                );
            }
        }
    }

    @Test
    void initialBoardShouldHaveWhitePiecesInCorrectPositions() {

        Board board =
                Board.initial();

        assertPiece(
                board,
                "a1",
                PieceType.ROOK,
                PieceColor.WHITE
        );

        assertPiece(
                board,
                "b1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        assertPiece(
                board,
                "c1",
                PieceType.BISHOP,
                PieceColor.WHITE
        );

        assertPiece(
                board,
                "d1",
                PieceType.QUEEN,
                PieceColor.WHITE
        );

        assertPiece(
                board,
                "e1",
                PieceType.KING,
                PieceColor.WHITE
        );

        assertPiece(
                board,
                "h1",
                PieceType.ROOK,
                PieceColor.WHITE
        );
    }

    @Test
    void initialBoardShouldHaveBlackPiecesInCorrectPositions() {

        Board board =
                Board.initial();

        assertPiece(
                board,
                "a8",
                PieceType.ROOK,
                PieceColor.BLACK
        );

        assertPiece(
                board,
                "b8",
                PieceType.KNIGHT,
                PieceColor.BLACK
        );

        assertPiece(
                board,
                "d8",
                PieceType.QUEEN,
                PieceColor.BLACK
        );

        assertPiece(
                board,
                "e8",
                PieceType.KING,
                PieceColor.BLACK
        );

        assertPiece(
                board,
                "h8",
                PieceType.ROOK,
                PieceColor.BLACK
        );
    }

    @Test
    void initialBoardShouldPlaceAllPawnsCorrectly() {

        Board board =
                Board.initial();

        for (int file = 0;
             file < 8;
             file++) {

            assertEquals(
                    new Piece(
                            PieceType.PAWN,
                            PieceColor.WHITE
                    ),
                    board.getPiece(
                            new Square(
                                    file,
                                    1
                            )
                    )
            );

            assertEquals(
                    new Piece(
                            PieceType.PAWN,
                            PieceColor.BLACK
                    ),
                    board.getPiece(
                            new Square(
                                    file,
                                    6
                            )
                    )
            );
        }
    }

    @Test
    void initialBoardShouldHaveEmptyMiddleRanks() {

        Board board =
                Board.initial();

        for (int rank = 2;
             rank <= 5;
             rank++) {

            for (int file = 0;
                 file < 8;
                 file++) {

                assertTrue(
                        board.isEmpty(
                                new Square(
                                        file,
                                        rank
                                )
                        )
                );
            }
        }
    }

    @Test
    void shouldSetAndClearPiece() {

        Board board =
                new Board();

        Square square =
                Square.fromAlgebraic(
                        "e4"
                );

        Piece queen =
                new Piece(
                        PieceType.QUEEN,
                        PieceColor.WHITE
                );

        board.setPiece(
                square,
                queen
        );

        assertEquals(
                queen,
                board.getPiece(square)
        );

        assertFalse(
                board.isEmpty(square)
        );

        board.clearSquare(
                square
        );

        assertNull(
                board.getPiece(square)
        );

        assertTrue(
                board.isEmpty(square)
        );
    }

    @Test
    void copiedBoardShouldBeIndependent() {

        Board original =
                Board.initial();

        Board copy =
                original.copy();

        Square e2 =
                Square.fromAlgebraic(
                        "e2"
                );

        Square e4 =
                Square.fromAlgebraic(
                        "e4"
                );

        Piece pawn =
                copy.getPiece(e2);

        copy.clearSquare(e2);

        copy.setPiece(
                e4,
                pawn
        );

        /*
         * Copy changed.
         */
        assertTrue(
                copy.isEmpty(e2)
        );

        assertEquals(
                pawn,
                copy.getPiece(e4)
        );

        /*
         * Original stays untouched.
         */
        assertFalse(
                original.isEmpty(e2)
        );

        assertTrue(
                original.isEmpty(e4)
        );
    }

    @Test
    void shouldRejectNullSquare() {

        Board board =
                new Board();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        board.getPiece(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        board.setPiece(
                                null,
                                new Piece(
                                        PieceType.ROOK,
                                        PieceColor.WHITE
                                )
                        )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void assertPiece(
            Board board,
            String squareNotation,
            PieceType expectedType,
            PieceColor expectedColor
    ) {

        Piece piece =
                board.getPiece(
                        Square.fromAlgebraic(
                                squareNotation
                        )
                );

        assertNotNull(
                piece
        );

        assertEquals(
                expectedType,
                piece.type()
        );

        assertEquals(
                expectedColor,
                piece.color()
        );
    }
}