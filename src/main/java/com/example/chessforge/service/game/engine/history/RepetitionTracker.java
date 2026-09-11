package com.example.chessforge.service.game.engine.history;

import com.example.chessforge.service.game.engine.model.GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RepetitionTracker {

    private final PositionKeyFactory positionKeyFactory;

    private final Map<PositionKey, Integer> repetitionCounts;

    public RepetitionTracker(
            PositionKeyFactory positionKeyFactory,
            GameState initialState
    ) {

        this.positionKeyFactory =
                Objects.requireNonNull(
                        positionKeyFactory,
                        "Position key factory cannot be null."
                );

        Objects.requireNonNull(
                initialState,
                "Initial game state cannot be null."
        );

        this.repetitionCounts =
                new HashMap<>();

        recordPosition(
                initialState
        );
    }

    // =========================================================
    // RECORDING
    // =========================================================

    public int recordPosition(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        PositionKey key =
                positionKeyFactory.create(
                        state
                );

        return repetitionCounts.merge(
                key,
                1,
                Integer::sum
        );
    }

    // =========================================================
    // QUERY
    // =========================================================

    public int getRepetitionCount(
            GameState state
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        PositionKey key =
                positionKeyFactory.create(
                        state
                );

        return repetitionCounts.getOrDefault(
                key,
                0
        );
    }

    public boolean isThreefoldRepetition(
            GameState state
    ) {

        return getRepetitionCount(
                state
        ) >= 3;
    }

    public boolean isFivefoldRepetition(
            GameState state
    ) {

        return getRepetitionCount(
                state
        ) >= 5;
    }
}