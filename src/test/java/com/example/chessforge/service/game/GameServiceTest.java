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
import com.example.chessforge.service.game.engine.GameEngine;
import com.example.chessforge.service.game.engine.history.RepetitionTracker;
import com.example.chessforge.service.game.engine.model.*;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        when(gameRepository.findById(gameId))
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
        mockEmptyRepetitionHistory(
                gameId
        );

        when(gameEngine.toFen(state))
                .thenReturn(
                        "checkmate-fen"
                );

        when(gameEngine.isCheckmate(state))
                .thenReturn(true);

        when(gameRepository.save(game))
                .thenReturn(game);

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

    // =========================================================
    // CREATE GAME FROM TOURNAMENT
    // =========================================================

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
// GAME ENGINE
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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

        mockEmptyRepetitionHistory(
                gameId
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
    void makeMoveShouldFinishGameOnStalemate() {

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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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

    @Test
    void makeMoveShouldStorePromotionPiece() {

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

        when(gameRepository.findById(gameId))
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

        when(gameRepository.findById(gameId))
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
    // HELPERS
    // =========================================================

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