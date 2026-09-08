package com.example.chessforge.service;

import com.example.chessforge.model.entity.Challenge;
import com.example.chessforge.model.entity.Game;
import com.example.chessforge.model.entity.User;
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

    @Transactional
    public Game createGameFromChallenge(Challenge challenge) {

        if (challenge.getStatus() != ChallengeStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "A game can only be created from an accepted challenge."
            );
        }

        User whitePlayer;
        User blackPlayer;

        switch (challenge.getColorPreference()) {

            case WHITE -> {
                whitePlayer = challenge.getChallenger();
                blackPlayer = challenge.getOpponent();
            }

            case BLACK -> {
                whitePlayer = challenge.getOpponent();
                blackPlayer = challenge.getChallenger();
            }

            case RANDOM -> {
                boolean challengerIsWhite =
                        ThreadLocalRandom.current().nextBoolean();

                if (challengerIsWhite) {
                    whitePlayer = challenge.getChallenger();
                    blackPlayer = challenge.getOpponent();
                } else {
                    whitePlayer = challenge.getOpponent();
                    blackPlayer = challenge.getChallenger();
                }
            }

            default -> throw new IllegalStateException(
                    "Unsupported color preference."
            );
        }

        Game game = Game.builder()
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .timeControl(challenge.getTimeControl())
                .rated(challenge.isRated())
                .status(GameStatus.WAITING)
                .build();

        return gameRepository.save(game);
    }

    @Transactional
    public void startGame(Game game) {

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException(
                    "Only a waiting game can be started."
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

        // Rating calculation will be added through RatingService.
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
    }

    public List<Game> getGamesForUser(User user) {
        return gameRepository
                .findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
                        user,
                        user
                );
    }

    private void setRatingSnapshots(Game game) {

        TimeControlType type =
                game.getTimeControl().getType();

        int whiteRating =
                getRating(game.getWhitePlayer(), type);

        int blackRating =
                getRating(game.getBlackPlayer(), type);

        game.setWhiteRatingBefore(whiteRating);
        game.setBlackRatingBefore(blackRating);

        // Until RatingService calculates changes,
        // "after" starts equal to "before".
        game.setWhiteRatingAfter(whiteRating);
        game.setBlackRatingAfter(blackRating);
    }

    private int getRating(
            User user,
            TimeControlType type
    ) {

        return switch (type) {
            case BULLET -> user.getBulletRating();
            case BLITZ -> user.getBlitzRating();
            case RAPID -> user.getRapidRating();
            case CLASSICAL -> user.getClassicalRating();
        };
    }
}