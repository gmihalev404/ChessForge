package com.example.chessforge.service.game.dto;

import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.timeControl.TimeControl;

import java.time.LocalDateTime;

public record GameSummaryResponse(
        Long gameId,
        Long opponentId,
        String opponentUsername,
        boolean white,
        TimeControl timeControl,
        boolean rated,
        GameStatus status,
        GameResult result,
        LocalDateTime startedAt
) {
}