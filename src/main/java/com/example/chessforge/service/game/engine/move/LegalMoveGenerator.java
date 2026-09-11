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

            GameState simulation =
                    state.copy();

            moveApplier.apply(
                    simulation,
                    move
            );

            /*
             * MoveApplier switches sideToMove,
             * therefore we explicitly check
             * the color that made the move.
             */
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
}