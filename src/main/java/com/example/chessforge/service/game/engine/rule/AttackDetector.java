package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.model.*;

import java.util.Objects;

public class AttackDetector {

    private static final int[][] KNIGHT_OFFSETS = {
            {1, 2},
            {2, 1},
            {2, -1},
            {1, -2},
            {-1, -2},
            {-2, -1},
            {-2, 1},
            {-1, 2}
    };

    private static final int[][] KING_OFFSETS = {
            {1, 0},
            {1, 1},
            {0, 1},
            {-1, 1},
            {-1, 0},
            {-1, -1},
            {0, -1},
            {1, -1}
    };

    private static final int[][] DIAGONAL_DIRECTIONS = {
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    private static final int[][] STRAIGHT_DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    // =========================================================
    // PUBLIC API
    // =========================================================

    public boolean isSquareAttacked(
            GameState state,
            Square square,
            PieceColor attackerColor
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                square,
                "Square cannot be null."
        );

        Objects.requireNonNull(
                attackerColor,
                "Attacker color cannot be null."
        );

        Board board =
                state.getBoard();

        return isAttackedByPawn(
                board,
                square,
                attackerColor
        )
                || isAttackedByKnight(
                board,
                square,
                attackerColor
        )
                || isAttackedBySlidingPiece(
                board,
                square,
                attackerColor,
                DIAGONAL_DIRECTIONS,
                PieceType.BISHOP
        )
                || isAttackedBySlidingPiece(
                board,
                square,
                attackerColor,
                STRAIGHT_DIRECTIONS,
                PieceType.ROOK
        )
                || isAttackedByKing(
                board,
                square,
                attackerColor
        );
    }

    public boolean isInCheck(
            GameState state,
            PieceColor color
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                color,
                "King color cannot be null."
        );

        Square kingSquare =
                findKing(
                        state.getBoard(),
                        color
                );

        return isSquareAttacked(
                state,
                kingSquare,
                color.opposite()
        );
    }

    // =========================================================
    // PAWN
    // =========================================================

    private boolean isAttackedByPawn(
            Board board,
            Square target,
            PieceColor attackerColor
    ) {

        int attackerRank =
                target.rank()
                        + (
                        attackerColor == PieceColor.WHITE
                                ? -1
                                : 1
                );

        int[] fileOffsets = {
                -1,
                1
        };

        for (int fileOffset : fileOffsets) {

            int attackerFile =
                    target.file()
                            + fileOffset;

            if (!isInsideBoard(
                    attackerFile,
                    attackerRank
            )) {

                continue;
            }

            Piece piece =
                    board.getPiece(
                            new Square(
                                    attackerFile,
                                    attackerRank
                            )
                    );

            if (isPiece(
                    piece,
                    PieceType.PAWN,
                    attackerColor
            )) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // KNIGHT
    // =========================================================

    private boolean isAttackedByKnight(
            Board board,
            Square target,
            PieceColor attackerColor
    ) {

        for (int[] offset : KNIGHT_OFFSETS) {

            int file =
                    target.file()
                            + offset[0];

            int rank =
                    target.rank()
                            + offset[1];

            if (!isInsideBoard(
                    file,
                    rank
            )) {

                continue;
            }

            Piece piece =
                    board.getPiece(
                            new Square(
                                    file,
                                    rank
                            )
                    );

            if (isPiece(
                    piece,
                    PieceType.KNIGHT,
                    attackerColor
            )) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // SLIDING PIECES
    // =========================================================

    private boolean isAttackedBySlidingPiece(
            Board board,
            Square target,
            PieceColor attackerColor,
            int[][] directions,
            PieceType attackingType
    ) {

        for (int[] direction : directions) {

            int file =
                    target.file()
                            + direction[0];

            int rank =
                    target.rank()
                            + direction[1];

            while (isInsideBoard(
                    file,
                    rank
            )) {

                Piece piece =
                        board.getPiece(
                                new Square(
                                        file,
                                        rank
                                )
                        );

                if (piece == null) {

                    file += direction[0];
                    rank += direction[1];

                    continue;
                }

                if (piece.color()
                        != attackerColor) {

                    break;
                }

                if (piece.type()
                        == attackingType
                        || piece.type()
                        == PieceType.QUEEN) {

                    return true;
                }

                break;
            }
        }

        return false;
    }

    // =========================================================
    // KING
    // =========================================================

    private boolean isAttackedByKing(
            Board board,
            Square target,
            PieceColor attackerColor
    ) {

        for (int[] offset : KING_OFFSETS) {

            int file =
                    target.file()
                            + offset[0];

            int rank =
                    target.rank()
                            + offset[1];

            if (!isInsideBoard(
                    file,
                    rank
            )) {

                continue;
            }

            Piece piece =
                    board.getPiece(
                            new Square(
                                    file,
                                    rank
                            )
                    );

            if (isPiece(
                    piece,
                    PieceType.KING,
                    attackerColor
            )) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // KING LOCATION
    // =========================================================

    private Square findKing(
            Board board,
            PieceColor color
    ) {

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

                if (isPiece(
                        piece,
                        PieceType.KING,
                        color
                )) {

                    return square;
                }
            }
        }

        throw new IllegalStateException(
                "No "
                        + color
                        + " king exists on the board."
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean isPiece(
            Piece piece,
            PieceType type,
            PieceColor color
    ) {

        return piece != null
                && piece.type() == type
                && piece.color() == color;
    }

    private boolean isInsideBoard(
            int file,
            int rank
    ) {

        return file >= 0
                && file < Square.BOARD_SIZE
                && rank >= 0
                && rank < Square.BOARD_SIZE;
    }
}