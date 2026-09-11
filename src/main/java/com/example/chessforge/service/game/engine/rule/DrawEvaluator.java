package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.history.RepetitionTracker;
import com.example.chessforge.service.game.engine.model.DrawReason;
import com.example.chessforge.service.game.engine.model.GameState;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class DrawEvaluator {

    private final PositionEvaluator positionEvaluator;

    public DrawEvaluator(
            PositionEvaluator positionEvaluator
    ) {

        this.positionEvaluator =
                Objects.requireNonNull(
                        positionEvaluator,
                        "Position evaluator cannot be null."
                );
    }

    // =========================================================
    // AUTOMATIC DRAW
    // =========================================================

    public Set<DrawReason> getAutomaticDrawReasons(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        validate(
                state,
                repetitionTracker
        );

        EnumSet<DrawReason> reasons =
                EnumSet.noneOf(
                        DrawReason.class
                );

        if (positionEvaluator.isStalemate(
                state
        )) {

            reasons.add(
                    DrawReason.STALEMATE
            );
        }

        if (positionEvaluator.isInsufficientMaterial(
                state
        )) {

            reasons.add(
                    DrawReason.INSUFFICIENT_MATERIAL
            );
        }

        if (repetitionTracker.isFivefoldRepetition(
                state
        )) {

            reasons.add(
                    DrawReason.FIVEFOLD_REPETITION
            );
        }

        return Set.copyOf(
                reasons
        );
    }

    public boolean isAutomaticDraw(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        return !getAutomaticDrawReasons(
                state,
                repetitionTracker
        ).isEmpty();
    }

    // =========================================================
    // CLAIMABLE DRAW
    // =========================================================

    public Set<DrawReason> getClaimableDrawReasons(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        validate(
                state,
                repetitionTracker
        );

        EnumSet<DrawReason> reasons =
                EnumSet.noneOf(
                        DrawReason.class
                );

        if (repetitionTracker.isThreefoldRepetition(
                state
        )) {

            reasons.add(
                    DrawReason.THREEFOLD_REPETITION
            );
        }

        if (positionEvaluator.isFiftyMoveRuleDraw(
                state
        )) {

            reasons.add(
                    DrawReason.FIFTY_MOVE_RULE
            );
        }

        return Set.copyOf(
                reasons
        );
    }

    public boolean canClaimDraw(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        return !getClaimableDrawReasons(
                state,
                repetitionTracker
        ).isEmpty();
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validate(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                repetitionTracker,
                "Repetition tracker cannot be null."
        );
    }
}