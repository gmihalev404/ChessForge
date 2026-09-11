package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MoveGenerator {

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

    private static final int[][] BISHOP_DIRECTIONS = {
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    private static final int[][] ROOK_DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private static final int[][] QUEEN_DIRECTIONS = {
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1},

            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
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

    private static final int WHITE_PAWN_DIRECTION = 1;
    private static final int BLACK_PAWN_DIRECTION = -1;

    private static final int WHITE_PAWN_START_RANK = 1;
    private static final int BLACK_PAWN_START_RANK = 6;

    private static final int WHITE_PROMOTION_RANK = 7;
    private static final int BLACK_PROMOTION_RANK = 0;

    private static final PieceType[] PROMOTION_PIECES = {
            PieceType.QUEEN,
            PieceType.ROOK,
            PieceType.BISHOP,
            PieceType.KNIGHT
    };

    public List<Move> generatePseudoLegalMoves(
            GameState state,
            Square from
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                from,
                "Source square cannot be null."
        );

        Piece piece =
                state.getBoard()
                        .getPiece(from);

        if (piece == null) {

            return List.of();
        }

        /*
         * We generate moves only for the side
         * whose turn it currently is.
         */
        if (piece.color()
                != state.getSideToMove()) {

            return List.of();
        }

        return switch (piece.type()) {

            case KNIGHT ->
                    generateKnightMoves(
                            state.getBoard(),
                            from,
                            piece.color()
                    );

            case BISHOP ->
                    generateSlidingMoves(
                            state.getBoard(),
                            from,
                            piece.color(),
                            BISHOP_DIRECTIONS
                    );

            case ROOK ->
                    generateSlidingMoves(
                            state.getBoard(),
                            from,
                            piece.color(),
                            ROOK_DIRECTIONS
                    );

            case QUEEN ->
                    generateSlidingMoves(
                            state.getBoard(),
                            from,
                            piece.color(),
                            QUEEN_DIRECTIONS
                    );

            case KING ->
                    generateKingMoves(
                            state.getBoard(),
                            from,
                            piece.color()
                    );

            case PAWN ->
                    generatePawnMoves(
                            state,
                            from,
                            piece.color()
                    );

            default ->
                    throw new UnsupportedOperationException(
                            "Move generation is not implemented yet for "
                                    + piece.type()
                    );
        };
    }

    // =========================================================
    // KNIGHT
    // =========================================================

    private List<Move> generateKnightMoves(
            Board board,
            Square from,
            PieceColor color
    ) {

        List<Move> moves =
                new ArrayList<>();

        for (int[] offset : KNIGHT_OFFSETS) {

            int targetFile =
                    from.file()
                            + offset[0];

            int targetRank =
                    from.rank()
                            + offset[1];

            if (!isInsideBoard(
                    targetFile,
                    targetRank
            )) {

                continue;
            }

            Square target =
                    new Square(
                            targetFile,
                            targetRank
                    );

            Piece targetPiece =
                    board.getPiece(
                            target
                    );

            /*
             * Empty square:
             * knight may move there.
             */
            if (targetPiece == null) {

                moves.add(
                        new Move(
                                from,
                                target
                        )
                );

                continue;
            }

            /*
             * Enemy piece:
             * knight may capture it.
             */
            if (targetPiece.color()
                    != color) {

                moves.add(
                        new Move(
                                from,
                                target
                        )
                );
            }

            /*
             * Friendly piece:
             * no move is added.
             */
        }

        return List.copyOf(
                moves
        );
    }

    // =========================================================
    // SLIDING PIECES
    // =========================================================

    private List<Move> generateSlidingMoves(
            Board board,
            Square from,
            PieceColor color,
            int[][] directions
    ) {

        List<Move> moves =
                new ArrayList<>();

        for (int[] direction : directions) {

            int file =
                    from.file()
                            + direction[0];

            int rank =
                    from.rank()
                            + direction[1];

            while (isInsideBoard(
                    file,
                    rank
            )) {

                Square target =
                        new Square(
                                file,
                                rank
                        );

                Piece targetPiece =
                        board.getPiece(
                                target
                        );

                // Empty square -> continue sliding.
                if (targetPiece == null) {

                    moves.add(
                            new Move(
                                    from,
                                    target
                            )
                    );

                    file += direction[0];
                    rank += direction[1];

                    continue;
                }

                // Enemy piece -> capture, then stop.
                if (targetPiece.color()
                        != color) {

                    moves.add(
                            new Move(
                                    from,
                                    target
                            )
                    );
                }

                /*
                 * Any piece blocks further movement.
                 *
                 * Friendly piece:
                 *      cannot move there.
                 *
                 * Enemy piece:
                 *      can capture it,
                 *      but cannot move beyond it.
                 */
                break;
            }
        }

        return List.copyOf(
                moves
        );
    }

    // =========================================================
    // KING
    // =========================================================

    private List<Move> generateKingMoves(
            Board board,
            Square from,
            PieceColor color
    ) {

        List<Move> moves =
                new ArrayList<>();

        for (int[] offset : KING_OFFSETS) {

            int targetFile =
                    from.file()
                            + offset[0];

            int targetRank =
                    from.rank()
                            + offset[1];

            if (!isInsideBoard(
                    targetFile,
                    targetRank
            )) {

                continue;
            }

            Square target =
                    new Square(
                            targetFile,
                            targetRank
                    );

            Piece targetPiece =
                    board.getPiece(
                            target
                    );

            if (targetPiece == null
                    || targetPiece.color()
                    != color) {

                moves.add(
                        new Move(
                                from,
                                target
                        )
                );
            }
        }

        return List.copyOf(
                moves
        );
    }

    // =========================================================
    // PAWN
    // =========================================================

    private List<Move> generatePawnMoves(
            GameState state,
            Square from,
            PieceColor color
    ) {

        List<Move> moves =
                new ArrayList<>();

        Board board =
                state.getBoard();

        int direction =
                pawnDirection(
                        color
                );

        generatePawnForwardMoves(
                board,
                from,
                color,
                direction,
                moves
        );

        generatePawnCaptures(
                state,
                from,
                color,
                direction,
                moves
        );

        return List.copyOf(
                moves
        );
    }

    // =========================================================
    // BOARD
    // =========================================================

    private boolean isInsideBoard(
            int file,
            int rank
    ) {

        return file >= 0
                && file < Square.BOARD_SIZE
                && rank >= 0
                && rank < Square.BOARD_SIZE;
    }


    // =========================================================
    // HELPERS
    // =========================================================
    private void generatePawnForwardMoves(
            Board board,
            Square from,
            PieceColor color,
            int direction,
            List<Move> moves
    ) {

        int oneStepRank =
                from.rank()
                        + direction;

        if (!isInsideBoard(
                from.file(),
                oneStepRank
        )) {

            return;
        }

        Square oneStep =
                new Square(
                        from.file(),
                        oneStepRank
                );

        /*
         * A pawn cannot move forward
         * through or onto another piece.
         */
        if (!board.isEmpty(oneStep)) {

            return;
        }

        addPawnMove(
                moves,
                from,
                oneStep,
                color
        );

        /*
         * Two-square move is possible only
         * from the pawn's starting rank.
         */
        if (from.rank()
                != pawnStartRank(color)) {

            return;
        }

        int twoStepRank =
                from.rank()
                        + 2 * direction;

        Square twoStep =
                new Square(
                        from.file(),
                        twoStepRank
                );

        if (board.isEmpty(twoStep)) {

            moves.add(
                    new Move(
                            from,
                            twoStep
                    )
            );
        }
    }

    private void generatePawnCaptures(
            GameState state,
            Square from,
            PieceColor color,
            int direction,
            List<Move> moves
    ) {

        int targetRank =
                from.rank()
                        + direction;

        if (targetRank < 0
                || targetRank >= Square.BOARD_SIZE) {

            return;
        }

        int[] fileOffsets = {
                -1,
                1
        };

        for (int fileOffset : fileOffsets) {

            int targetFile =
                    from.file()
                            + fileOffset;

            if (!isInsideBoard(
                    targetFile,
                    targetRank
            )) {

                continue;
            }

            Square target =
                    new Square(
                            targetFile,
                            targetRank
                    );

            Piece targetPiece =
                    state.getBoard()
                            .getPiece(target);

            // Normal diagonal capture.
            if (targetPiece != null
                    && targetPiece.color()
                    != color) {

                addPawnMove(
                        moves,
                        from,
                        target,
                        color
                );

                continue;
            }

            // Special diagonal capture.
            if (isValidEnPassant(
                    state,
                    target,
                    color,
                    direction
            )) {

                moves.add(
                        new Move(
                                from,
                                target,
                                MoveType.EN_PASSANT,
                                null
                        )
                );
            }
        }
    }

    private void addPawnMove(
            List<Move> moves,
            Square from,
            Square to,
            PieceColor color
    ) {

        if (to.rank()
                == promotionRank(color)) {

            for (PieceType promotionPiece :
                    PROMOTION_PIECES) {

                moves.add(
                        Move.promotion(
                                from,
                                to,
                                promotionPiece
                        )
                );
            }

            return;
        }

        moves.add(
                new Move(
                        from,
                        to
                )
        );
    }

    private boolean isValidEnPassant(
            GameState state,
            Square target,
            PieceColor color,
            int direction
    ) {

        Square enPassantTarget =
                state.getEnPassantTarget();

        if (enPassantTarget == null
                || !enPassantTarget.equals(target)) {

            return false;
        }

        /*
         * En passant destination itself
         * must be empty.
         */
        if (!state.getBoard()
                .isEmpty(target)) {

            return false;
        }

        Square capturedPawnSquare =
                new Square(
                        target.file(),
                        target.rank()
                                - direction
                );

        Piece capturedPiece =
                state.getBoard()
                        .getPiece(
                                capturedPawnSquare
                        );

        return capturedPiece != null
                && capturedPiece.type()
                == PieceType.PAWN
                && capturedPiece.color()
                != color;
    }

    private int pawnDirection(
            PieceColor color
    ) {

        return color == PieceColor.WHITE
                ? WHITE_PAWN_DIRECTION
                : BLACK_PAWN_DIRECTION;
    }

    private int pawnStartRank(
            PieceColor color
    ) {

        return color == PieceColor.WHITE
                ? WHITE_PAWN_START_RANK
                : BLACK_PAWN_START_RANK;
    }

    private int promotionRank(
            PieceColor color
    ) {

        return color == PieceColor.WHITE
                ? WHITE_PROMOTION_RANK
                : BLACK_PROMOTION_RANK;
    }
}