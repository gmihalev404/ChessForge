package com.example.chessforge.service.game.engine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class GameState {

    private Board board;

    private PieceColor sideToMove;

    private boolean whiteKingSideCastlingAllowed;
    private boolean whiteQueenSideCastlingAllowed;

    private boolean blackKingSideCastlingAllowed;
    private boolean blackQueenSideCastlingAllowed;

    private Square enPassantTarget;

    /*
     * Number of half-moves since the last
     * pawn move or capture.
     *
     * Used by the fifty-move rule.
     */
    private int halfMoveClock;

    /*
     * Starts at 1 and increases after
     * every BLACK move.
     */
    private int fullMoveNumber;

    public GameState(
            Board board,
            PieceColor sideToMove,
            boolean whiteKingSideCastlingAllowed,
            boolean whiteQueenSideCastlingAllowed,
            boolean blackKingSideCastlingAllowed,
            boolean blackQueenSideCastlingAllowed,
            Square enPassantTarget,
            int halfMoveClock,
            int fullMoveNumber
    ) {

        this.board =
                Objects.requireNonNull(
                        board,
                        "Board cannot be null."
                );

        this.sideToMove =
                Objects.requireNonNull(
                        sideToMove,
                        "Side to move cannot be null."
                );

        if (halfMoveClock < 0) {

            throw new IllegalArgumentException(
                    "Half-move clock cannot be negative."
            );
        }

        if (fullMoveNumber < 1) {

            throw new IllegalArgumentException(
                    "Full-move number must be at least 1."
            );
        }

        this.whiteKingSideCastlingAllowed =
                whiteKingSideCastlingAllowed;

        this.whiteQueenSideCastlingAllowed =
                whiteQueenSideCastlingAllowed;

        this.blackKingSideCastlingAllowed =
                blackKingSideCastlingAllowed;

        this.blackQueenSideCastlingAllowed =
                blackQueenSideCastlingAllowed;

        this.enPassantTarget =
                enPassantTarget;

        this.halfMoveClock =
                halfMoveClock;

        this.fullMoveNumber =
                fullMoveNumber;
    }

    // =========================================================
    // INITIAL POSITION
    // =========================================================

    public static GameState initial() {

        return new GameState(
                Board.initial(),
                PieceColor.WHITE,
                true,
                true,
                true,
                true,
                null,
                0,
                1
        );
    }

    // =========================================================
    // COPY
    // =========================================================

    public GameState copy() {

        return new GameState(
                board.copy(),
                sideToMove,
                whiteKingSideCastlingAllowed,
                whiteQueenSideCastlingAllowed,
                blackKingSideCastlingAllowed,
                blackQueenSideCastlingAllowed,
                enPassantTarget,
                halfMoveClock,
                fullMoveNumber
        );
    }

    // =========================================================
    // BOARD
    // =========================================================

    public Board getBoard() {

        return board;
    }

    // =========================================================
    // SIDE TO MOVE
    // =========================================================

    public PieceColor getSideToMove() {

        return sideToMove;
    }

    public void setSideToMove(
            PieceColor sideToMove
    ) {

        this.sideToMove =
                Objects.requireNonNull(
                        sideToMove,
                        "Side to move cannot be null."
                );
    }

    public void switchSideToMove() {

        sideToMove =
                sideToMove.opposite();
    }

    // =========================================================
    // CASTLING RIGHTS
    // =========================================================

    public boolean isWhiteKingSideCastlingAllowed() {

        return whiteKingSideCastlingAllowed;
    }

    public boolean isWhiteQueenSideCastlingAllowed() {

        return whiteQueenSideCastlingAllowed;
    }

    public boolean isBlackKingSideCastlingAllowed() {

        return blackKingSideCastlingAllowed;
    }

    public boolean isBlackQueenSideCastlingAllowed() {

        return blackQueenSideCastlingAllowed;
    }

    // =========================================================
    // EN PASSANT
    // =========================================================

    public Square getEnPassantTarget() {

        return enPassantTarget;
    }

    // =========================================================
    // HALF-MOVE CLOCK
    // =========================================================

    public int getHalfMoveClock() {

        return halfMoveClock;
    }

    public void setHalfMoveClock(
            int halfMoveClock
    ) {

        if (halfMoveClock < 0) {

            throw new IllegalArgumentException(
                    "Half-move clock cannot be negative."
            );
        }

        this.halfMoveClock =
                halfMoveClock;
    }

    public void incrementHalfMoveClock() {

        halfMoveClock++;
    }

    public void resetHalfMoveClock() {

        halfMoveClock = 0;
    }

    // =========================================================
    // FULL-MOVE NUMBER
    // =========================================================

    public int getFullMoveNumber() {

        return fullMoveNumber;
    }

    public void setFullMoveNumber(
            int fullMoveNumber
    ) {

        if (fullMoveNumber < 1) {

            throw new IllegalArgumentException(
                    "Full-move number must be at least 1."
            );
        }

        this.fullMoveNumber =
                fullMoveNumber;
    }

    public void incrementFullMoveNumber() {

        fullMoveNumber++;
    }
}