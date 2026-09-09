package com.example.chessforge.service;

import com.example.chessforge.model.entity.*;
import com.example.chessforge.model.enums.*;
import com.example.chessforge.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private RatingService ratingService;

    private GameService gameService;

    private User challenger;
    private User opponent;

    @BeforeEach
    void setUp() {

        gameService = new GameService(gameRepository, ratingService);

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

    @Test
    void createGameFromTournamentShouldCreateWaitingGame() {

        Tournament tournament = Tournament.builder()
                .timeControl(TimeControl.values()[0])
                .rated(true)
                .build();

        TournamentParticipant whiteParticipant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(challenger)
                        .status(TournamentParticipantStatus.ACTIVE)
                        .build();

        TournamentParticipant blackParticipant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(opponent)
                        .status(TournamentParticipantStatus.ACTIVE)
                        .build();

        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(whiteParticipant)
                .blackParticipant(blackParticipant)
                .roundNumber(1)
                .boardNumber(1)
                .status(TournamentMatchStatus.PENDING)
                .build();

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Game game =
                gameService.createGameFromTournament(match);

        assertEquals(challenger, game.getWhitePlayer());
        assertEquals(opponent, game.getBlackPlayer());

        assertEquals(
                tournament.getTimeControl(),
                game.getTimeControl()
        );

        assertTrue(game.isRated());

        assertEquals(
                GameStatus.WAITING,
                game.getStatus()
        );

        assertEquals(
                match,
                game.getTournamentMatch()
        );

        verify(gameRepository)
                .save(game);
    }

    @Test
    void createGameFromTournamentShouldThrowForByeMatch() {

        Tournament tournament = Tournament.builder()
                .timeControl(TimeControl.values()[0])
                .rated(true)
                .build();

        TournamentParticipant participant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(challenger)
                        .status(TournamentParticipantStatus.ACTIVE)
                        .build();

        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(participant)
                .blackParticipant(null)
                .roundNumber(1)
                .boardNumber(1)
                .status(TournamentMatchStatus.COMPLETED)
                .termination(TournamentMatchTermination.BYE)
                .build();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gameService.createGameFromTournament(match)
        );

        assertEquals(
                "A bye match cannot create a game.",
                exception.getMessage()
        );

        verifyNoInteractions(gameRepository);
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

        verifyNoInteractions(gameRepository, ratingService);
    }

    @Test
    void startGameShouldSetRatingSnapshotsUsingRatingService() {

        TimeControl timeControl = TimeControl.values()[0];

        Game game = createWaitingGame(
                challenger,
                opponent,
                timeControl
        );

        TimeControlType type = timeControl.getType();

        when(ratingService.getRating(
                challenger,
                type
        )).thenReturn(650);

        when(ratingService.getRating(
                opponent,
                type
        )).thenReturn(720);

        gameService.startGame(game);

        assertEquals(
                650,
                game.getWhiteRatingBefore()
        );

        assertEquals(
                720,
                game.getBlackRatingBefore()
        );

        verify(ratingService)
                .getRating(challenger, type);

        verify(ratingService)
                .getRating(opponent, type);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void startGameShouldThrowWhenEitherPlayerAlreadyHasActiveGame(
            boolean whitePlayerHasActiveGame
    ) {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        if (whitePlayerHasActiveGame) {
            doReturn(true)
                    .when(gameRepository)
                    .existsByPlayerAndStatus(
                            challenger,
                            GameStatus.IN_PROGRESS
                    );
        } else {
            doReturn(false)
                    .when(gameRepository)
                    .existsByPlayerAndStatus(
                            challenger,
                            GameStatus.IN_PROGRESS
                    );

            doReturn(true)
                    .when(gameRepository)
                    .existsByPlayerAndStatus(
                            opponent,
                            GameStatus.IN_PROGRESS
                    );
        }

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gameService.startGame(game)
        );

        assertEquals(
                whitePlayerHasActiveGame
                        ? "White player already has an active game."
                        : "Black player already has an active game.",
                exception.getMessage()
        );

        assertEquals(
                GameStatus.WAITING,
                game.getStatus()
        );

        verifyNoInteractions(ratingService);
    }

    // =========================================================
    // FINISH GAME
    // =========================================================

    @Test
    void finishGameShouldFinishActiveGameAndUpdateRatings() {

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

        verify(ratingService).updateRatings(game);
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

        verifyNoInteractions(ratingService);
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

        verifyNoInteractions(ratingService);
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

        verifyNoInteractions(ratingService);
    }

    // =========================================================
    // ABORT GAME
    // =========================================================

    @Test
    void abortGameShouldKeepRatingsUnchanged() {

        Game game = createWaitingGame(
                challenger,
                opponent,
                TimeControl.values()[0]
        );

        game.setWhiteRatingBefore(600);
        game.setBlackRatingBefore(550);

        gameService.abortGame(game);

        assertEquals(GameStatus.ABORTED, game.getStatus());
        assertEquals(GameTermination.ABORTED, game.getTermination());
        assertNotNull(game.getFinishedAt());

        assertEquals(600, game.getWhiteRatingAfter());
        assertEquals(550, game.getBlackRatingAfter());

        verifyNoInteractions(ratingService);
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