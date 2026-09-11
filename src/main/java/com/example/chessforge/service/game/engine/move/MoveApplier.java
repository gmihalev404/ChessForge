package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;

import java.util.Objects;

public class MoveApplier {

    public void apply(
            GameState state,
            Move move
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                move,
                "Move cannot be null."
        );

        Board board =
                state.getBoard();

        Piece movingPiece =
                board.getPiece(
                        move.from()
                );

        if (movingPiece == null) {

            throw new IllegalStateException(
                    "There is no piece on the source square."
            );
        }

        if (movingPiece.color()
                != state.getSideToMove()) {

            throw new IllegalStateException(
                    "Cannot move a piece of the opposite side."
            );
        }

        Piece destinationPiece =
                board.getPiece(
                        move.to()
                );

        if (destinationPiece != null
                && destinationPiece.color()
                == movingPiece.color()) {

            throw new IllegalStateException(
                    "Cannot capture a friendly piece."
            );
        }

        if (destinationPiece != null
                && destinationPiece.type()
                == PieceType.KING) {

            throw new IllegalStateException(
                    "The king cannot be captured."
            );
        }

        Square previousEnPassantTarget =
                state.getEnPassantTarget();

        Piece capturedPiece =
                destinationPiece;

        Square capturedSquare =
                destinationPiece == null
                        ? null
                        : move.to();

        switch (move.type()) {

            case NORMAL ->
                    applyNormalMove(
                            board,
                            move,
                            movingPiece
                    );

            case PROMOTION ->
                    applyPromotion(
                            board,
                            move,
                            movingPiece
                    );

            case EN_PASSANT -> {

                capturedSquare =
                        applyEnPassant(
                                board,
                                move,
                                movingPiece,
                                previousEnPassantTarget
                        );

                capturedPiece =
                        new Piece(
                                PieceType.PAWN,
                                movingPiece.color()
                                        .opposite()
                        );
            }

            case CASTLE_KING_SIDE,
                    CASTLE_QUEEN_SIDE ->
                    throw new UnsupportedOperationException(
                            "Castling is not implemented yet."
                    );
        }

        updateCastlingRights(
                state,
                movingPiece,
                move.from(),
                capturedPiece,
                capturedSquare
        );

        updateEnPassantTarget(
                state,
                movingPiece,
                move
        );

        updateMoveCounters(
                state,
                movingPiece,
                capturedPiece
        );

        if (movingPiece.color()
                == PieceColor.BLACK) {

            state.incrementFullMoveNumber();
        }

