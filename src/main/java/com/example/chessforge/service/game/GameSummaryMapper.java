package com.example.chessforge.service.game;

import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.dto.GameSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class GameSummaryMapper {

    public GameSummaryResponse toResponse(
            Game game,
            User currentUser
    ) {

        boolean currentUserIsWhite =
                game.getWhitePlayer()
                        .getId()
                        .equals(
                                currentUser.getId()
                        );

        User opponent =
                currentUserIsWhite
                        ? game.getBlackPlayer()
                        : game.getWhitePlayer();

        return new GameSummaryResponse(
                game.getId(),
                opponent.getId(),
                opponent.getUsername(),
                currentUserIsWhite,
                game.getTimeControl(),
                game.isRated(),
                game.getStatus(),
                game.getResult(),
                game.getStartedAt()
        );
    }
}