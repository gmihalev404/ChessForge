package com.example.chessforge.service.game;

import com.example.chessforge.model.entity.challenge.Challenge;
import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.game.GameMove;
import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.challenge.ChallengeStatus;
import com.example.chessforge.model.enums.challenge.ColorPreference;
import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.game.GameTermination;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.model.enums.timeControl.TimeControlType;
import com.example.chessforge.model.enums.tournament.TournamentMatchStatus;
import com.example.chessforge.model.enums.tournament.TournamentMatchTermination;
import com.example.chessforge.model.enums.tournament.TournamentParticipantStatus;
import com.example.chessforge.model.enums.user.UserStatus;
import com.example.chessforge.repository.game.GameMoveRepository;
import com.example.chessforge.repository.game.GameRepository;
import com.example.chessforge.service.game.dto.GameStateResponse;
import com.example.chessforge.service.game.engine.GameEngine;
import com.example.chessforge.service.game.engine.history.RepetitionTracker;
import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.notation.PgnGenerator;
import com.example.chessforge.service.tournament.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private RatingService ratingService;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private GameEngine gameEngine;

    @Mock
    private GameMoveRepository gameMoveRepository;

    @Mock
    private RepetitionTracker repetitionTracker;

    @Mock
    private GameStateMapper gameStateMapper;

    @Spy
    private Clock clock =
            Clock.systemDefaultZone();

    @Mock
    private PgnGenerator pgnGenerator;

    @InjectMocks
    private GameService gameService;

    private User challenger;
    private User opponent;
    private static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @BeforeEach
    void setUp() {
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
    void createGameFromChallengeShouldInitializeCurrentFen() {

        Challenge challenge =
                createChallenge(
                        ChallengeStatus.ACCEPTED,
                        ColorPreference.WHITE,
                        TimeControl.values()[0],
                        true
                );

        when(gameEngine.toFen(any(GameState.class)))
                .thenReturn(INITIAL_FEN);

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Game game =
                gameService.createGameFromChallenge(
                        challenge
                );

        assertEquals(
                INITIAL_FEN,
                game.getCurrentFen()
        );

        verify(gameEngine)
                .toFen(
                        any(GameState.class)
                );
    }

    // =========================================================
    // CREATE GAME FROM TOURNAMENT
    // =========================================================

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

    @Test
    void createGameFromTournamentShouldInitializeCurrentFen() {

        Tournament tournament =
                Tournament.builder()
                        .timeControl(
                                TimeControl.values()[0]
                        )
                        .rated(true)
                        .build();

        TournamentParticipant whiteParticipant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(challenger)
                        .status(
                                TournamentParticipantStatus.ACTIVE
                        )
                        .build();

        TournamentParticipant blackParticipant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(opponent)
                        .status(
                                TournamentParticipantStatus.ACTIVE
                        )
                        .build();

        TournamentMatch match =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(
                                whiteParticipant
                        )
                        .blackParticipant(
                                blackParticipant
                        )
                        .roundNumber(1)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        when(gameEngine.toFen(any(GameState.class)))
                .thenReturn(INITIAL_FEN);

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Game game =
                gameService.createGameFromTournament(
                        match
                );

        assertEquals(
                INITIAL_FEN,
                game.getCurrentFen()
        );

        verify(gameEngine)
                .toFen(
                        any(GameState.class)
                );
    }

    // =========================================================
    // START GAME
    // =========================================================

    @Test
    void startGameShouldStartWaitingGame() {

        TimeControl timeControl =
                TimeControl.values()[0];

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        timeControl
                );

        mockCurrentTime(
                "2026-09-11T12:00:00Z"
        );

        gameService.startGame(
                game
        );

        long expectedInitialTimeMillis =
                timeControl.getInitialTimeSeconds()
                        * 1000L;

        LocalDateTime expectedStartTime =
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0
                );

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        assertEquals(
                expectedStartTime,
                game.getStartedAt()
        );

        assertEquals(
                expectedStartTime,
                game.getTurnStartedAt()
        );

        assertEquals(
                expectedInitialTimeMillis,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                expectedInitialTimeMillis,
                game.getBlackTimeRemainingMillis()
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

    @Test
    void startGameShouldInitializeTurnStartTime() {

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        mockCurrentTime(
                "2026-09-11T12:00:00Z"
        );

        gameService.startGame(game);

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0
                ),
                game.getTurnStartedAt()
        );
    }

    @Test
    void startGameShouldSetTurnExpirationTime() {

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        mockCurrentTime(
                "2026-09-12T12:00:00Z"
        );

        when(gameRepository.existsByPlayerAndStatus(
                game.getWhitePlayer(),
                GameStatus.IN_PROGRESS
        )).thenReturn(false);

        when(gameRepository.existsByPlayerAndStatus(
                game.getBlackPlayer(),
                GameStatus.IN_PROGRESS
        )).thenReturn(false);

        gameService.startGame(
                game
        );

        long initialTimeMillis =
                game.getTimeControl()
                        .getInitialTimeSeconds()
                        * 1000L;

        assertEquals(
                game.getTurnStartedAt()
                        .plus(
                                Duration.ofMillis(
                                        initialTimeMillis
                                )
                        ),
                game.getTurnExpiresAt()
        );
    }

    // =========================================================
    // START GAME - TOURNAMENT MATCH
    // =========================================================

    @Test
    void startGameShouldStartTournamentMatch() {

        TournamentMatch tournamentMatch =
                TournamentMatch.builder()
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setTournamentMatch(
                tournamentMatch
        );

        when(
                gameRepository.existsByPlayerAndStatus(
                        game.getWhitePlayer(),
                        GameStatus.IN_PROGRESS
                )
        ).thenReturn(false);

        when(
                gameRepository.existsByPlayerAndStatus(
                        game.getBlackPlayer(),
                        GameStatus.IN_PROGRESS
                )
        ).thenReturn(false);

        gameService.startGame(game);

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        assertEquals(
                TournamentMatchStatus.IN_PROGRESS,
                tournamentMatch.getStatus()
        );
    }

    @Test
    void startGameShouldAllowTournamentMatchAlreadyInProgress() {

        TournamentMatch tournamentMatch =
                TournamentMatch.builder()
                        .status(
                                TournamentMatchStatus.IN_PROGRESS
                        )
                        .build();

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setTournamentMatch(
                tournamentMatch
        );

        when(
                gameRepository.existsByPlayerAndStatus(
                        game.getWhitePlayer(),
                        GameStatus.IN_PROGRESS
                )
        ).thenReturn(false);

        when(
                gameRepository.existsByPlayerAndStatus(
                        game.getBlackPlayer(),
                        GameStatus.IN_PROGRESS
                )
        ).thenReturn(false);

        assertDoesNotThrow(
                () ->
                        gameService.startGame(
                                game
                        )
        );

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        assertEquals(
                TournamentMatchStatus.IN_PROGRESS,
                tournamentMatch.getStatus()
        );
    }

    @Test
    void startGameShouldRejectCompletedTournamentMatch() {

        TournamentMatch tournamentMatch =
                TournamentMatch.builder()
                        .status(
                                TournamentMatchStatus.COMPLETED
                        )
                        .build();

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setTournamentMatch(
                tournamentMatch
        );

        when(
                gameRepository.existsByPlayerAndStatus(
                        game.getWhitePlayer(),
                        GameStatus.IN_PROGRESS
                )
        ).thenReturn(false);

        when(
                gameRepository.existsByPlayerAndStatus(
                        game.getBlackPlayer(),
                        GameStatus.IN_PROGRESS
                )
        ).thenReturn(false);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.startGame(
                                        game
                                )
                );

        assertEquals(
                "A game cannot be started for a completed tournament match.",
                exception.getMessage()
        );

        assertEquals(
                GameStatus.WAITING,
                game.getStatus()
        );
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

    @Test
    void finishGameShouldUpdateTournamentMatchForTournamentGame() {

        TournamentMatch tournamentMatch =
                TournamentMatch.builder()
                        .status(
                                TournamentMatchStatus.IN_PROGRESS
                        )
                        .build();

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setTournamentMatch(
                tournamentMatch
        );

        gameService.finishGame(
                game,
                GameResult.WHITE_WIN,
                GameTermination.CHECKMATE
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.WHITE_WIN,
                game.getResult()
        );

        verify(ratingService)
                .updateRatings(game);

        verify(tournamentService)
                .recordGameResult(game);
    }

    @Test
    void finishGameShouldNotUpdateTournamentForNormalGame() {

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        assertNull(
                game.getTournamentMatch()
        );

        gameService.finishGame(
                game,
                GameResult.BLACK_WIN,
                GameTermination.RESIGNATION
        );

        verify(ratingService)
                .updateRatings(game);

        verifyNoInteractions(
                tournamentService
        );
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
    // MAKE MOVE - CORE AND VALIDATION
    // =========================================================

    @Test
    void makeMoveShouldUpdateCurrentFen() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "old-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("old-fen"))
                .thenReturn(state);

        when(gameEngine.toFen(state))
                .thenReturn("new-fen");

        when(gameRepository.save(game))
                .thenReturn(game);

        mockEmptyRepetitionHistory(
                gameId
        );

        mockSan(
                state,
                move,
                "e4"
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        challenger,
                        move
                );

        verify(gameEngine)
                .fromFen(
                        "old-fen"
                );

        verify(gameEngine)
                .makeMove(
                        state,
                        move
                );

        verify(repetitionTracker)
                .recordPosition(
                        state
                );

        ArgumentCaptor<GameMove> moveCaptor =
                ArgumentCaptor.forClass(
                        GameMove.class
                );

        verify(gameMoveRepository)
                .save(
                        moveCaptor.capture()
                );

        GameMove savedMove =
                moveCaptor.getValue();

        assertEquals(
                game,
                savedMove.getGame()
        );

        assertEquals(
                1,
                savedMove.getPlyNumber()
        );

        assertEquals(
                "e2",
                savedMove.getFromSquare()
        );

        assertEquals(
                "e4",
                savedMove.getToSquare()
        );

        assertEquals(
                "NORMAL",
                savedMove.getMoveType()
        );

        assertNull(
                savedMove.getPromotionPiece()
        );

        assertEquals(
                "new-fen",
                savedMove.getFenAfter()
        );

        assertSame(
                game,
                result
        );

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void makeMoveShouldAllowBlackPlayerWhenBlackIsToMove() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "black-to-move-fen"
        );

        GameState state =
                GameState.initial();

        state.setSideToMove(
                PieceColor.BLACK
        );

        Move move =
                Move.normal(
                        "e7",
                        "e5"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "black-to-move-fen"
        )).thenReturn(state);

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        mockEmptyRepetitionHistory(
                gameId
        );

        mockSan(
                state,
                move,
                "e5"
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        opponent,
                        move
                );

        verify(gameEngine)
                .makeMove(
                        state,
                        move
                );

        verify(gameRepository)
                .save(
                        game
                );

        assertSame(
                game,
                result
        );

        ArgumentCaptor<GameMove> moveCaptor =
                ArgumentCaptor.forClass(
                        GameMove.class
                );

        verify(gameMoveRepository)
                .save(
                        moveCaptor.capture()
                );

        assertEquals(
                2,
                moveCaptor
                        .getValue()
                        .getPlyNumber()
        );
    }

    @Test
    void makeMoveShouldRejectPlayerWhenItIsNotTheirTurn() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(state);

        assertThrows(
                IllegalStateException.class,
                () ->
                        gameService.makeMove(
                                gameId,
                                opponent,
                                move
                        )
        );

        verify(gameEngine, never())
                .makeMove(
                        any(),
                        any()
                );

        verify(gameEngine, never())
                .toFen(
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void makeMoveShouldRejectGameThatIsNotInProgress() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        // createWaitingGame already creates WAITING game.

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        gameService.makeMove(
                                gameId,
                                challenger,
                                Move.normal(
                                        "e2",
                                        "e4"
                                )
                        )
        );

        verify(gameEngine, never())
                .fromFen(
                        anyString()
                );

        verify(gameEngine, never())
                .makeMove(
                        any(),
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void makeMoveShouldRejectMissingGame() {

        Long gameId = 999L;

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gameService.makeMove(
                                gameId,
                                challenger,
                                Move.normal(
                                        "e2",
                                        "e4"
                                )
                        )
        );

        verifyNoInteractions(
                gameEngine
        );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void makeMoveShouldRejectGameWithoutCurrentFen() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                null
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        gameService.makeMove(
                                gameId,
                                challenger,
                                Move.normal(
                                        "e2",
                                        "e4"
                                )
                        )
        );

        verify(gameEngine, never())
                .fromFen(
                        anyString()
                );

        verify(gameEngine, never())
                .makeMove(
                        any(),
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void makeMoveShouldNotSaveGameWhenMoveIsIllegal() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e5"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(state);

        doThrow(
                new IllegalArgumentException(
                        "Illegal chess move."
                )
        )
                .when(gameEngine)
                .makeMove(
                        state,
                        move
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gameService.makeMove(
                                gameId,
                                challenger,
                                move
                        )
        );

        assertEquals(
                "current-fen",
                game.getCurrentFen()
        );

        verify(gameEngine, never())
                .toFen(
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void makeMoveRequestShouldResolveMoveAfterLock() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        initializeTestClock(
                game
        );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Square from =
                Square.fromAlgebraic(
                        "e2"
                );

        Square to =
                Square.fromAlgebraic(
                        "e4"
                );

        Move resolvedMove =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.resolveMove(
                state,
                from,
                to,
                null
        )).thenReturn(
                resolvedMove
        );

        mockSan(
                state,
                resolvedMove,
                "e4"
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(
                state
        )).thenReturn(
                "new-fen"
        );

        when(gameRepository.save(
                game
        )).thenReturn(
                game
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        challenger,
                        "e2",
                        "e4",
                        null
                );

        assertSame(
                game,
                result
        );

        verify(gameEngine)
                .resolveMove(
                        state,
                        from,
                        to,
                        null
                );

        verify(gameEngine)
                .makeMove(
                        state,
                        resolvedMove
                );
    }

    // =========================================================
    // MAKE MOVE - TERMINAL CONDITIONS
    // =========================================================

    @ParameterizedTest
    @EnumSource(PieceColor.class)
    void makeMoveShouldFinishGameOnCheckmate(
            PieceColor movingColor
    ) {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        state.setSideToMove(
                movingColor
        );

        User movingPlayer =
                movingColor == PieceColor.WHITE
                        ? challenger
                        : opponent;

        Move move =
                movingColor == PieceColor.WHITE
                        ? Move.normal("e2", "e4")
                        : Move.normal("e7", "e5");

        GameResult expectedResult =
                movingColor == PieceColor.WHITE
                        ? GameResult.WHITE_WIN
                        : GameResult.BLACK_WIN;

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(state);

        doAnswer(invocation -> {

            state.setSideToMove(
                    movingColor.opposite()
            );

            return null;

        }).when(gameEngine)
                .makeMove(
                        state,
                        move
                );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "checkmate-fen"
                );

        when(gameEngine.isCheckmate(state))
                .thenReturn(true);

        when(gameRepository.save(game))
                .thenReturn(game);

        mockEmptyRepetitionHistory(gameId);

        String san =
                movingColor == PieceColor.WHITE
                        ? "e4#"
                        : "e5#";

        mockSan(
                state,
                move,
                san
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        movingPlayer,
                        move
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                expectedResult,
                game.getResult()
        );

        assertEquals(
                GameTermination.CHECKMATE,
                game.getTermination()
        );

        assertEquals(
                "checkmate-fen",
                game.getCurrentFen()
        );

        assertNotNull(
                game.getFinishedAt()
        );

        verify(ratingService)
                .updateRatings(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void makeMoveShouldFinishGameOnStalemate() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("current-fen"))
                .thenReturn(state);

        when(gameEngine.toFen(state))
                .thenReturn(
                        "stalemate-fen"
                );

        when(gameEngine.isStalemate(state))
                .thenReturn(true);

        when(gameRepository.save(game))
                .thenReturn(game);

        mockEmptyRepetitionHistory(
                gameId
        );

        mockSan(
                state,
                move,
                "e4"
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        challenger,
                        move
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.STALEMATE,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(game);

        assertSame(
                game,
                result
        );
    }

    @Test
    void makeMoveShouldFinishGameOnInsufficientMaterial() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);
        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("current-fen"))
                .thenReturn(state);

        when(gameEngine.toFen(state))
                .thenReturn(
                        "insufficient-material-fen"
                );

        when(gameEngine.isInsufficientMaterial(state))
                .thenReturn(true);

        when(gameRepository.save(game))
                .thenReturn(game);

        mockEmptyRepetitionHistory(
                gameId
        );

        mockSan(
                state,
                move,
                "e4"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.INSUFFICIENT_MATERIAL,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(game);
    }

    // =========================================================
    // MOVE PERSISTENCE AND HISTORY
    // =========================================================

    @Test
    void makeMoveShouldStorePromotionPiece() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "promotion-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.promotion(
                        Square.fromAlgebraic("a7"),
                        Square.fromAlgebraic("a8"),
                        PieceType.QUEEN
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "promotion-fen"
        )).thenReturn(state);

        when(gameEngine.toFen(state))
                .thenReturn(
                        "after-promotion-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        mockEmptyRepetitionHistory(
                gameId
        );

        mockSan(
                state,
                move,
                "a8=Q"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        ArgumentCaptor<GameMove> captor =
                ArgumentCaptor.forClass(
                        GameMove.class
                );

        verify(gameMoveRepository)
                .save(
                        captor.capture()
                );

        GameMove savedMove =
                captor.getValue();

        assertEquals(
                "PROMOTION",
                savedMove.getMoveType()
        );

        assertEquals(
                "QUEEN",
                savedMove.getPromotionPiece()
        );
    }

    @Test
    void makeMoveShouldRestoreRepetitionHistoryFromStoredMoves() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState currentState =
                GameState.initial();

        GameState firstHistoricalState =
                GameState.initial();

        GameState secondHistoricalState =
                GameState.initial();

        GameMove firstMove =
                GameMove.builder()
                        .game(game)
                        .plyNumber(1)
                        .fenAfter("fen-1")
                        .build();

        GameMove secondMove =
                GameMove.builder()
                        .game(game)
                        .plyNumber(2)
                        .fenAfter("fen-2")
                        .build();

        Move move =
                Move.normal(
                        "g1",
                        "f3"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("current-fen"))
                .thenReturn(
                        currentState
                );

        when(gameEngine.createRepetitionTracker(
                any(GameState.class)
        )).thenReturn(
                repetitionTracker
        );

        when(gameMoveRepository
                .findByGameIdOrderByPlyNumberAsc(gameId))
                .thenReturn(
                        List.of(
                                firstMove,
                                secondMove
                        )
                );

        when(gameEngine.fromFen("fen-1"))
                .thenReturn(
                        firstHistoricalState
                );

        when(gameEngine.fromFen("fen-2"))
                .thenReturn(
                        secondHistoricalState
                );

        when(gameEngine.toFen(currentState))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        mockSan(
                currentState,
                move,
                "Nf3"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        verify(repetitionTracker)
                .recordPosition(
                        firstHistoricalState
                );

        verify(repetitionTracker)
                .recordPosition(
                        secondHistoricalState
                );

        verify(repetitionTracker)
                .recordPosition(
                        currentState
                );
    }

    // =========================================================
    // CLAIM DRAW
    // =========================================================

    @Test
    void claimDrawShouldFinishGameOnThreefoldRepetition() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("current-fen"))
                .thenReturn(state);

        when(gameEngine.createRepetitionTracker(
                any(GameState.class)
        )).thenReturn(
                repetitionTracker
        );

        when(gameMoveRepository
                .findByGameIdOrderByPlyNumberAsc(gameId))
                .thenReturn(
                        List.of()
                );

        when(gameEngine.getClaimableDrawReasons(
                state,
                repetitionTracker
        )).thenReturn(
                Set.of(
                        DrawReason.THREEFOLD_REPETITION
                )
        );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.claimDraw(
                        gameId,
                        challenger
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.THREEFOLD_REPETITION,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(game);

        assertSame(
                game,
                result
        );
    }

    @Test
    void claimDrawShouldRejectWhenNoClaimableDrawExists() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("current-fen"))
                .thenReturn(state);

        when(gameEngine.createRepetitionTracker(
                any(GameState.class)
        )).thenReturn(
                repetitionTracker
        );

        when(gameMoveRepository
                .findByGameIdOrderByPlyNumberAsc(gameId))
                .thenReturn(
                        List.of()
                );

        when(gameEngine.getClaimableDrawReasons(
                state,
                repetitionTracker
        )).thenReturn(
                Set.of()
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        gameService.claimDraw(
                                gameId,
                                challenger
                        )
        );

        verify(ratingService, never())
                .updateRatings(any());

        verify(gameRepository, never())
                .save(any());
    }

    @Test
    void claimDrawShouldFinishGameOnFiftyMoveRule() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(state);

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.getClaimableDrawReasons(
                state,
                repetitionTracker
        )).thenReturn(
                Set.of(
                        DrawReason.FIFTY_MOVE_RULE
                )
        );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.claimDraw(
                        gameId,
                        challenger
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.FIFTY_MOVE_RULE,
                game.getTermination()
        );

        assertNotNull(
                game.getFinishedAt()
        );

        verify(gameEngine)
                .getClaimableDrawReasons(
                        state,
                        repetitionTracker
                );

        verify(ratingService)
                .updateRatings(
                        game
                );

        verify(gameRepository)
                .save(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void claimDrawShouldRejectWhenGameIsNotInProgress() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        // createWaitingGame() already sets WAITING.

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.claimDraw(
                                        gameId,
                                        challenger
                                )
                );

        assertEquals(
                "Game is not in progress.",
                exception.getMessage()
        );

        verify(gameEngine, never())
                .fromFen(
                        anyString()
                );

        verify(gameEngine, never())
                .createRepetitionTracker(
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void claimDrawShouldRejectPlayerWhenItIsNotTheirTurn() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        // Initial state => WHITE to move.
        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(state);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.claimDraw(
                                        gameId,
                                        opponent
                                )
                );

        assertEquals(
                "It is not this player's turn.",
                exception.getMessage()
        );

        verify(gameEngine, never())
                .createRepetitionTracker(
                        any()
                );

        verify(gameEngine, never())
                .getClaimableDrawReasons(
                        any(),
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );

        verifyNoInteractions(
                ratingService
        );
    }

    // =========================================================
    // RESIGN GAME
    // =========================================================

    @Test
    void resignGameShouldGiveBlackWinWhenWhiteResigns() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.resignGame(
                        gameId,
                        challenger
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.BLACK_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.RESIGNATION,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(game);

        assertSame(
                game,
                result
        );
    }

    @Test
    void resignGameShouldGiveWhiteWinWhenBlackResigns() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.resignGame(
                        gameId,
                        opponent
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.WHITE_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.RESIGNATION,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(game);

        assertSame(
                game,
                result
        );
    }

    @Test
    void resignGameShouldRejectNonParticipant() {

        Long gameId = 1L;

        User outsider =
                createUser(
                        3L,
                        "outsider",
                        500,
                        500,
                        500,
                        500
                );

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                gameService.resignGame(
                                        gameId,
                                        outsider
                                )
                );

        assertEquals(
                "User is not a participant in this game.",
                exception.getMessage()
        );

        verify(gameRepository, never())
                .save(any());

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void resignGameShouldRejectGameThatIsNotInProgress() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        gameService.resignGame(
                                gameId,
                                challenger
                        )
        );

        verify(gameRepository, never())
                .save(any());

        verifyNoInteractions(
                ratingService
        );
    }

    // =========================================================
    // DRAW OFFERS
    // =========================================================

    @Test
    void offerDrawShouldStoreOfferingPlayer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.offerDraw(
                        gameId,
                        challenger
                );

        assertEquals(
                challenger,
                game.getDrawOfferBy()
        );

        assertSame(
                game,
                result
        );

        verify(gameRepository)
                .save(game);
    }

    @Test
    void offerDrawShouldRejectWhenOfferAlreadyExists() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setDrawOfferBy(
                challenger
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.offerDraw(
                                        gameId,
                                        opponent
                                )
                );

        assertEquals(
                "There is already an active draw offer.",
                exception.getMessage()
        );

        verify(gameRepository, never())
                .save(any());
    }

    @Test
    void offerDrawShouldRejectNonParticipant() {

        Long gameId = 1L;

        User outsider =
                createUser(
                        3L,
                        "outsider",
                        500,
                        500,
                        500,
                        500
                );

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gameService.offerDraw(
                                gameId,
                                outsider
                        )
        );

        assertNull(
                game.getDrawOfferBy()
        );

        verify(gameRepository, never())
                .save(any());
    }

    @Test
    void acceptDrawShouldFinishGameAsDrawByAgreement() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setDrawOfferBy(
                challenger
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.acceptDraw(
                        gameId,
                        opponent
                );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.AGREEMENT,
                game.getTermination()
        );

        assertNull(
                game.getDrawOfferBy()
        );

        verify(ratingService)
                .updateRatings(game);

        verify(gameRepository)
                .save(game);

        assertSame(
                game,
                result
        );
    }

    @Test
    void acceptDrawShouldRejectWhenThereIsNoActiveOffer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.acceptDraw(
                                        gameId,
                                        opponent
                                )
                );

        assertEquals(
                "There is no active draw offer.",
                exception.getMessage()
        );

        verify(gameRepository, never())
                .save(any());

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void acceptDrawShouldRejectOwnDrawOffer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setDrawOfferBy(
                challenger
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.acceptDraw(
                                        gameId,
                                        challenger
                                )
                );

        assertEquals(
                "A player cannot accept their own draw offer.",
                exception.getMessage()
        );

        assertEquals(
                challenger,
                game.getDrawOfferBy()
        );

        verify(gameRepository, never())
                .save(any());

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void rejectDrawShouldClearActiveOffer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setDrawOfferBy(
                challenger
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        Game result =
                gameService.rejectDraw(
                        gameId,
                        opponent
                );

        assertNull(
                game.getDrawOfferBy()
        );

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        verify(gameRepository)
                .save(game);

        verifyNoInteractions(
                ratingService
        );

        assertSame(
                game,
                result
        );
    }

    @Test
    void rejectDrawShouldRejectWhenThereIsNoActiveOffer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.rejectDraw(
                                        gameId,
                                        opponent
                                )
                );

        assertEquals(
                "There is no active draw offer.",
                exception.getMessage()
        );

        verify(gameRepository, never())
                .save(any());
    }

    @Test
    void rejectDrawShouldRejectOwnDrawOffer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setDrawOfferBy(
                challenger
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.rejectDraw(
                                        gameId,
                                        challenger
                                )
                );

        assertEquals(
                "A player cannot reject their own draw offer.",
                exception.getMessage()
        );

        assertEquals(
                challenger,
                game.getDrawOfferBy()
        );

        verify(gameRepository, never())
                .save(any());
    }

    @Test
    void makeMoveShouldClearOpponentsDrawOffer() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );
        initializeTestClock(game);

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setDrawOfferBy(
                opponent
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen("current-fen"))
                .thenReturn(state);

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(game);

        mockSan(
                state,
                move,
                "e4"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        assertNull(
                game.getDrawOfferBy()
        );
    }

    // =========================================================
    // CLOCKS AND TIMEOUTS
    // =========================================================

    @Test
    void timeoutGameShouldGiveBlackWinWhenWhiteTimesOut() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.hasInsufficientMatingMaterial(
                state,
                PieceColor.BLACK
        )).thenReturn(
                false
        );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setWhiteTimeRemainingMillis(
                10_000L
        );

        game.setBlackTimeRemainingMillis(
                20_000L
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        Game result =
                gameService.timeoutGame(
                        gameId,
                        challenger
                );

        assertEquals(
                0L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                20_000L,
                game.getBlackTimeRemainingMillis()
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.BLACK_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void timeoutGameShouldGiveWhiteWinWhenBlackTimesOut() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setWhiteTimeRemainingMillis(
                20_000L
        );

        game.setBlackTimeRemainingMillis(
                10_000L
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        Game result =
                gameService.timeoutGame(
                        gameId,
                        opponent
                );

        assertEquals(
                0L,
                game.getBlackTimeRemainingMillis()
        );

        assertEquals(
                20_000L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.WHITE_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT,
                game.getTermination()
        );

        verify(ratingService)
                .updateRatings(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void timeoutGameShouldRejectNonParticipant() {

        Long gameId = 1L;

        User outsider =
                createUser(
                        3L,
                        "outsider",
                        500,
                        500,
                        500,
                        500
                );

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gameService.timeoutGame(
                                gameId,
                                outsider
                        )
        );

        verify(gameRepository, never())
                .save(any());

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void timeoutGameShouldRejectGameThatIsNotInProgress() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(game, gameId);

        // WAITING

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        gameService.timeoutGame(
                                gameId,
                                challenger
                        )
        );

        verify(gameRepository, never())
                .save(any());

        verifyNoInteractions(
                ratingService
        );
    }

    @Test
    void makeMoveShouldDecreaseWhiteClock() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                100_000L
        );

        game.setBlackTimeRemainingMillis(
                100_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0
                )
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        mockCurrentTime(
                "2026-09-11T12:00:10Z"
        );

        mockSan(
                state,
                move,
                "e4"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        long expected =
                100_000L
                        - 10_000L
                        + game.getTimeControl()
                        .getIncrementSeconds()
                        * 1000L;

        assertEquals(
                expected,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                100_000L,
                game.getBlackTimeRemainingMillis()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0,
                        10
                ),
                game.getTurnStartedAt()
        );
    }

    @Test
    void makeMoveShouldDecreaseBlackClock() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                100_000L
        );

        game.setBlackTimeRemainingMillis(
                80_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0
                )
        );

        GameState state =
                GameState.initial();

        state.setSideToMove(
                PieceColor.BLACK
        );

        Move move =
                Move.normal(
                        "e7",
                        "e5"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        mockCurrentTime(
                "2026-09-11T12:00:15Z"
        );

        mockSan(
                state,
                move,
                "e5"
        );

        gameService.makeMove(
                gameId,
                opponent,
                move
        );

        long expected =
                80_000L
                        - 15_000L
                        + game.getTimeControl()
                        .getIncrementSeconds()
                        * 1000L;

        assertEquals(
                expected,
                game.getBlackTimeRemainingMillis()
        );

        assertEquals(
                100_000L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0,
                        15
                ),
                game.getTurnStartedAt()
        );
    }

    @Test
    void makeMoveShouldFinishGameOnTimeoutBeforeExecutingMove() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                5_000L
        );

        game.setBlackTimeRemainingMillis(
                100_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0
                )
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        when(gameEngine.hasInsufficientMatingMaterial(
                state,
                PieceColor.BLACK
        )).thenReturn(false);

        mockCurrentTime(
                "2026-09-11T12:00:05Z"
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        challenger,
                        Move.normal(
                                "e2",
                                "e4"
                        )
                );

        assertEquals(
                0L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                100_000L,
                game.getBlackTimeRemainingMillis()
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.BLACK_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT,
                game.getTermination()
        );

        verify(gameEngine, never())
                .makeMove(
                        any(),
                        any()
                );

        verify(gameMoveRepository, never())
                .save(
                        any()
                );

        verify(ratingService)
                .updateRatings(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void makeMoveShouldRejectWhenTurnStartTimeIsMissing() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                100_000L
        );

        game.setBlackTimeRemainingMillis(
                100_000L
        );

        game.setTurnStartedAt(
                null
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                gameService.makeMove(
                                        gameId,
                                        challenger,
                                        Move.normal(
                                                "e2",
                                                "e4"
                                        )
                                )
                );

        assertEquals(
                "Turn start time is missing.",
                exception.getMessage()
        );

        verify(gameEngine, never())
                .makeMove(
                        any(),
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void makeMoveShouldTrackMillisecondsPrecisely() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                20_000L
        );

        game.setBlackTimeRemainingMillis(
                20_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0,
                        0,
                        0
                )
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        mockCurrentTime(
                "2026-09-11T12:00:01.375Z"
        );

        mockSan(
                state,
                move,
                "e4"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        long expected =
                20_000L
                        - 1_375L
                        + game.getTimeControl()
                        .getIncrementSeconds()
                        * 1000L;

        assertEquals(
                expected,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                20_000L,
                game.getBlackTimeRemainingMillis()
        );
    }

    @Test
    void timeoutGameShouldDrawWhenOpponentCannotPossiblyCheckmate() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                10_000L
        );

        game.setBlackTimeRemainingMillis(
                20_000L
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.hasInsufficientMatingMaterial(
                state,
                PieceColor.BLACK
        )).thenReturn(
                true
        );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        Game result =
                gameService.timeoutGame(
                        gameId,
                        challenger
                );

        assertEquals(
                0L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT_INSUFFICIENT_MATERIAL,
                game.getTermination()
        );

        verify(gameEngine)
                .hasInsufficientMatingMaterial(
                        state,
                        PieceColor.BLACK
                );

        verify(ratingService)
                .updateRatings(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void makeMoveShouldDrawOnTimeoutWhenOpponentCannotPossiblyCheckmate() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                5_000L
        );

        game.setBlackTimeRemainingMillis(
                100_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        12,
                        0
                )
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.hasInsufficientMatingMaterial(
                state,
                PieceColor.BLACK
        )).thenReturn(
                true
        );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        mockCurrentTime(
                "2026-09-11T12:00:05Z"
        );

        Game result =
                gameService.makeMove(
                        gameId,
                        challenger,
                        Move.normal(
                                "e2",
                                "e4"
                        )
                );

        assertEquals(
                0L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT_INSUFFICIENT_MATERIAL,
                game.getTermination()
        );

        verify(gameEngine, never())
                .makeMove(
                        any(),
                        any()
                );

        verify(gameMoveRepository, never())
                .save(
                        any()
                );

        verify(gameEngine)
                .hasInsufficientMatingMaterial(
                        state,
                        PieceColor.BLACK
                );

        verify(ratingService)
                .updateRatings(
                        game
                );

        assertSame(
                game,
                result
        );
    }

    @Test
    void makeMoveShouldSetExpirationForNextPlayer() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                100_000L
        );

        game.setBlackTimeRemainingMillis(
                80_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        0
                )
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        mockSan(
                state,
                move,
                "e4"
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(
                state
        )).thenReturn(
                "new-fen"
        );

        when(gameRepository.save(
                game
        )).thenReturn(
                game
        );

        mockCurrentTime(
                "2026-09-12T12:00:10Z"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        1,
                        30
                ),
                game.getTurnExpiresAt()
        );
    }

    @Test
    void finalizeTimeoutIfExpiredShouldFinishExpiredGame() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                5_000L
        );

        game.setBlackTimeRemainingMillis(
                100_000L
        );

        game.setTurnExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        0,
                        5
                )
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.hasInsufficientMatingMaterial(
                state,
                PieceColor.BLACK
        )).thenReturn(
                false
        );

        when(gameRepository.save(
                game
        )).thenReturn(
                game
        );

        mockCurrentTime(
                "2026-09-12T12:00:05Z"
        );

        boolean finalized =
                gameService.finalizeTimeoutIfExpired(
                        gameId
                );

        assertTrue(
                finalized
        );

        assertEquals(
                0L,
                game.getWhiteTimeRemainingMillis()
        );

        assertEquals(
                GameStatus.FINISHED,
                game.getStatus()
        );

        assertEquals(
                GameResult.BLACK_WIN,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT,
                game.getTermination()
        );

        assertNull(
                game.getTurnExpiresAt()
        );

        verify(gameEngine)
                .hasInsufficientMatingMaterial(
                        state,
                        PieceColor.BLACK
                );

        verify(ratingService)
                .updateRatings(
                        game
                );

        verify(gameRepository)
                .save(
                        game
                );
    }

    @Test
    void finalizeTimeoutIfExpiredShouldNotFinishWhenDeadlineWasMoved() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setTurnExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        0,
                        6
                )
        );

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        mockCurrentTime(
                "2026-09-12T12:00:05Z"
        );

        boolean finalized =
                gameService.finalizeTimeoutIfExpired(
                        gameId
                );

        assertFalse(
                finalized
        );

        assertEquals(
                GameStatus.IN_PROGRESS,
                game.getStatus()
        );

        verify(gameEngine, never())
                .fromFen(
                        anyString()
                );

        verify(ratingService, never())
                .updateRatings(
                        any()
                );

        verify(gameRepository, never())
                .save(
                        any()
                );
    }

    @Test
    void finalizeTimeoutIfExpiredShouldDrawWhenOpponentCannotCheckmate() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                5_000L
        );

        game.setBlackTimeRemainingMillis(
                100_000L
        );

        game.setTurnExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        12,
                        0,
                        5
                )
        );

        GameState state =
                GameState.initial();

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.hasInsufficientMatingMaterial(
                state,
                PieceColor.BLACK
        )).thenReturn(
                true
        );

        when(gameRepository.save(
                game
        )).thenReturn(
                game
        );

        mockCurrentTime(
                "2026-09-12T12:00:05Z"
        );

        boolean finalized =
                gameService.finalizeTimeoutIfExpired(
                        gameId
                );

        assertTrue(
                finalized
        );

        assertEquals(
                GameResult.DRAW,
                game.getResult()
        );

        assertEquals(
                GameTermination.TIMEOUT_INSUFFICIENT_MATERIAL,
                game.getTermination()
        );

        assertEquals(
                0L,
                game.getWhiteTimeRemainingMillis()
        );
    }

    // =========================================================
    // CONCURRENCY / LOCKING
    // =========================================================

    @Test
    void makeMoveShouldLoadGameForUpdate() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        initializeTestClock(
                game
        );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        mockSan(
                state,
                move,
                "e4"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        verify(gameRepository)
                .findByIdForUpdate(
                        gameId
                );

        verify(gameRepository, never())
                .findById(
                        gameId
                );
    }

    // =========================================================
    // SAN NOTATION
    // =========================================================

    @Test
    void makeMoveShouldSaveSanNotation() {

        Long gameId = 1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        initializeTestClock(game);

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                new Move(
                        Square.fromAlgebraic("e2"),
                        Square.fromAlgebraic("e4")
                );

        when(gameRepository.findByIdForUpdate(gameId))
                .thenReturn(
                        Optional.of(game)
                );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "new-fen"
                );

        when(gameRepository.save(game))
                .thenReturn(
                        game
                );

        mockSan(
                state,
                move,
                "e4"
        );

        gameService.makeMove(
                gameId,
                challenger,
                move
        );

        ArgumentCaptor<GameMove> captor =
                ArgumentCaptor.forClass(
                        GameMove.class
                );

        verify(gameMoveRepository)
                .save(
                        captor.capture()
                );

        GameMove savedMove =
                captor.getValue();

        assertEquals(
                "e4",
                savedMove.getSan()
        );

        assertEquals(
                "e2",
                savedMove.getFromSquare()
        );

        assertEquals(
                "e4",
                savedMove.getToSquare()
        );

        assertEquals(
                "new-fen",
                savedMove.getFenAfter()
        );
    }

    // =========================================================
    // PGN NOTATION
    // =========================================================

    @Test
    void finishGameShouldGenerateAndStorePgn() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        GameMove move1 =
                GameMove.builder()
                        .game(game)
                        .plyNumber(1)
                        .san("e4")
                        .build();

        GameMove move2 =
                GameMove.builder()
                        .game(game)
                        .plyNumber(2)
                        .san("e5")
                        .build();

        GameMove move3 =
                GameMove.builder()
                        .game(game)
                        .plyNumber(3)
                        .san("Nf3")
                        .build();

        List<GameMove> moves =
                List.of(
                        move1,
                        move2,
                        move3
                );

        when(gameMoveRepository
                .findByGameIdOrderByPlyNumberAsc(
                        gameId
                ))
                .thenReturn(
                        moves
                );

        when(pgnGenerator.generate(
                moves,
                GameResult.WHITE_WIN
        )).thenReturn(
                "1. e4 e5 2. Nf3 1-0"
        );

        gameService.finishGame(
                game,
                GameResult.WHITE_WIN,
                GameTermination.CHECKMATE
        );

        assertEquals(
                "1. e4 e5 2. Nf3 1-0",
                game.getPgn()
        );

        verify(pgnGenerator)
                .generate(
                        moves,
                        GameResult.WHITE_WIN
                );

        verify(ratingService)
                .updateRatings(
                        game
                );
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    @Test
    void makeMoveAndGetStateShouldReturnMappedResponse() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        initializeTestClock(
                game
        );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        GameStateResponse response =
                mock(
                        GameStateResponse.class
                );

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        when(gameEngine.resolveMove(
                state,
                Square.fromAlgebraic("e2"),
                Square.fromAlgebraic("e4"),
                null
        )).thenReturn(
                move
        );

        mockSan(
                state,
                move,
                "e4"
        );

        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(
                state
        )).thenReturn(
                "new-fen"
        );

        when(gameRepository.save(
                game
        )).thenReturn(
                game
        );

        when(gameStateMapper.toResponse(
                game
        )).thenReturn(
                response
        );

        GameStateResponse result =
                gameService.makeMoveAndGetState(
                        gameId,
                        challenger,
                        "e2",
                        "e4",
                        null
                );

        assertSame(
                response,
                result
        );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }

    @Test
    void finalizeTimeoutIfExpiredAndGetStateShouldReturnMappedState() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setCurrentFen(
                "current-fen"
        );

        game.setWhiteTimeRemainingMillis(
                10_000L
        );

        game.setBlackTimeRemainingMillis(
                10_000L
        );

        mockCurrentTime(
                "2026-09-12T10:00:00Z"
        );

        game.setTurnExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        9,
                        59,
                        59
                )
        );

        GameState state =
                GameState.initial();

        GameStateResponse response =
                mock(
                        GameStateResponse.class
                );

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameEngine.fromFen(
                "current-fen"
        )).thenReturn(
                state
        );

        /*
         * Keep here the same stubbing that your existing
         * finalizeTimeoutIfExpired success test uses for
         * mating-material / timeout evaluation.
         */

        when(gameRepository.save(
                game
        )).thenReturn(
                game
        );

        when(gameRepository.findById(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        when(gameStateMapper.toResponse(
                game
        )).thenReturn(
                response
        );

        Optional<GameStateResponse> result =
                gameService
                        .finalizeTimeoutIfExpiredAndGetState(
                                gameId
                        );

        assertTrue(
                result.isPresent()
        );

        assertSame(
                response,
                result.get()
        );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }

    @Test
    void finalizeTimeoutIfExpiredAndGetStateShouldReturnEmptyWhenNotExpired() {

        Long gameId =
                1L;

        Game game =
                createWaitingGame(
                        challenger,
                        opponent,
                        TimeControl.values()[0]
                );

        setId(
                game,
                gameId
        );

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        mockCurrentTime(
                "2026-09-12T10:00:00Z"
        );

        game.setTurnExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        10,
                        0,
                        1
                )
        );

        when(gameRepository.findByIdForUpdate(
                gameId
        )).thenReturn(
                Optional.of(game)
        );

        Optional<GameStateResponse> result =
                gameService
                        .finalizeTimeoutIfExpiredAndGetState(
                                gameId
                        );

        assertTrue(
                result.isEmpty()
        );

        verifyNoInteractions(
                gameStateMapper
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void mockSan(
            GameState afterState,
            Move move,
            String san
    ) {

        when(gameEngine.generateSan(
                any(GameState.class),
                eq(move),
                eq(afterState)
        )).thenReturn(
                san
        );
    }

    private void mockEmptyRepetitionHistory(
            Long gameId
    ) {

        when(gameEngine.createRepetitionTracker(
                any(GameState.class)
        )).thenReturn(
                repetitionTracker
        );

        when(gameMoveRepository
                .findByGameIdOrderByPlyNumberAsc(gameId))
                .thenReturn(
                        List.of()
                );
    }

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

    private void initializeTestClock(
            Game game
    ) {

        game.setWhiteTimeRemainingMillis(
                300_000L
        );

        game.setBlackTimeRemainingMillis(
                300_000L
        );

        game.setTurnStartedAt(
                LocalDateTime.now(clock)
        );
    }

}