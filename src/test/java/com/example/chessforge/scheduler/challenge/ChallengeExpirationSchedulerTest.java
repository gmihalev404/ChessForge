package com.example.chessforge.scheduler.challenge;

import com.example.chessforge.service.challenge.ChallengeService;
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