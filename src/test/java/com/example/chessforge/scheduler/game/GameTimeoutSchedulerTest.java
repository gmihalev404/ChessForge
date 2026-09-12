package com.example.chessforge.scheduler.game;

import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.repository.game.GameRepository;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GameStateResponse;
import com.example.chessforge.service.game.realtime.GameRealtimePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameTimeoutSchedulerTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameService gameService;

    @Spy
    private Clock clock =
            Clock.systemDefaultZone();

    @Mock
    private GameRealtimePublisher realtimePublisher;

    @Mock
    private GameStateResponse gameStateResponse;

    @InjectMocks
    private GameTimeoutScheduler scheduler;

    @Test
    void shouldFinalizeExpiredGames() {

        when(gameRepository.findExpiredGameIds(
                any(),
                any(),
                any()
        )).thenReturn(
                List.of(
                        11L,
                        22L,
                        33L
                )
        );

        scheduler.finalizeExpiredGames();

        verify(gameService)
                .finalizeTimeoutIfExpiredAndGetState(11L);

        verify(gameService)
                .finalizeTimeoutIfExpiredAndGetState(22L);

        verify(gameService)
                .finalizeTimeoutIfExpiredAndGetState(33L);
    }

    @Test
    void shouldDoNothingWhenThereAreNoExpiredGames() {

        mockCurrentTime(
                "2026-09-12T12:00:00Z"
        );

        when(gameRepository.findExpiredGameIds(
                eq(GameStatus.IN_PROGRESS),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(
                List.of()
        );

        scheduler.finalizeExpiredGames();

        verifyNoInteractions(
                gameService
        );
    }

    @Test
    void shouldSearchOnlyForInProgressGames() {

        mockCurrentTime(
                "2026-09-12T12:00:00Z"
        );

        when(gameRepository.findExpiredGameIds(
                eq(GameStatus.IN_PROGRESS),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(
                List.of()
        );

        scheduler.finalizeExpiredGames();

        verify(gameRepository)
                .findExpiredGameIds(
                        eq(GameStatus.IN_PROGRESS),
                        any(LocalDateTime.class),
                        any(Pageable.class)
                );
    }

    @Test
    void finalizeExpiredGamesShouldNotBroadcastWhenGameWasNotFinalized() {

        Long gameId =
                1L;

        when(gameRepository.findExpiredGameIds(
                any(),
                any(),
                any()
        )).thenReturn(
                List.of(
                        gameId
                )
        );

        when(gameService
                .finalizeTimeoutIfExpiredAndGetState(
                        gameId
                ))
                .thenReturn(
                        Optional.empty()
                );

        scheduler.finalizeExpiredGames();

        verify(gameService)
                .finalizeTimeoutIfExpiredAndGetState(
                        gameId
                );

        verifyNoInteractions(
                realtimePublisher
        );
    }

    private void mockCurrentTime(
            String instant
    ) {

        doReturn(
                ZoneId.of("UTC")
        ).when(clock)
                .getZone();

        doReturn(
                Instant.parse(instant)
        ).when(clock)
                .instant();
    }
}