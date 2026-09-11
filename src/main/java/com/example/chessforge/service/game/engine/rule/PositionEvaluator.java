package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PositionEvaluator {

    private final LegalMoveGenerator legalMoveGenerator;
    private final AttackDetector attackDetector;

    public PositionEvaluator(
            LegalMoveGenerator legalMoveGenerator,
            AttackDetector attackDetector
    ) {

        this.legalMoveGenerator =
                Objects.requireNonNull(
                        legalMoveGenerator,
                        "Legal move generator cannot be null."
                );

        this.attackDetector =
                Objects.requireNonNull(
                        attackDetector,
                        "Attack detector cannot be null."
                );
    }

    private record PieceOnSquare(
            Piece piece,
            Square square
    ) {
    }

    public boolean isCheckmate(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        PieceColor sideToMove =
                state.getSideToMove();

        return attackDetector.isInCheck(
                state,
                sideToMove
        )
                && legalMoveGenerator
                .generateAllLegalMoves(state)
                .isEmpty();
    }

    public boolean isStalemate(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        PieceColor sideToMove =
                state.getSideToMove();

        return !attackDetector.isInCheck(
                state,
                sideToMove
        )
                && legalMoveGenerator
                .generateAllLegalMoves(state)
                .isEmpty();
    }

    public boolean isFiftyMoveRuleDraw(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        return state.getHalfMoveClock() >= 100;
    }

    public boolean isInsufficientMaterial(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Board board =
                state.getBoard();

        List<PieceOnSquare> nonKingPieces =
                new ArrayList<>();

        for (int rank = 0;
             rank < Square.BOARD_SIZE;
             rank++) {

            for (int file = 0;
                 file < Square.BOARD_SIZE;
                 file++) {

                Square square =
                        new Square(
                                file,
                                rank
                        );

                Piece piece =
                        board.getPiece(
                                square
                        );

                if (piece == null
                        || piece.type()
                        == PieceType.KING) {

                    continue;
                }

                /*
                 * Presence of pawn, rook or queen means
                 * this is not one of our dead-material cases.
                 */
                if (piece.type()
                        == PieceType.PAWN
                        || piece.type()
                        == PieceType.ROOK
                        || piece.type()
                        == PieceType.QUEEN) {

                    return false;
                }

                nonKingPieces.add(
                        new PieceOnSquare(
                                piece,
                                square
                        )
                );
            }
        }

        // K vs K
        if (nonKingPieces.isEmpty()) {

            return true;
        }

        // K+B vs K or K+N vs K
        if (nonKingPieces.size() == 1) {

            PieceType type =
                    nonKingPieces
                            .get(0)
                            .piece()
                            .type();

            return type == PieceType.BISHOP
                    || type == PieceType.KNIGHT;
        }

        /*
         * K+B vs K+B with both bishops
         * living on the same square color.
         */
        if (nonKingPieces.size() == 2) {

            PieceOnSquare first =
                    nonKingPieces.get(0);

            PieceOnSquare second =
                    nonKingPieces.get(1);

            if (first.piece().type()
                    != PieceType.BISHOP
                    || second.piece().type()
                    != PieceType.BISHOP) {

                return false;
            }

            return squareColor(
                    first.square()
            )
                    == squareColor(
                    second.square()
            );
        }

        return false;
    }

    public boolean isSeventyFiveMoveRuleDraw(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        return state.getHalfMoveClock() >= 150;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private int squareColor(
            Square square
    ) {

        return (
                square.file()
                        + square.rank()
        ) % 2;
    }
}