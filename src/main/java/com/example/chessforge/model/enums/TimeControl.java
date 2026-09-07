package com.example.chessforge.model.enums;

import lombok.Getter;

@Getter
public enum TimeControl {

    BULLET_1_0(60, 0, TimeControlType.BULLET),
    BULLET_2_1(120, 1, TimeControlType.BULLET),

    BLITZ_3_0(180, 0, TimeControlType.BLITZ),
    BLITZ_3_2(180, 2, TimeControlType.BLITZ),
    BLITZ_5_0(300, 0, TimeControlType.BLITZ),

    RAPID_10_0(600, 0, TimeControlType.RAPID),
    RAPID_10_5(600, 5, TimeControlType.RAPID),
    RAPID_15_10(900, 10, TimeControlType.RAPID),

    CLASSICAL_30_0(1800, 0, TimeControlType.CLASSICAL);

    private final int initialTimeSeconds;
    private final int incrementSeconds;
    private final TimeControlType type;

    TimeControl(
            int initialTimeSeconds,
            int incrementSeconds,
            TimeControlType type
    ) {
        this.initialTimeSeconds = initialTimeSeconds;
        this.incrementSeconds = incrementSeconds;
        this.type = type;
    }

}