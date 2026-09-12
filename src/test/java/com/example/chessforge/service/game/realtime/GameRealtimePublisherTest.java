package com.example.chessforge.service.game.realtime;

import com.example.chessforge.service.game.dto.GameStateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameRealtimePublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameStateResponse response;

    private GameRealtimePublisher publisher;

    @BeforeEach
    void setUp() {

        publisher =
                new GameRealtimePublisher(
                        messagingTemplate
                );
    }

    @Test
    void publishShouldBroadcastGameState() {

        publisher.publish(
                12L,
                response
        );

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/games/12",
                        response
                );
    }

    @Test
    void publishShouldRejectMissingGameId() {

        assertThrows(
                NullPointerException.class,
                () ->
                        publisher.publish(
                                null,
                                response
                        )
        );

        verifyNoInteractions(
                messagingTemplate
        );
    }

    @Test
    void publishShouldRejectMissingResponse() {

        assertThrows(
                NullPointerException.class,
                () ->
                        publisher.publish(
                                12L,
                                null
                        )
        );

        verifyNoInteractions(
                messagingTemplate
        );
    }
}