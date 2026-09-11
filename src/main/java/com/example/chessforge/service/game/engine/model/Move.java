package com.example.chessforge.service.game.engine.model;

import java.util.Objects;

public record Move(
        Square from,
        Square to,
        MoveType type,
        PieceType promotion
) {

    public Move {

        Objects.requireNonNull(
                from,
                "Move source square cannot be null."
        );

        Objects.requireNonNull(
                to,
                "Move destination square cannot be null."
        );

        Objects.requireNonNull(
                type,
                "Move type cannot be null."
        );

        if (from.equals(to)) {

            throw new IllegalArgumentException(
                    "Move source and destination cannot be the same."
            );
        }

        if (type == MoveType.PROMOTION) {

            validatePromotionPiece(
                    promotion
            );

        } else if (promotion != null) {

            throw new IllegalArgumentException(
                    "Only a promotion move can specify a promotion piece."
            );
        }
    }

    public Move(
            Square from,
            Square to
    ) {

        this(
                from,
                to,
                MoveType.NORMAL,
                null
        );
    }

    public static Move normal(
            String from,
            String to
    ) {

        return new Move(
                Square.fromAlgebraic(from),
                Square.fromAlgebraic(to)
        );
    }

    public static Move promotion(
            Square from,
            Square to,
            PieceType promotion
    ) {

        return new Move(
                from,
                to,
                MoveType.PROMOTION,
                promotion
        );
    }

    private static void validatePromotionPiece(
            PieceType promotion
    ) {

        if (promotion == null) {

            throw new IllegalArgumentException(
                    "Promotion move must specify a promotion piece."
            );
        }

        if (promotion != PieceType.QUEEN
                && promotion != PieceType.ROOK
                && promotion != PieceType.BISHOP
                && promotion != PieceType.KNIGHT) {

            throw new IllegalArgumentException(
                    "Pawn can only promote to queen, rook, bishop or knight."
            );
        }
    }
}