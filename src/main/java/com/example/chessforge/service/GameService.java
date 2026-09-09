package com.example.chessforge.service;

import com.example.chessforge.model.entity.*;
import com.example.chessforge.model.enums.*;
import com.example.chessforge.repository.GameRepository;
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

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());

        int initialTime =
                game.getTimeControl().getInitialTimeSeconds();

        game.setWhiteTimeRemaining(initialTime);
        game.setBlackTimeRemaining(initialTime);

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

        if (whitePlayer.getId().equals(blackPlayer.getId())) {
            throw new IllegalArgumentException(
                    "A player cannot play against themselves."
            );
        }

        Game game = Game.builder()
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .timeControl(timeControl)
                .rated(rated)
                .status(GameStatus.WAITING)
                .tournamentMatch(tournamentMatch)
                .build();

        return gameRepository.save(game);
    }
}