        state.switchSideToMove();
    }

    // =========================================================
    // NORMAL
    // =========================================================

    private void applyNormalMove(
            Board board,
            Move move,
            Piece movingPiece
    ) {

        if (movingPiece.type()
                == PieceType.PAWN
                && isPromotionRank(
                move.to(),
                movingPiece.color()
        )) {

            throw new IllegalStateException(
                    "A pawn reaching the last rank must promote."
            );
        }

        board.clearSquare(
                move.from()
        );

        board.setPiece(
                move.to(),
                movingPiece
        );
    }

    // =========================================================
    // PROMOTION
    // =========================================================

    private void applyPromotion(
            Board board,
            Move move,
            Piece movingPiece
    ) {

        if (movingPiece.type()
                != PieceType.PAWN) {

            throw new IllegalStateException(
                    "Only a pawn can promote."
            );
        }

        if (!isPromotionRank(
                move.to(),
                movingPiece.color()
        )) {

            throw new IllegalStateException(
                    "Pawn can promote only on the last rank."
            );
        }

        board.clearSquare(
                move.from()
        );

        board.setPiece(
                move.to(),
                new Piece(
                        move.promotion(),
                        movingPiece.color()
                )
        );
    }

    // =========================================================
    // EN PASSANT
    // =========================================================

    private Square applyEnPassant(
            Board board,
            Move move,
            Piece movingPiece,
            Square enPassantTarget
    ) {

        if (movingPiece.type()
                != PieceType.PAWN) {

            throw new IllegalStateException(
                    "Only a pawn can capture en passant."
            );
        }

        if (enPassantTarget == null
                || !enPassantTarget.equals(
                move.to()
        )) {

            throw new IllegalStateException(
                    "Invalid en passant target."
            );
        }

        if (!board.isEmpty(
                move.to()
        )) {

            throw new IllegalStateException(
                    "En passant destination must be empty."
            );
        }

        int direction =
                movingPiece.color()
                        == PieceColor.WHITE
                        ? 1
                        : -1;

        if (Math.abs(
                move.to().file()
                        - move.from().file()
        ) != 1
                || move.to().rank()
                - move.from().rank()
                != direction) {

            throw new IllegalStateException(
                    "Invalid en passant move."
            );
        }

        Square capturedPawnSquare =
                new Square(
                        move.to().file(),
                        move.to().rank()
                                - direction
                );

        Piece capturedPiece =
                board.getPiece(
                        capturedPawnSquare
                );

        if (capturedPiece == null
                || capturedPiece.type()
                != PieceType.PAWN
                || capturedPiece.color()
                == movingPiece.color()) {

            throw new IllegalStateException(
                    "No capturable pawn exists for en passant."
            );
        }

        board.clearSquare(
                move.from()
        );

        board.clearSquare(
                capturedPawnSquare
        );

        board.setPiece(
                move.to(),
                movingPiece
        );

        return capturedPawnSquare;
    }

    // =========================================================
    // CASTLING RIGHTS
    // =========================================================

    private void updateCastlingRights(
            GameState state,
            Piece movingPiece,
            Square from,
            Piece capturedPiece,
            Square capturedSquare
    ) {

        if (movingPiece.type()
                == PieceType.KING) {

            removeBothCastlingRights(
                    state,
                    movingPiece.color()
            );
        }

        if (movingPiece.type()
                == PieceType.ROOK) {

            removeRookCastlingRight(
                    state,
                    movingPiece.color(),
                    from
            );
        }

        if (capturedPiece != null
                && capturedPiece.type()
                == PieceType.ROOK
                && capturedSquare != null) {

            removeRookCastlingRight(
                    state,
                    capturedPiece.color(),
                    capturedSquare
            );
        }
    }

    private void removeBothCastlingRights(
            GameState state,
            PieceColor color
    ) {

        if (color == PieceColor.WHITE) {

            state.setWhiteKingSideCastlingAllowed(
                    false
            );

            state.setWhiteQueenSideCastlingAllowed(
                    false
            );

        } else {

            state.setBlackKingSideCastlingAllowed(
                    false
            );

            state.setBlackQueenSideCastlingAllowed(
                    false
            );
        }
    }

    private void removeRookCastlingRight(
            GameState state,
            PieceColor color,
            Square square
    ) {

        if (color == PieceColor.WHITE) {

            if (square.equals(
                    Square.fromAlgebraic("a1")
            )) {

                state.setWhiteQueenSideCastlingAllowed(
                        false
                );
            }

            if (square.equals(
                    Square.fromAlgebraic("h1")
            )) {

                state.setWhiteKingSideCastlingAllowed(
                        false
                );
            }

        } else {

            if (square.equals(
                    Square.fromAlgebraic("a8")
            )) {

                state.setBlackQueenSideCastlingAllowed(
                        false
                );
            }

            if (square.equals(
                    Square.fromAlgebraic("h8")
            )) {

                state.setBlackKingSideCastlingAllowed(
                        false
                );
            }
        }
    }

    // =========================================================
    // EN PASSANT TARGET
    // =========================================================

    private void updateEnPassantTarget(
            GameState state,
            Piece movingPiece,
            Move move
    ) {

        state.setEnPassantTarget(
                null
        );

        if (movingPiece.type()
                != PieceType.PAWN) {

            return;
        }

        int rankDifference =
                move.to().rank()
                        - move.from().rank();

        if (Math.abs(
                rankDifference
        ) != 2) {

            return;
        }

        int middleRank =
                (
                        move.from().rank()
                                + move.to().rank()
                )
                        / 2;

        state.setEnPassantTarget(
                new Square(
                        move.from().file(),
                        middleRank
                )
        );
    }

    // =========================================================
    // MOVE COUNTERS
    // =========================================================

    private void updateMoveCounters(
            GameState state,
            Piece movingPiece,
            Piece capturedPiece
    ) {

        if (movingPiece.type()
                == PieceType.PAWN
                || capturedPiece != null) {

            state.resetHalfMoveClock();

            return;
        }

        state.incrementHalfMoveClock();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean isPromotionRank(
            Square square,
            PieceColor color
    ) {

        return color == PieceColor.WHITE
                ? square.rank() == 7
                : square.rank() == 0;
    }
}