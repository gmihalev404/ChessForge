package com.example.chessforge.service;

import com.example.chessforge.model.entity.BaseEntity;
import com.example.chessforge.model.entity.Challenge;
import com.example.chessforge.model.entity.Game;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.*;
import com.example.chessforge.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    private GameService gameService;

    private User challenger;
    private User opponent;

    @BeforeEach
    void setUp() {

        gameService = new GameService(gameRepository);

        challenger = createUser(
                1L,
                "challenger",
                500,
                600,
                700,
                800
        );

        opponent = createUser(
                2L,
                "opponent",
                450,
                550,
                650,
                750
        );
    }

    // =========================================================
    // CREATE GAME FROM CHALLENGE
    // =========================================================

    @Test
    void createGameFromChallengeShouldThrowWhenChallengeIsNotAccepted() {

        Challenge challenge = createChallenge(
                ChallengeStatus.PENDING,
                ColorPreference.WHITE,
                TimeControl.values()[0],
                true
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gameService.createGameFromChallenge(challenge)
        );

        assertEquals(
                "A game can only be created from an accepted challenge.",
                exception.getMessage()
        );

        verifyNoInteractions(gameRepository);
    }

    @Test
    void createGameFromChallengeShouldAssignChallengerWhite() {

        Challenge challenge = createChallenge(
                ChallengeStatus.ACCEPTED,
                ColorPreference.WHITE,
                TimeControl.values()[0],
                true
        );

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Game game = gameService.createGameFromChallenge(challenge);

        assertEquals(challenger, game.getWhitePlayer());
        assertEquals(opponent, game.getBlackPlayer());

        assertEquals(
                challenge.getTimeControl(),
                game.getTimeControl()
        );

        assertTrue(game.isRated());
        assertEquals(GameStatus.WAITING, game.getStatus());
    }

    @Test
    void createGameFromChallengeShouldAssignChallengerBlack() {

        Challenge challenge = createChallenge(
                ChallengeStatus.ACCEPTED,
                ColorPreference.BLACK,
                TimeControl.values()[0],
                false
        );

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Game game = gameService.createGameFromChallenge(challenge);

        assertEquals(opponent, game.getWhitePlayer());
        assertEquals(challenger, game.getBlackPlayer());

        assertFalse(game.isRated());
        assertEquals(GameStatus.WAITING, game.getStatus());
    }

    @Test
    void createGameFromChallengeShouldAssignBothPlayersWhenColorIsRandom() {

        Challenge challenge = createChallenge(
                ChallengeStatus.ACCEPTED,
                ColorPreference.RANDOM,
                TimeControl.values()[0],
                true
        );

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Game game = gameService.createGameFromChallenge(challenge);

        assertNotEquals(
                game.getWhitePlayer(),
                game.getBlackPlayer()
        );

        assertTrue(
                game.getWhitePlayer().equals(challenger)
                        || game.getWhitePlayer().equals(opponent)
        );

        assertTrue(
                game.getBlackPlayer().equals(challenger)
                        || game.getBlackPlayer().equals(opponent)
        );

        assertTrue(
                (game.getWhitePlayer().equals(challenger)
                        && game.getBlackPlayer().equals(opponent))
                        ||
                        (game.getWhitePlayer().equals(opponent)
                                && game.getBlackPlayer().equals(challenger))
        );
    }

    @Test
    void createGameFromChallengeShouldSaveCreatedGame() {

        Challenge challenge = createChallenge(
                ChallengeStatus.ACCEPTED,
                ColorPreference.WHITE,
                TimeControl.values()[0],
                true
        );

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        gameService.createGameFromChallenge(challenge);

        ArgumentCaptor<Game> captor =
                ArgumentCaptor.forClass(Game.class);

        verify(gameRepository).save(captor.capture());

        Game savedGame = captor.getValue();

        assertEquals(GameStatus.WAITING, savedGame.getStatus());
        assertEquals(challenge.getTimeControl(), savedGame.getTimeControl());
        assertEquals(challenge.isRated(), savedGame.isRated());
    }

    // =========================================================
    // START GAME
    // =========================================================

    @Test
    void startGameShouldStartWaitingGame() {

        TimeControl timeControl = TimeControl.values()[0];

        Game game = createWaitingGame(
                challenger,
                opponent,
                timeControl
        );

        LocalDateTime before = LocalDateTime.now();

        gameService.startGame(game);

        LocalDateTime after = LocalDateTime.now();

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        assertNotNull(game.getStartedAt());

        assertFalse(game.getStartedAt().isBefore(before));
        assertFalse(game.getStartedAt().isAfter(after));

        assertEquals(
                timeControl.getInitialTimeSeconds(),
                game.getWhiteTimeRemaining()
        );

        assertEquals(
                timeControl.getInitialTimeSeconds(),
                game.getBlackTimeRemaining()
        );
    }

    @Test
    void startGameShouldThrowWhenGameIsNotWaiting() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.IN_PROGRESS);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.startGame(game)
        );
    }

    @Test
    void startGameShouldUseBulletRatings() {

        TimeControl control =
                findTimeControl(TimeControlType.BULLET);

        Game game =
                createWaitingGame(challenger, opponent, control);

        gameService.startGame(game);

        assertEquals(
                challenger.getBulletRating(),
                game.getWhiteRatingBefore()
        );

        assertEquals(
                opponent.getBulletRating(),
                game.getBlackRatingBefore()
        );
    }

    @Test
    void startGameShouldUseBlitzRatings() {

        TimeControl control =
                findTimeControl(TimeControlType.BLITZ);

        Game game =
                createWaitingGame(challenger, opponent, control);

        gameService.startGame(game);

        assertEquals(
                challenger.getBlitzRating(),
                game.getWhiteRatingBefore()
        );

        assertEquals(
                opponent.getBlitzRating(),
                game.getBlackRatingBefore()
        );
    }

    @Test
    void startGameShouldUseRapidRatings() {

        TimeControl control =
                findTimeControl(TimeControlType.RAPID);

        Game game =
                createWaitingGame(challenger, opponent, control);

        gameService.startGame(game);

        assertEquals(
                challenger.getRapidRating(),
                game.getWhiteRatingBefore()
        );

        assertEquals(
                opponent.getRapidRating(),
                game.getBlackRatingBefore()
        );
    }

    @Test
    void startGameShouldUseClassicalRatings() {

        TimeControl control =
                findTimeControl(TimeControlType.CLASSICAL);

        Game game =
                createWaitingGame(challenger, opponent, control);

        gameService.startGame(game);

        assertEquals(
                challenger.getClassicalRating(),
                game.getWhiteRatingBefore()
        );

        assertEquals(
                opponent.getClassicalRating(),
                game.getBlackRatingBefore()
        );
    }

    // =========================================================
    // FINISH GAME
    // =========================================================

    @Test
    void finishGameShouldFinishActiveGame() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.IN_PROGRESS);

        LocalDateTime before = LocalDateTime.now();

        gameService.finishGame(
                game,
                GameResult.WHITE_WIN,
                GameTermination.CHECKMATE
        );

        LocalDateTime after = LocalDateTime.now();

        assertEquals(GameStatus.FINISHED, game.getStatus());

        assertEquals(
                GameResult.WHITE_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.CHECKMATE,
                game.getTermination()
        );

        assertNotNull(game.getFinishedAt());

        assertFalse(game.getFinishedAt().isBefore(before));
        assertFalse(game.getFinishedAt().isAfter(after));
    }

    @Test
    void finishGameShouldThrowWhenGameIsNotInProgress() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        assertThrows(
                IllegalStateException.class,
                () -> gameService.finishGame(
                        game,
                        GameResult.WHITE_WIN,
                        GameTermination.CHECKMATE
                )
        );
    }

    @Test
    void finishGameShouldThrowWhenResultIsNull() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.IN_PROGRESS);

        assertThrows(
                IllegalArgumentException.class,
                () -> gameService.finishGame(
                        game,
                        null,
                        GameTermination.CHECKMATE
                )
        );
    }

    @Test
    void finishGameShouldThrowWhenTerminationIsNull() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.IN_PROGRESS);

        assertThrows(
                IllegalArgumentException.class,
                () -> gameService.finishGame(
                        game,
                        GameResult.DRAW,
                        null
                )
        );
    }

    // =========================================================
    // ABORT GAME
    // =========================================================

    @Test
    void abortGameShouldAbortWaitingGame() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        gameService.abortGame(game);

        assertEquals(GameStatus.ABORTED, game.getStatus());

        assertEquals(
                GameTermination.ABORTED,
                game.getTermination()
        );

        assertNotNull(game.getFinishedAt());
    }

    @Test
    void abortGameShouldAbortGameInProgress() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.IN_PROGRESS);

        gameService.abortGame(game);

        assertEquals(GameStatus.ABORTED, game.getStatus());

        assertEquals(
                GameTermination.ABORTED,
                game.getTermination()
        );
    }

    @Test
    void abortGameShouldThrowWhenGameIsFinished() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.FINISHED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.abortGame(game)
        );
    }

    @Test
    void abortGameShouldThrowWhenGameAlreadyAborted() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setStatus(GameStatus.ABORTED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.abortGame(game)
        );
    }

    // =========================================================
    // GET USER GAMES
    // =========================================================

    @Test
    void getGamesForUserShouldReturnRepositoryResult() {

        Game game1 = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        Game game2 = createWaitingGame(
                opponent,
                challenger,
                TimeControl.values()[0]
        );

        when(gameRepository
                .findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
                        challenger,
                        challenger
                ))
                .thenReturn(List.of(game1, game2));

        List<Game> result =
                gameService.getGamesForUser(challenger);

        assertEquals(2, result.size());

        assertEquals(game1, result.get(0));
        assertEquals(game2, result.get(1));

        verify(gameRepository)
                .findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
                        challenger,
                        challenger
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser(
            Long id,
            String username,
            int bullet,
            int blitz,
            int rapid,
            int classical
    ) {

        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password("password")
                .status(UserStatus.ACTIVE)
                .bulletRating(bullet)
                .blitzRating(blitz)
                .rapidRating(rapid)
                .classicalRating(classical)
                .build();

        setId(user, id);

        return user;
    }

    private Challenge createChallenge(
            ChallengeStatus status,
            ColorPreference colorPreference,
            TimeControl timeControl,
            boolean rated
    ) {

        return Challenge.builder()
                .challenger(challenger)
                .opponent(opponent)
                .status(status)
                .colorPreference(colorPreference)
                .timeControl(timeControl)
                .rated(rated)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private Game createWaitingGame(
            User whitePlayer,
            User blackPlayer,
            TimeControl timeControl
    ) {

        return Game.builder()
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .timeControl(timeControl)
                .rated(true)
                .status(GameStatus.WAITING)
                .build();
    }

    private TimeControl findTimeControl(
            TimeControlType type
    ) {

        return Arrays.stream(TimeControl.values())
                .filter(timeControl ->
                        timeControl.getType() == type
                )
                .findFirst()
                .orElseThrow();
    }

    private void setId(
            BaseEntity entity,
            Long id
    ) {

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