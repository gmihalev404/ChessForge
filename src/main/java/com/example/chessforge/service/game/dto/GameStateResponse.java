package com.example.chessforge.service.game.dto;

import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.game.GameTermination;
import com.example.chessforge.model.enums.timeControl.TimeControl;

import java.time.LocalDateTime;

public record GameStateResponse(
        Long gameId,

        Long whitePlayerId,
        String whiteUsername,

        Long blackPlayerId,
        String blackUsername,

        TimeControl timeControl,
        boolean rated,

        GameStatus status,
        GameResult result,
        GameTermination termination,

        String currentFen,

        Long whiteTimeRemainingMillis,
        Long blackTimeRemainingMillis,

        LocalDateTime turnStartedAt,
        LocalDateTime turnExpiresAt,

        Long drawOfferByUserId,

        String pgn
) {
}