package com.example.chessforge.service.game.engine.history;

import com.example.chessforge.service.game.engine.GameEngine;
import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveResolver;
import com.example.chessforge.service.game.engine.notation.FenConverter;
import com.example.chessforge.service.game.engine.notation.SanGenerator;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.DrawEvaluator;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepetitionTrackerTest {

    private PositionKeyFactory positionKeyFactory;
    private GameEngine gameEngine;

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
        DrawEvaluator drawEvaluator = new DrawEvaluator(positionEvaluator);

        SanGenerator sanGenerator = new SanGenerator(legalMoveGenerator,
                attackDetector,
                positionEvaluator);

        positionKeyFactory =
                new PositionKeyFactory(
                        legalMoveGenerator
                );

        FenConverter fenConverter = new FenConverter();

        MoveResolver moveResolver =
                new MoveResolver(
                        legalMoveGenerator
                );

        gameEngine =
                new GameEngine(
                        legalMoveGenerator,
                        moveApplier,
                        attackDetector,
                        positionEvaluator,
                        drawEvaluator,
                        fenConverter,
                        positionKeyFactory,
                        sanGenerator,
                        moveResolver
                );
    }

    @Test
    void initialPositionShouldBeRecordedOnce() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        assertEquals(
                1,
                tracker.getRepetitionCount(
                        state
                )
        );

        assertFalse(
                tracker.isThreefoldRepetition(
                        state
                )
        );
    }

    @Test
    void recordingSamePositionThreeTimesShouldDetectThreefoldRepetition() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        tracker.recordPosition(state);
        tracker.recordPosition(state);

        assertEquals(
                3,
                tracker.getRepetitionCount(
                        state
                )
        );

        assertTrue(
                tracker.isThreefoldRepetition(
                        state
                )
        );

        assertFalse(
                tracker.isFivefoldRepetition(
                        state
                )
        );
    }

    @Test
    void differentMoveCountersShouldStillCountAsSamePosition() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        state.setHalfMoveClock(17);
        state.setFullMoveNumber(9);

        tracker.recordPosition(
                state
        );

        assertEquals(
                2,
                tracker.getRepetitionCount(
                        state
                )
        );
    }

    @Test
    void differentSideToMoveShouldNotCountAsSamePosition() {

        GameState initial =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        initial
                );

        GameState different =
                initial.copy();

        different.setSideToMove(
                PieceColor.BLACK
        );

        tracker.recordPosition(
                different
        );

        assertEquals(
                1,
                tracker.getRepetitionCount(
                        initial
                )
        );

        assertEquals(
                1,
                tracker.getRepetitionCount(
                        different
                )
        );
    }

    @Test
    void repeatedKnightCycleShouldProduceThreefoldRepetition() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        playAndRecord(
                state,
                tracker,
                "g1",
                "f3"
        );

        playAndRecord(
                state,
                tracker,
                "g8",
                "f6"
        );

        playAndRecord(
                state,
                tracker,
                "f3",
                "g1"
        );

        playAndRecord(
                state,
                tracker,
                "f6",
                "g8"
        );

        assertEquals(
                2,
                tracker.getRepetitionCount(
                        state
                )
        );

        assertFalse(
                tracker.isThreefoldRepetition(
                        state
                )
        );

        // Second cycle.

        playAndRecord(
                state,
                tracker,
                "g1",
                "f3"
        );

        playAndRecord(
                state,
                tracker,
                "g8",
                "f6"
        );

        playAndRecord(
                state,
                tracker,
                "f3",
                "g1"
        );

        playAndRecord(
                state,
                tracker,
                "f6",
                "g8"
        );

        assertEquals(
                3,
                tracker.getRepetitionCount(
                        state
                )
        );

        assertTrue(
                tracker.isThreefoldRepetition(
                        state
                )
        );
    }

    @Test
    void fifthOccurrenceShouldDetectFivefoldRepetition() {

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

        assertFalse(
                tracker.isFivefoldRepetition(
                        state
                )
        );

        tracker.recordPosition(state);

        assertEquals(
                5,
                tracker.getRepetitionCount(
                        state
                )
        );

        assertTrue(
                tracker.isFivefoldRepetition(
                        state
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void playAndRecord(
            GameState state,
            RepetitionTracker tracker,
            String from,
            String to
    ) {

        gameEngine.makeMove(
                state,
                Move.normal(
                        from,
                        to
                )
        );

        tracker.recordPosition(
                state
        );
    }
}
