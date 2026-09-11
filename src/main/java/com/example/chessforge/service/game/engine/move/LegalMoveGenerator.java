package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.rule.AttackDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LegalMoveGenerator {

    private final MoveGenerator moveGenerator;
    private final MoveApplier moveApplier;
    private final AttackDetector attackDetector;

    public LegalMoveGenerator(
            MoveGenerator moveGenerator,
            MoveApplier moveApplier,
            AttackDetector attackDetector
    ) {

        this.moveGenerator =
                Objects.requireNonNull(
                        moveGenerator,
                        "Move generator cannot be null."
                );

        this.moveApplier =
                Objects.requireNonNull(
                        moveApplier,
                        "Move applier cannot be null."
                );

        this.attackDetector =
                Objects.requireNonNull(
                        attackDetector,
                        "Attack detector cannot be null."
                );
    }

    // =========================================================
    // MOVES FOR ONE PIECE
    // =========================================================

    public List<Move> generateLegalMoves(
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

        List<Move> pseudoLegalMoves =
                moveGenerator
                        .generatePseudoLegalMoves(
                                state,
                                from
                        );

        if (pseudoLegalMoves.isEmpty()) {

            return List.of();
        }

        PieceColor movingColor =
                state.getSideToMove();

        List<Move> legalMoves =
                new ArrayList<>();

        for (Move move :
                pseudoLegalMoves) {

            if (isCastling(move)
                    && !isCastlingPathSafe(
                    state,
                    move,
                    movingColor
            )) {

                continue;
            }

            GameState simulation =
                    state.copy();

            moveApplier.apply(
                    simulation,
                    move
            );

            if (!attackDetector.isInCheck(
                    simulation,
                    movingColor
            )) {

                legalMoves.add(
                        move
                );
            }
        }

        return List.copyOf(
                legalMoves
        );
    }

    // =========================================================
    // ALL MOVES
    // =========================================================

    public List<Move> generateAllLegalMoves(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        List<Move> moves =
                new ArrayList<>();

        PieceColor sideToMove =
                state.getSideToMove();

        Board board =
                state.getBoard();

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
                        || piece.color()
                        != sideToMove) {

                    continue;
                }

                moves.addAll(
                        generateLegalMoves(
                                state,
                                square
                        )
                );
            }
        }

        return List.copyOf(
                moves
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean isCastling(
            Move move
    ) {

        return move.type()
                == MoveType.CASTLE_KING_SIDE
                || move.type()
                == MoveType.CASTLE_QUEEN_SIDE;
    }

    private boolean isCastlingPathSafe(
            GameState state,
            Move move,
            PieceColor color
    ) {

        /*
         * A king cannot castle while
         * already in check.
         */
        if (attackDetector.isInCheck(
                state,
                color
        )) {

            return false;
        }

        int rank =
                color == PieceColor.WHITE
                        ? 0
                        : 7;

        int transitFile =
                move.type()
                        == MoveType.CASTLE_KING_SIDE
                        ? 5
                        : 3;

        Square transit =
                new Square(
                        transitFile,
                        rank
                );

        /*
         * Simulate the king moving through
         * the intermediate square.
         */
        GameState transitState =
                state.copy();

        Move transitMove =
                new Move(
                        move.from(),
                        transit
                );

        moveApplier.apply(
                transitState,
                transitMove
        );

        return !attackDetector.isInCheck(
                transitState,
                color
        );
    }
}