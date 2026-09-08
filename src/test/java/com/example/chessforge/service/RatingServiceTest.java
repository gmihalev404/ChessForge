package com.example.chessforge.service;

import com.example.chessforge.model.entity.Game;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RatingServiceTest {

    private RatingService ratingService;

    private User white;
    private User black;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService();

        white = createUser();
        black = createUser();
    }

    // =========================================================
    // UNRATED
    // =========================================================

    @Test
    void updateRatingsShouldNotChangeUserRatingsForUnratedGame() {

        Game game = createGame(
                TimeControlType.BLITZ,
                GameResult.WHITE_WIN,
                false
        );

        game.setWhiteRatingBefore(500);
        game.setBlackRatingBefore(500);

        ratingService.updateRatings(game);

        assertEquals(500, white.getBlitzRating());
        assertEquals(500, black.getBlitzRating());

        assertEquals(500, game.getWhiteRatingAfter());
        assertEquals(500, game.getBlackRatingAfter());
    }

    // =========================================================
    // INVALID GAME
    // =========================================================

    @Test
    void updateRatingsShouldThrowWhenRatedGameHasNoResult() {

        Game game = createGame(
                TimeControlType.BLITZ,
                null,
                true
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ratingService.updateRatings(game)
        );

        assertEquals(
                "Cannot update ratings for a game without a result.",
                exception.getMessage()
        );
    }

    // =========================================================
    // EQUAL RATINGS
    // =========================================================

    @Test
    void updateRatingsShouldIncreaseWhiteAndDecreaseBlackOnWhiteWin() {

        white.setBlitzRating(400);
        black.setBlitzRating(400);

        Game game = createGame(
                TimeControlType.BLITZ,
                GameResult.WHITE_WIN,
                true
        );

        ratingService.updateRatings(game);

        assertEquals(408, white.getBlitzRating());
        assertEquals(392, black.getBlitzRating());

        assertEquals(408, game.getWhiteRatingAfter());
        assertEquals(392, game.getBlackRatingAfter());
    }

    @Test
    void updateRatingsShouldIncreaseBlackAndDecreaseWhiteOnBlackWin() {

        white.setBlitzRating(400);
        black.setBlitzRating(400);

        Game game = createGame(
                TimeControlType.BLITZ,
                GameResult.BLACK_WIN,
                true
        );

        ratingService.updateRatings(game);

        assertEquals(392, white.getBlitzRating());
        assertEquals(408, black.getBlitzRating());

        assertEquals(392, game.getWhiteRatingAfter());
        assertEquals(408, game.getBlackRatingAfter());
    }

    @Test
    void updateRatingsShouldKeepEqualRatingsAfterDraw() {

        white.setBlitzRating(400);
        black.setBlitzRating(400);

        Game game = createGame(
                TimeControlType.BLITZ,
                GameResult.DRAW,
                true
        );

        ratingService.updateRatings(game);

        assertEquals(400, white.getBlitzRating());
        assertEquals(400, black.getBlitzRating());

        assertEquals(400, game.getWhiteRatingAfter());
        assertEquals(400, game.getBlackRatingAfter());
    }

    // =========================================================
    // DIFFERENT RATINGS
    // =========================================================

    @Test
    void updateRatingsShouldRewardUpsetMoreStrongly() {

        white.setBlitzRating(400);
        black.setBlitzRating(800);

        Game game = createGame(
                TimeControlType.BLITZ,
                GameResult.WHITE_WIN,
                true
        );

        ratingService.updateRatings(game);

        /*
         * White expected score ≈ 0.0909
         *
         * 400 + 16 * (1 - 0.0909)
         * ≈ 414.55
         * rounds to 415
         *
         * Black becomes 785.
         */
        assertEquals(415, white.getBlitzRating());
        assertEquals(785, black.getBlitzRating());
    }

    // =========================================================
    // TIME CONTROL TYPES
    // =========================================================

    @ParameterizedTest
    @EnumSource(TimeControlType.class)
    void updateRatingsShouldModifyCorrectRatingCategory(
            TimeControlType type
    ) {

        int whiteBefore = ratingService.getRating(white, type);
        int blackBefore = ratingService.getRating(black, type);

        assertEquals(whiteBefore, blackBefore);

        Game game = createGame(
                type,
                GameResult.WHITE_WIN,
                true
        );

        ratingService.updateRatings(game);

        assertEquals(
                whiteBefore + 8,
                ratingService.getRating(white, type)
        );

        assertEquals(
                blackBefore - 8,
                ratingService.getRating(black, type)
        );
    }

    @Test
    void updateRatingsShouldNotModifyOtherRatingCategories() {

        Game game = createGame(
                TimeControlType.BLITZ,
                GameResult.WHITE_WIN,
                true
        );

        ratingService.updateRatings(game);

        assertEquals(400, white.getBulletRating());
        assertEquals(600, white.getRapidRating());
        assertEquals(700, white.getClassicalRating());

        assertEquals(400, black.getBulletRating());
        assertEquals(600, black.getRapidRating());
        assertEquals(700, black.getClassicalRating());

        assertEquals(508, white.getBlitzRating());
        assertEquals(492, black.getBlitzRating());
    }

    // =========================================================
    // GET RATING
    // =========================================================

    @Test
    void getRatingShouldReturnCorrectRatingForEachType() {

        assertEquals(
                400,
                ratingService.getRating(
                        white,
                        TimeControlType.BULLET
                )
        );

        assertEquals(
                500,
                ratingService.getRating(
                        white,
                        TimeControlType.BLITZ
                )
        );

        assertEquals(
                600,
                ratingService.getRating(
                        white,
                        TimeControlType.RAPID
                )
        );

        assertEquals(
                700,
                ratingService.getRating(
                        white,
                        TimeControlType.CLASSICAL
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser() {

        return User.builder()
                .username("test")
                .email("test@test.com")
                .password("password")
                .bulletRating(400)
                .blitzRating(500)
                .rapidRating(600)
                .classicalRating(700)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Game createGame(
            TimeControlType type,
            GameResult result,
            boolean rated
    ) {

        return Game.builder()
                .whitePlayer(white)
                .blackPlayer(black)
                .timeControl(findTimeControl(type))
                .status(GameStatus.FINISHED)
                .result(result)
                .rated(rated)
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
}