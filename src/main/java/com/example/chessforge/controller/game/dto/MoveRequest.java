package com.example.chessforge.controller.game.dto;

import com.example.chessforge.service.game.engine.model.PieceType;

public record MoveRequest(
        String from,
        String to,
        PieceType promotion
) {
}