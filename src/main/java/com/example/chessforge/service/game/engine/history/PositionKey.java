package com.example.chessforge.service.game.engine.history;

import com.example.chessforge.service.game.engine.model.PieceColor;
import com.example.chessforge.service.game.engine.model.Square;

import java.util.Objects;

public record PositionKey(
        String boardSignature,
        PieceColor sideToMove,
        boolean whiteKingSideCastlingAllowed,
        boolean whiteQueenSideCastlingAllowed,
        boolean blackKingSideCastlingAllowed,
        boolean blackQueenSideCastlingAllowed,
        Square enPassantTarget
) {

    public PositionKey {

        Objects.requireNonNull(
                boardSignature,
                "Board signature cannot be null."
        );

        Objects.requireNonNull(
                sideToMove,
                "Side to move cannot be null."
        );
    }
}