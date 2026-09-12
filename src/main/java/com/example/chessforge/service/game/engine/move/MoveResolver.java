package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.model.Move;
import com.example.chessforge.service.game.engine.model.PieceType;
import com.example.chessforge.service.game.engine.model.Square;

import java.util.Objects;

public class MoveResolver {

    private final LegalMoveGenerator legalMoveGenerator;

    public MoveResolver(
            LegalMoveGenerator legalMoveGenerator
    ) {

        this.legalMoveGenerator =
                Objects.requireNonNull(
                        legalMoveGenerator,
                        "Legal move generator cannot be null."
                );
    }

    public Move resolve(
            GameState state,
            Square from,
            Square to,
            PieceType promotion
    ) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        Objects.requireNonNull(
                from,
                "Source square cannot be null."
        );

        Objects.requireNonNull(
                to,
                "Target square cannot be null."
        );

        return legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                )
                .stream()
                .filter(move ->
                        move.to().equals(to)
                )
                .filter(move ->
                        Objects.equals(
                                move.promotion(),
                                promotion
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No legal move matches the request."
                        )
                );
    }
}