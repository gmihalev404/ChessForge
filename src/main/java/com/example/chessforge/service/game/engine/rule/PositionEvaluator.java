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

    public boolean hasInsufficientMatingMaterial(
            GameState state,
            PieceColor color
    ) {

        Board board =
                state.getBoard();

        MatingMaterial ownMaterial =
                collectMatingMaterial(
                        board,
                        color
                );

        MatingMaterial opponentMaterial =
                collectMatingMaterial(
                        board,
                        color.opposite()
                );

        /*
         * Pawn, rook or queen is enough for a possible mate.
         */
        if (ownMaterial.pawns() > 0
                || ownMaterial.rooks() > 0
                || ownMaterial.queens() > 0) {

            return false;
        }

        int bishopCount =
                ownMaterial.lightSquareBishops()
                        + ownMaterial.darkSquareBishops();

        /*
         * Two knights can reach a mating position,
         * although they cannot force mate against a bare king.
         */
        if (ownMaterial.knights() >= 2) {
            return false;
        }

        /*
         * Bishop + knight can mate a bare king.
         */
        if (ownMaterial.knights() >= 1
                && bishopCount >= 1) {

            return false;
        }

        /*
         * Bishops on both square colours can mate.
         */
        if (ownMaterial.lightSquareBishops() > 0
                && ownMaterial.darkSquareBishops() > 0) {

            return false;
        }

        /*
         * King only.
         */
        if (ownMaterial.knights() == 0
                && bishopCount == 0) {

            return true;
        }

        /*
         * King + one knight.
         *
         * Against a bare king there is no possible mate.
         * If the opponent still has material, that material
         * may block its own king and make a mate possible.
         */
        if (ownMaterial.knights() == 1) {

            return opponentMaterial.nonKingPieceCount()
                    == 0;
        }

        /*
         * From here the player has one or more bishops,
         * but all bishops live on the same square colour.
         */

        if (opponentMaterial.nonKingPieceCount()
                == 0) {

            return true;
        }

        /*
         * Any opponent pawn, knight, rook or queen can
         * potentially help create a mating position.
         */
        if (opponentMaterial.pawns() > 0
                || opponentMaterial.knights() > 0
                || opponentMaterial.rooks() > 0
                || opponentMaterial.queens() > 0) {

            return false;
        }

        /*
         * Only bishops remain on the opponent's side.
         *
         * If every bishop on the board moves on the same
         * colour complex, mate is impossible.
         */

        if (ownMaterial.lightSquareBishops() > 0) {

            return opponentMaterial
                    .darkSquareBishops() == 0;
        }

        return opponentMaterial
                .lightSquareBishops() == 0;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private record MatingMaterial(
            int pawns,
            int knights,
            int lightSquareBishops,
            int darkSquareBishops,
            int rooks,
            int queens
    ) {

        private int nonKingPieceCount() {

            return pawns
                    + knights
                    + lightSquareBishops
                    + darkSquareBishops
                    + rooks
                    + queens;
        }
    }

    private int squareColor(
            Square square
    ) {

        return (
                square.file()
                        + square.rank()
        ) % 2;
    }

    private MatingMaterial collectMatingMaterial(
            Board board,
            PieceColor color
    ) {

        int pawns = 0;
        int knights = 0;
        int lightSquareBishops = 0;
        int darkSquareBishops = 0;
        int rooks = 0;
        int queens = 0;

        for (int rank = 0; rank < 8; rank++) {

            for (int file = 0; file < 8; file++) {

                Square square =
                        new Square(
                                file,
                                rank
                        );

                Piece piece =
                        board.getPiece(square);

                if (piece == null
                        || piece.color() != color) {

                    continue;
                }

                switch (piece.type()) {

                    case PAWN ->
                            pawns++;

                    case KNIGHT ->
                            knights++;

                    case BISHOP -> {

                        if (isLightSquare(square)) {
                            lightSquareBishops++;
                        } else {
                            darkSquareBishops++;
                        }
                    }

                    case ROOK ->
                            rooks++;

                    case QUEEN ->
                            queens++;

                    case KING -> {
                        // King is not counted.
                    }
                }
            }
        }

        return new MatingMaterial(
                pawns,
                knights,
                lightSquareBishops,
                darkSquareBishops,
                rooks,
                queens
        );
    }

    private boolean isLightSquare(
            Square square
    ) {

        return (
                square.file()
                        + square.rank()
        ) % 2 == 0;
    }
}