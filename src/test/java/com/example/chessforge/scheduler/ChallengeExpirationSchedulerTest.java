package com.example.chessforge.scheduler;

import com.example.chessforge.service.ChallengeService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ChallengeExpirationSchedulerTest {

    @Test
    void expireChallengesShouldDelegateToService() {

        ChallengeService challengeService =
                mock(ChallengeService.class);

        ChallengeExpirationScheduler scheduler =
                new ChallengeExpirationScheduler(
                        challengeService
                );

        scheduler.expireChallenges();

        verify(challengeService).expireChallenges();
    }
}