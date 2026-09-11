package com.example.chessforge.service.game.engine.model;

import java.util.Objects;

public record Piece(
        PieceType type,
        PieceColor color
) {

    public Piece {

        Objects.requireNonNull(
                type,
                "Piece type cannot be null."
        );

        Objects.requireNonNull(
                color,
                "Piece color cannot be null."
        );
    }
}