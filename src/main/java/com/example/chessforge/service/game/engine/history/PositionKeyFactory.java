package com.example.chessforge.service.game.engine.history;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;

import java.util.Objects;

public class PositionKeyFactory {

    private final LegalMoveGenerator legalMoveGenerator;

    public PositionKeyFactory(
            LegalMoveGenerator legalMoveGenerator
    ) {

        this.legalMoveGenerator =
                Objects.requireNonNull(
                        legalMoveGenerator,
                        "Legal move generator cannot be null."
                );
    }

    public PositionKey create(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        return new PositionKey(
                createBoardSignature(
                        state.getBoard()
                ),
                state.getSideToMove(),
                state.isWhiteKingSideCastlingAllowed(),
                state.isWhiteQueenSideCastlingAllowed(),
                state.isBlackKingSideCastlingAllowed(),
                state.isBlackQueenSideCastlingAllowed(),
                getEffectiveEnPassantTarget(
                        state
                )
        );
    }

    // =========================================================
    // BOARD
    // =========================================================

    private String createBoardSignature(
            Board board
    ) {

        StringBuilder signature =
                new StringBuilder(64);

        for (int rank = 0;
             rank < Square.BOARD_SIZE;
             rank++) {

            for (int file = 0;
                 file < Square.BOARD_SIZE;
                 file++) {

                Piece piece =
                        board.getPiece(
                                new Square(
                                        file,
                                        rank
                                )
                        );

                signature.append(
                        piece == null
                                ? '.'
                                : pieceSymbol(piece)
                );
            }
        }

        return signature.toString();
    }

    private char pieceSymbol(
            Piece piece
    ) {

        char symbol =
                switch (piece.type()) {

                    case PAWN -> 'p';
                    case KNIGHT -> 'n';
                    case BISHOP -> 'b';
                    case ROOK -> 'r';
                    case QUEEN -> 'q';
                    case KING -> 'k';
                };

        return piece.color()
                == PieceColor.WHITE
                ? Character.toUpperCase(symbol)
                : symbol;
    }

    // =========================================================
    // EN PASSANT
    // =========================================================

    private Square getEffectiveEnPassantTarget(
            GameState state
    ) {

        Square target =
                state.getEnPassantTarget();

        if (target == null) {

            return null;
        }

        /*
         * For repetition purposes an en-passant target
         * matters only if the side to move can actually
         * make a legal en-passant capture.
         */
        boolean legalEnPassantExists =
                legalMoveGenerator
                        .generateAllLegalMoves(state)
                        .stream()
                        .anyMatch(move ->
                                move.type()
                                        == MoveType.EN_PASSANT
                        );

        return legalEnPassantExists
                ? target
                : null;
    }
}