package com.example.chessforge.service.game;

import com.example.chessforge.service.game.dto.GameStateResponse;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class GameStateMapper {

    public GameStateResponse toResponse(
            Game game
    ) {

        User whitePlayer =
                game.getWhitePlayer();

        User blackPlayer =
                game.getBlackPlayer();

        User drawOfferBy =
                game.getDrawOfferBy();

        return new GameStateResponse(
                game.getId(),

                whitePlayer.getId(),
                whitePlayer.getUsername(),

                blackPlayer.getId(),
                blackPlayer.getUsername(),

                game.getTimeControl(),
                game.isRated(),

                game.getStatus(),
                game.getResult(),
                game.getTermination(),

                game.getCurrentFen(),

                game.getWhiteTimeRemainingMillis(),
                game.getBlackTimeRemainingMillis(),

                game.getTurnStartedAt(),
                game.getTurnExpiresAt(),

                drawOfferBy == null
                        ? null
                        : drawOfferBy.getId(),

                game.getPgn()
        );
    }
}