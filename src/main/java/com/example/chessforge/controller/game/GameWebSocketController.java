package com.example.chessforge.controller.game;

import com.example.chessforge.controller.game.dto.MoveRequest;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GameStateResponse;
import com.example.chessforge.service.game.realtime.GameRealtimePublisher;
import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final UserService userService;
    private final GameRealtimePublisher realtimePublisher;
    @MessageMapping("/games/{gameId}/move")
    public void makeMove(
            @DestinationVariable Long gameId,
            MoveRequest request,
            Principal principal
    ) {

        if (principal == null) {
            throw new IllegalStateException(
                    "Authenticated user is required."
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Move request is required."
            );
        }

        User player =
                userService.findByUsername(
                        principal.getName()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user was not found."
                        )
                );

        GameStateResponse response =
                gameService.makeMoveAndGetState(
                        gameId,
                        player,
                        request.from(),
                        request.to(),
                        request.promotion()
                );

        realtimePublisher.publish(
                gameId,
                response
        );
    }
}