package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.history.PositionKeyFactory;
import com.example.chessforge.service.game.engine.history.RepetitionTracker;
import com.example.chessforge.service.game.engine.model.DrawReason;
import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawEvaluatorTest {

    private DrawEvaluator drawEvaluator;
    private PositionKeyFactory positionKeyFactory;

    @BeforeEach
    void setUp() {

        MoveGenerator moveGenerator =
                new MoveGenerator();

        MoveApplier moveApplier =
                new MoveApplier();

        AttackDetector attackDetector =
                new AttackDetector();

        LegalMoveGenerator legalMoveGenerator =
                new LegalMoveGenerator(
                        moveGenerator,
                        moveApplier,
                        attackDetector
                );

        PositionEvaluator positionEvaluator =
                new PositionEvaluator(
                        legalMoveGenerator,
                        attackDetector
                );

        positionKeyFactory =
                new PositionKeyFactory(
                        legalMoveGenerator
                );

        drawEvaluator =
                new DrawEvaluator(
                        positionEvaluator
                );
    }

    @Test
    void threefoldRepetitionShouldBeClaimableDraw() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        tracker.recordPosition(state);
        tracker.recordPosition(state);

        assertTrue(
                drawEvaluator
                        .canClaimDraw(
                                state,
                                tracker
                        )
        );

        assertTrue(
                drawEvaluator
                        .getClaimableDrawReasons(
                                state,
                                tracker
                        )
                        .contains(
                                DrawReason.THREEFOLD_REPETITION
                        )
        );

        assertFalse(
                drawEvaluator
                        .isAutomaticDraw(
                                state,
                                tracker
                        )
        );
    }

    @Test
    void fivefoldRepetitionShouldBeAutomaticDraw() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        tracker.recordPosition(state);
        tracker.recordPosition(state);
        tracker.recordPosition(state);
        tracker.recordPosition(state);

        assertTrue(
                drawEvaluator
                        .isAutomaticDraw(
                                state,
                                tracker
                        )
        );

        assertTrue(
                drawEvaluator
                        .getAutomaticDrawReasons(
                                state,
                                tracker
                        )
                        .contains(
                                DrawReason.FIVEFOLD_REPETITION
                        )
        );
    }

    @Test
    void fiftyMoveRuleShouldBeClaimableDraw() {

        GameState state =
                GameState.initial();

        state.setHalfMoveClock(
                100
        );

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        assertTrue(
                drawEvaluator
                        .getClaimableDrawReasons(
                                state,
                                tracker
                        )
                        .contains(
                                DrawReason.FIFTY_MOVE_RULE
                        )
        );
    }

    @Test
    void initialPositionShouldHaveNoDrawCondition() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        assertFalse(
                drawEvaluator.isAutomaticDraw(
                        state,
                        tracker
                )
        );

        assertFalse(
                drawEvaluator.canClaimDraw(
                        state,
                        tracker
                )
        );
    }
}
