package com.example.chessforge.service.game.notation;

import com.example.chessforge.model.entity.game.GameMove;
import com.example.chessforge.model.enums.game.GameResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class PgnGenerator {

    public String generate(
            List<GameMove> moves,
            GameResult result
    ) {

        Objects.requireNonNull(
                moves,
                "Moves cannot be null."
        );

        Objects.requireNonNull(
                result,
                "Game result cannot be null."
        );

        StringBuilder pgn =
                new StringBuilder();

        for (int i = 0;
             i < moves.size();
             i++) {

            GameMove move =
                    moves.get(i);

            if (move.getSan() == null
                    || move.getSan().isBlank()) {

                throw new IllegalArgumentException(
                        "Every move must contain SAN notation."
                );
            }

            if (i % 2 == 0) {

                int moveNumber =
                        i / 2 + 1;

                pgn.append(
                        moveNumber
                );

                pgn.append(". ");
            }

            pgn.append(
                    move.getSan()
            );

            pgn.append(" ");
        }

        pgn.append(
                resultNotation(
                        result
                )
        );

        return pgn.toString();
    }

    private String resultNotation(
            GameResult result
    ) {

        return switch (result) {

            case WHITE_WIN ->
                    "1-0";

            case BLACK_WIN ->
                    "0-1";

            case DRAW ->
                    "1/2-1/2";
        };
    }
}