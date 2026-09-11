package com.example.chessforge.service.game.engine;

import com.example.chessforge.service.game.engine.history.RepetitionTracker;
import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.DrawEvaluator;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class GameEngine {

    private final LegalMoveGenerator legalMoveGenerator;
    private final MoveApplier moveApplier;
    private final AttackDetector attackDetector;
    private final PositionEvaluator positionEvaluator;

    private final DrawEvaluator drawEvaluator;

    public GameEngine(
            LegalMoveGenerator legalMoveGenerator,
            MoveApplier moveApplier,
            AttackDetector attackDetector,
            PositionEvaluator positionEvaluator,
            DrawEvaluator drawEvaluator
    ) {

        this.legalMoveGenerator =
                Objects.requireNonNull(
                        legalMoveGenerator,
                        "Legal move generator cannot be null."
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

        this.positionEvaluator =
                Objects.requireNonNull(
                        positionEvaluator,
                        "Position evaluator cannot be null."
                );
        this.drawEvaluator = drawEvaluator;
    }

    // =========================================================
    // LEGAL MOVES
    // =========================================================

    public List<Move> getLegalMoves(
            GameState state,
            Square from
    ) {

        return legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                );
    }

    public List<Move> getAllLegalMoves(
            GameState state
    ) {

        return legalMoveGenerator
                .generateAllLegalMoves(
                        state
                );
    }

    public boolean isLegalMove(
            GameState state,
            Move move
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                move,
                "Move cannot be null."
        );

        return legalMoveGenerator
                .generateLegalMoves(
                        state,
                        move.from()
                )
                .contains(move);
    }

    // =========================================================
    // MOVE EXECUTION
    // =========================================================

    public void makeMove(
            GameState state,
            Move move
    ) {

        if (!isLegalMove(
                state,
                move
        )) {

            throw new IllegalArgumentException(
                    "Illegal chess move: "
                            + move
            );
        }

        moveApplier.apply(
                state,
                move
        );
    }

    // =========================================================
    // POSITION STATE
    // =========================================================

    public boolean isInCheck(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        return attackDetector.isInCheck(
                state,
                state.getSideToMove()
        );
    }

    public boolean isInCheck(
            GameState state,
            PieceColor color
    ) {

        return attackDetector.isInCheck(
                state,
                color
        );
    }

    public boolean isCheckmate(
            GameState state
    ) {

        return positionEvaluator
                .isCheckmate(state);
    }

    // =========================================================
    // DRAW
    // =========================================================

    public boolean isStalemate(
            GameState state
    ) {

        return positionEvaluator
                .isStalemate(state);
    }

    public boolean isInsufficientMaterial(
            GameState state
    ) {

        return positionEvaluator
                .isInsufficientMaterial(
                        state
                );
    }

    public boolean isFiftyMoveRuleDraw(
            GameState state
    ) {

        return positionEvaluator
                .isFiftyMoveRuleDraw(
                        state
                );
    }

    public Set<DrawReason> getAutomaticDrawReasons(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        return drawEvaluator
                .getAutomaticDrawReasons(
                        state,
                        repetitionTracker
                );
    }

    public Set<DrawReason> getClaimableDrawReasons(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        return drawEvaluator
                .getClaimableDrawReasons(
                        state,
                        repetitionTracker
                );
    }

    public boolean isAutomaticDraw(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        return drawEvaluator
                .isAutomaticDraw(
                        state,
                        repetitionTracker
                );
    }

    public boolean canClaimDraw(
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        return drawEvaluator
                .canClaimDraw(
                        state,
                        repetitionTracker
                );
    }
}