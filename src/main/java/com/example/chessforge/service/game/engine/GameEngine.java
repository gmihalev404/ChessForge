package com.example.chessforge.service.game.engine;

import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.model.Move;
import com.example.chessforge.service.game.engine.model.PieceColor;
import com.example.chessforge.service.game.engine.model.Square;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class GameEngine {

    private final LegalMoveGenerator legalMoveGenerator;
    private final MoveApplier moveApplier;
    private final AttackDetector attackDetector;
    private final PositionEvaluator positionEvaluator;

    public GameEngine(
            LegalMoveGenerator legalMoveGenerator,
            MoveApplier moveApplier,
            AttackDetector attackDetector,
            PositionEvaluator positionEvaluator
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

    public boolean isStalemate(
            GameState state
    ) {

        return positionEvaluator
                .isStalemate(state);
    }
}