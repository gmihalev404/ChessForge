package com.example.chessforge.service.challenge;

import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.entity.challenge.Challenge;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.challenge.ChallengeStatus;
import com.example.chessforge.model.enums.challenge.ColorPreference;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.model.enums.user.UserStatus;
import com.example.chessforge.repository.challenge.ChallengeRepository;
import com.example.chessforge.service.challenge.ChallengeService;
import com.example.chessforge.service.game.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private GameService gameService;

    private ChallengeService challengeService;

    private User challenger;
    private User opponent;
    private User thirdUser;

    @BeforeEach
    void setUp() {
        challengeService =
                new ChallengeService(
                        challengeRepository,
                        gameService
                );

        challenger = createUser(1L, "challenger", UserStatus.ACTIVE);
        opponent = createUser(2L, "opponent", UserStatus.ACTIVE);
        thirdUser = createUser(3L, "third", UserStatus.ACTIVE);
    }

    // =========================================================
    // SEND CHALLENGE
    // =========================================================

    @Test
    void sendChallengeShouldCreateChallenge() {

        when(challengeRepository
                .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                        eq(challenger),
                        eq(opponent),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(false);

        when(challengeRepository
                .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                        eq(opponent),
                        eq(challenger),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(false);

        when(challengeRepository.save(any(Challenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();

        Challenge result = challengeService.sendChallenge(
                challenger,
                opponent,
                TimeControl.values()[0],
                ColorPreference.values()[0],
                true
        );

        LocalDateTime after = LocalDateTime.now();

        assertEquals(challenger, result.getChallenger());
        assertEquals(opponent, result.getOpponent());
        assertTrue(result.isRated());

        assertNotNull(result.getExpiresAt());

        assertFalse(
                result.getExpiresAt()
                        .isBefore(before.plusHours(24))
        );

        assertFalse(
                result.getExpiresAt()
                        .isAfter(after.plusHours(24))
        );

        verify(challengeRepository)
                .save(any(Challenge.class));
    }

    @Test
    void sendChallengeShouldThrowWhenChallengingSelf() {

        assertThrows(
                IllegalArgumentException.class,
                () -> challengeService.sendChallenge(
                        challenger,
                        challenger,
                        TimeControl.values()[0],
                        ColorPreference.values()[0],
                        true
                )
        );

        verifyNoInteractions(challengeRepository);
    }

    @Test
    void sendChallengeShouldThrowWhenChallengerIsDisabled() {

        challenger.setStatus(UserStatus.DISABLED);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.sendChallenge(
                        challenger,
                        opponent,
                        TimeControl.values()[0],
                        ColorPreference.values()[0],
                        true
                )
        );

        verifyNoInteractions(challengeRepository);
    }

    @Test
    void sendChallengeShouldThrowWhenOpponentIsDisabled() {

        opponent.setStatus(UserStatus.DISABLED);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.sendChallenge(
                        challenger,
                        opponent,
                        TimeControl.values()[0],
                        ColorPreference.values()[0],
                        true
                )
        );

        verifyNoInteractions(challengeRepository);
    }

    @Test
    void sendChallengeShouldThrowWhenActivePendingChallengeExists() {

        when(challengeRepository
                .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                        eq(challenger),
                        eq(opponent),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.sendChallenge(
                        challenger,
                        opponent,
                        TimeControl.values()[0],
                        ColorPreference.values()[0],
                        true
                )
        );

        verify(challengeRepository, never())
                .save(any());
    }

    @Test
    void sendChallengeShouldThrowWhenReverseActiveChallengeExists() {

        when(challengeRepository
                .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                        eq(challenger),
                        eq(opponent),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(false);

        when(challengeRepository
                .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                        eq(opponent),
                        eq(challenger),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.sendChallenge(
                        challenger,
                        opponent,
                        TimeControl.values()[0],
                        ColorPreference.values()[0],
                        false
                )
        );

        verify(challengeRepository, never())
                .save(any());
    }

    // =========================================================
    // ACCEPT
    // =========================================================

    @Test
    void acceptChallengeShouldAcceptValidChallenge() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challengeService.acceptChallenge(
                challenge,
                opponent
        );

        assertEquals(
                ChallengeStatus.ACCEPTED,
                challenge.getStatus()
        );
    }

    @Test
    void acceptChallengeShouldThrowWhenUserIsNotOpponent() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.acceptChallenge(
                        challenge,
                        thirdUser
                )
        );

        assertEquals(
                ChallengeStatus.PENDING,
                challenge.getStatus()
        );
        verifyNoInteractions(gameService);
    }

    @Test
    void acceptChallengeShouldThrowWhenNotPending() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challenge.setStatus(ChallengeStatus.DECLINED);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.acceptChallenge(
                        challenge,
                        opponent
                )
        );
        verifyNoInteractions(gameService);
    }

    @Test
    void acceptChallengeShouldThrowWhenExpired() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challenge.setExpiresAt(
                LocalDateTime.now().minusSeconds(1)
        );

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.acceptChallenge(
                        challenge,
                        opponent
                )
        );

        assertEquals(
                ChallengeStatus.PENDING,
                challenge.getStatus()
        );
        verifyNoInteractions(gameService);
    }

    @Test
    void acceptChallengeShouldAcceptChallengeAndCreateGame() {

        Challenge challenge =
                createPendingChallenge(
                        challenger,
                        opponent
                );

        Game game = Game.builder()
                .whitePlayer(challenger)
                .blackPlayer(opponent)
                .status(GameStatus.WAITING)
                .build();

        when(gameService.createGameFromChallenge(challenge))
                .thenReturn(game);

        Game result =
                challengeService.acceptChallenge(
                        challenge,
                        opponent
                );

        assertEquals(
                ChallengeStatus.ACCEPTED,
                challenge.getStatus()
        );

        assertEquals(game, result);

        verify(gameService)
                .createGameFromChallenge(challenge);
    }

    // =========================================================
    // DECLINE
    // =========================================================

    @Test
    void declineChallengeShouldDeclineChallenge() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challengeService.declineChallenge(
                challenge,
                opponent
        );

        assertEquals(
                ChallengeStatus.DECLINED,
                challenge.getStatus()
        );
    }

    @Test
    void declineChallengeShouldThrowWhenUserIsNotOpponent() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.declineChallenge(
                        challenge,
                        thirdUser
                )
        );

        assertEquals(
                ChallengeStatus.PENDING,
                challenge.getStatus()
        );
    }

    @Test
    void declineChallengeShouldThrowWhenNotPending() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challenge.setStatus(ChallengeStatus.ACCEPTED);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.declineChallenge(
                        challenge,
                        opponent
                )
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancelChallengeShouldCancelChallenge() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challengeService.cancelChallenge(
                challenge,
                challenger
        );

        assertEquals(
                ChallengeStatus.CANCELLED,
                challenge.getStatus()
        );
    }

    @Test
    void cancelChallengeShouldThrowWhenUserIsNotChallenger() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.cancelChallenge(
                        challenge,
                        opponent
                )
        );

        assertEquals(
                ChallengeStatus.PENDING,
                challenge.getStatus()
        );
    }

    @Test
    void cancelChallengeShouldThrowWhenNotPending() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        challenge.setStatus(ChallengeStatus.DECLINED);

        assertThrows(
                IllegalStateException.class,
                () -> challengeService.cancelChallenge(
                        challenge,
                        challenger
                )
        );
    }

    // =========================================================
    // EXPIRATION
    // =========================================================

    @Test
    void expireChallengesShouldMarkReturnedChallengesExpired() {

        Challenge challenge1 =
                createPendingChallenge(challenger, opponent);

        Challenge challenge2 =
                createPendingChallenge(thirdUser, opponent);

        when(challengeRepository.findByStatusAndExpiresAtBefore(
                eq(ChallengeStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of(challenge1, challenge2));

        challengeService.expireChallenges();

        assertEquals(
                ChallengeStatus.EXPIRED,
                challenge1.getStatus()
        );

        assertEquals(
                ChallengeStatus.EXPIRED,
                challenge2.getStatus()
        );
    }

    @Test
    void expireChallengesShouldHandleNoExpiredChallenges() {

        when(challengeRepository.findByStatusAndExpiresAtBefore(
                eq(ChallengeStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        assertDoesNotThrow(
                () -> challengeService.expireChallenges()
        );
    }

    // =========================================================
    // GET ACTIVE PENDING CHALLENGES
    // =========================================================

    @Test
    void getIncomingPendingChallengesShouldReturnActiveChallenges() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        when(challengeRepository
                .findByOpponentAndStatusAndExpiresAtAfter(
                        eq(opponent),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(challenge));

        List<Challenge> result =
                challengeService
                        .getIncomingPendingChallenges(opponent);

        assertEquals(1, result.size());
        assertEquals(challenge, result.get(0));
    }

    @Test
    void getOutgoingPendingChallengesShouldReturnActiveChallenges() {

        Challenge challenge =
                createPendingChallenge(challenger, opponent);

        when(challengeRepository
                .findByChallengerAndStatusAndExpiresAtAfter(
                        eq(challenger),
                        eq(ChallengeStatus.PENDING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(challenge));

        List<Challenge> result =
                challengeService
                        .getOutgoingPendingChallenges(challenger);

        assertEquals(1, result.size());
        assertEquals(challenge, result.get(0));
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser(
            Long id,
            String username,
            UserStatus status
    ) {

        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password("password")
                .status(status)
                .build();

        setId(user, id);

        return user;
    }

    private Challenge createPendingChallenge(
            User challenger,
            User opponent
    ) {

        return Challenge.builder()
                .challenger(challenger)
                .opponent(opponent)
                .timeControl(TimeControl.values()[0])
                .colorPreference(ColorPreference.values()[0])
                .rated(true)
                .status(ChallengeStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private void setId(BaseEntity entity, Long id) {

        try {
            var field =
                    BaseEntity.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}