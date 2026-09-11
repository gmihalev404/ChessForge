package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.model.PieceColor;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;

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
}