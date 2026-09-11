package com.example.chessforge.service.game;

import com.example.chessforge.model.entity.challenge.Challenge;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.challenge.ChallengeStatus;
import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.game.GameTermination;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.model.enums.timeControl.TimeControlType;
import com.example.chessforge.model.enums.tournament.TournamentMatchStatus;
import com.example.chessforge.repository.game.GameRepository;
import com.example.chessforge.service.game.engine.GameEngine;
import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.model.Move;
import com.example.chessforge.service.game.engine.model.PieceColor;
import com.example.chessforge.service.tournament.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final RatingService ratingService;
    private final TournamentService tournamentService;
    private final GameEngine gameEngine;

    @Transactional
    public Game createGameFromChallenge(Challenge challenge) {

        if (challenge.getStatus() != ChallengeStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "A game can only be created from an accepted challenge."
            );
        }

        boolean challengerIsWhite = switch (challenge.getColorPreference()) {
            case WHITE -> true;
            case BLACK -> false;
            case RANDOM -> ThreadLocalRandom.current().nextBoolean();
        };

        User whitePlayer = challengerIsWhite
                ? challenge.getChallenger()
                : challenge.getOpponent();

        User blackPlayer = challengerIsWhite
                ? challenge.getOpponent()
                : challenge.getChallenger();

        return createGame(
                whitePlayer,
                blackPlayer,
                challenge.getTimeControl(),
                challenge.isRated(),
                null
        );
    }

    @Transactional
    public Game createGameFromTournament(TournamentMatch match) {

        if (match.getBlackParticipant() == null) {
            throw new IllegalStateException(
                    "A bye match cannot create a game."
            );
        }

        Tournament tournament = match.getTournament();

        return createGame(
                match.getWhiteParticipant().getUser(),
                match.getBlackParticipant().getUser(),
                tournament.getTimeControl(),
                tournament.isRated(),
                match
        );
    }

    @Transactional
    public void startGame(Game game) {

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException(
                    "Only a waiting game can be started."
            );
        }

        if (hasActiveGame(game.getWhitePlayer())) {
            throw new IllegalStateException(
                    "White player already has an active game."
            );
        }

        if (hasActiveGame(game.getBlackPlayer())) {
            throw new IllegalStateException(
                    "Black player already has an active game."
            );
        }

        TournamentMatch tournamentMatch =
                game.getTournamentMatch();

        if (tournamentMatch != null) {

            if (tournamentMatch.getStatus()
                    == TournamentMatchStatus.COMPLETED) {

                throw new IllegalStateException(
                        "A game cannot be started for a completed tournament match."
                );
            }

            if (tournamentMatch.getStatus()
                    == TournamentMatchStatus.PENDING) {

                tournamentMatch.setStatus(
                        TournamentMatchStatus.IN_PROGRESS
                );
            }
        }

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setStartedAt(
                LocalDateTime.now()
        );

        int initialTime =
                game.getTimeControl()
                        .getInitialTimeSeconds();

        game.setWhiteTimeRemaining(
                initialTime
        );

        game.setBlackTimeRemaining(
                initialTime
        );

        game.setCurrentFen(
                gameEngine.toFen(
                        GameState.initial()
                )
        );

        setRatingSnapshots(game);
    }

    @Transactional
    public void finishGame(
            Game game,
            GameResult result,
            GameTermination termination
    ) {

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only an active game can be finished."
            );
        }

        if (result == null) {
            throw new IllegalArgumentException(
                    "Game result cannot be null."
            );
        }

        if (termination == null) {
            throw new IllegalArgumentException(
                    "Game termination cannot be null."
            );
        }

        game.setResult(result);
        game.setTermination(termination);
        game.setStatus(GameStatus.FINISHED);
        game.setFinishedAt(LocalDateTime.now());

        ratingService.updateRatings(game);

        if (game.getTournamentMatch() != null) {
            tournamentService.recordGameResult(game);
        }
    }

    @Transactional
    public void abortGame(Game game) {

        if (game.getStatus() == GameStatus.FINISHED) {
            throw new IllegalStateException(
                    "A finished game cannot be aborted."
            );
        }

        if (game.getStatus() == GameStatus.ABORTED) {
            throw new IllegalStateException(
                    "Game is already aborted."
            );
        }

        game.setStatus(GameStatus.ABORTED);
        game.setTermination(GameTermination.ABORTED);
        game.setFinishedAt(LocalDateTime.now());

        game.setWhiteRatingAfter(game.getWhiteRatingBefore());
        game.setBlackRatingAfter(game.getBlackRatingBefore());
    }

    public List<Game> getGamesForUser(User user) {
        return gameRepository
                .findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
                        user,
                        user
                );
    }

    @Transactional
    public Game makeMove(
            Long gameId,
            User player,
            Move move
    ) {

        Game game =
                gameRepository.findById(gameId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Game not found."
                                )
                        );

        if (game.getStatus()
                != GameStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Game is not in progress."
            );
        }

        GameState state =
                loadGameState(game);

        validatePlayerTurn(
                game,
                player,
                state
        );

        gameEngine.makeMove(
                state,
                move
        );

        updateGameState(
                game,
                state
        );

        finishIfGameEnded(
                game,
                state
        );

        return gameRepository.save(
                game
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void setRatingSnapshots(Game game) {

        TimeControlType type = game.getTimeControl().getType();

        int whiteRating = ratingService.getRating(
                game.getWhitePlayer(),
                type
        );

        int blackRating = ratingService.getRating(
                game.getBlackPlayer(),
                type
        );

        game.setWhiteRatingBefore(whiteRating);
        game.setBlackRatingBefore(blackRating);
    }

    private boolean hasActiveGame(User player) {
        return gameRepository.existsByPlayerAndStatus(
                player,
                GameStatus.IN_PROGRESS
        );
    }

    private Game createGame(
            User whitePlayer,
            User blackPlayer,
            TimeControl timeControl,
            boolean rated,
            TournamentMatch tournamentMatch
    ) {

        Game game =
                Game.builder()
                        .whitePlayer(whitePlayer)
                        .blackPlayer(blackPlayer)
                        .timeControl(timeControl)
                        .rated(rated)
                        .status(GameStatus.WAITING)
                        .tournamentMatch(tournamentMatch)
                        .build();

        initializeGameState(game);

        return gameRepository.save(game);
    }

    private GameState loadGameState(
            Game game
    ) {

        String currentFen =
                game.getCurrentFen();

        if (currentFen == null
                || currentFen.isBlank()) {

            throw new IllegalStateException(
                    "Game does not have a current FEN."
            );
        }

        return gameEngine.fromFen(
                currentFen
        );
    }

    private void updateGameState(
            Game game,
            GameState state
    ) {

        game.setCurrentFen(
                gameEngine.toFen(
                        state
                )
        );
    }

    private void validatePlayerTurn(
            Game game,
            User player,
            GameState state
    ) {

        User expectedPlayer =
                state.getSideToMove()
                        == PieceColor.WHITE
                        ? game.getWhitePlayer()
                        : game.getBlackPlayer();

        if (!expectedPlayer.getId()
                .equals(player.getId())) {

            throw new IllegalStateException(
                    "It is not this player's turn."
            );
        }
    }

    private void initializeGameState(
            Game game
    ) {

        game.setCurrentFen(
                gameEngine.toFen(
                        GameState.initial()
                )
        );
    }

    private void finishIfGameEnded(
            Game game,
            GameState state
    ) {

        if (gameEngine.isCheckmate(state)) {

            GameResult result =
                    state.getSideToMove()
                            == PieceColor.WHITE
                            ? GameResult.BLACK_WIN
                            : GameResult.WHITE_WIN;

            finishGame(
                    game,
                    result,
                    GameTermination.CHECKMATE
            );

            return;
        }

        if (gameEngine.isStalemate(state)) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.STALEMATE
            );

            return;
        }

        if (gameEngine.isInsufficientMaterial(state)) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.INSUFFICIENT_MATERIAL
            );
        }
    }
}