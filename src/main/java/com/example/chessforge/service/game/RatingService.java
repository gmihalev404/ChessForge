package com.example.chessforge.service.game;

import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.timeControl.TimeControlType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RatingService {

    private static final int K_FACTOR = 16;

    @Transactional
    public void updateRatings(Game game) {

        if (!game.isRated()) {
            game.setWhiteRatingAfter(game.getWhiteRatingBefore());
            game.setBlackRatingAfter(game.getBlackRatingBefore());
            return;
        }

        if (game.getResult() == null) {
            throw new IllegalStateException(
                    "Cannot update ratings for a game without a result."
            );
        }

        User white = game.getWhitePlayer();
        User black = game.getBlackPlayer();

        TimeControlType type =
                game.getTimeControl().getType();

        int whiteRating = getRating(white, type);
        int blackRating = getRating(black, type);

        double whiteScore = getWhiteScore(game.getResult());
        double blackScore = 1.0 - whiteScore;

        double whiteExpected =
                expectedScore(whiteRating, blackRating);

        double blackExpected = 1.0 - whiteExpected;

        int newWhiteRating = calculateNewRating(
                whiteRating,
                whiteScore,
                whiteExpected
        );

        int newBlackRating = calculateNewRating(
                blackRating,
                blackScore,
                blackExpected
        );

        setRating(white, type, newWhiteRating);
        setRating(black, type, newBlackRating);

        game.setWhiteRatingAfter(newWhiteRating);
        game.setBlackRatingAfter(newBlackRating);
    }

    public int getRating(
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

    // =========================================================
    // HELPERS
    // =========================================================

    private double expectedScore(
            int playerRating,
            int opponentRating
    ) {

        return 1.0 /
                (1.0 + Math.pow(
                        10.0,
                        (opponentRating - playerRating) / 400.0
                ));
    }

    private int calculateNewRating(
            int rating,
            double actualScore,
            double expectedScore
    ) {

        return (int) Math.round(
                rating +
                        K_FACTOR * (actualScore - expectedScore)
        );
    }

    private double getWhiteScore(GameResult result) {

        return switch (result) {
            case WHITE_WIN -> 1.0;
            case BLACK_WIN -> 0.0;
            case DRAW -> 0.5;
        };
    }

    private void setRating(
            User user,
            TimeControlType type,
            int rating
    ) {

        switch (type) {
            case BULLET -> user.setBulletRating(rating);
            case BLITZ -> user.setBlitzRating(rating);
            case RAPID -> user.setRapidRating(rating);
            case CLASSICAL -> user.setClassicalRating(rating);
        }
    }
}