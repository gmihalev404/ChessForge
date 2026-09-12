package com.example.chessforge.service.game.realtime;

import com.example.chessforge.service.game.dto.GameStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class GameRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(
            Long gameId,
            GameStateResponse response
    ) {

        Objects.requireNonNull(
                gameId,
                "Game id cannot be null."
        );

        Objects.requireNonNull(
                response,
                "Game state response cannot be null."
        );

        messagingTemplate.convertAndSend(
                "/topic/games/" + gameId,
                response
        );
    }
}