package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.model.enums.tournament.*;
import com.example.chessforge.model.enums.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentLeaderboardServiceTest {

    private TournamentLeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService =
                new TournamentLeaderboardService();
    }

    // =========================================================
    // PRIMARY SCORE
    // =========================================================

    @Test
    void higherTournamentScoreShouldAlwaysRankFirst() {

        Tournament tournament =
                createTournament(
                        List.of(TieBreakType.RATING)
                );

        TournamentParticipant first =
                createParticipant(
                        1L,
                        "lowRated",
                        1,
                        3.0,
                        1000
                );

        TournamentParticipant second =
                createParticipant(
                        2L,
                        "highRated",
                        2,
                        2.0,
                        2500
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(first, second),
                        List.of()
                );

        assertEquals(
                first,
                result.get(0)
        );

        assertEquals(
                second,
                result.get(1)
        );
    }

    // =========================================================
    // WINS
    // =========================================================

    @Test
    void winsShouldBreakEqualScoreTie() {

        Tournament tournament =
                createTournament(
                        List.of(TieBreakType.WINS)
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 1.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 1.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 0.0, 1500
                );

        TournamentParticipant d =
                createParticipant(
                        4L, "d", 4, 0.0, 1500
                );

        TournamentParticipant e =
                createParticipant(
                        5L, "e", 5, 0.0, 1500
                );

        TournamentParticipant f =
                createParticipant(
                        6L, "f", 6, 0.0, 1500
                );

        List<TournamentMatch> matches =
                List.of(
                        completedMatch(
                                tournament,
                                a,
                                c,
                                1.0,
                                0.0
                        ),
                        completedMatch(
                                tournament,
                                a,
                                d,
                                0.0,
                                1.0
                        ),
                        completedMatch(
                                tournament,
                                b,
                                e,
                                0.5,
                                0.5
                        ),
                        completedMatch(
                                tournament,
                                b,
                                f,
                                0.5,
                                0.5
                        )
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        matches
                );

        /*
         * Both have 1 tournament point.
         *
         * A: 1 win
         * B: 0 wins
         */
        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
    }

    // =========================================================
    // BLACK WINS
    // =========================================================

    @Test
    void blackWinsShouldBreakEqualScoreTie() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.BLACK_WINS
                        )
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 1.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 1.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 0.0, 1500
                );

        TournamentParticipant d =
                createParticipant(
                        4L, "d", 4, 0.0, 1500
                );

        List<TournamentMatch> matches =
                List.of(
                        /*
                         * A wins as BLACK.
                         */
                        completedMatch(
                                tournament,
                                c,
                                a,
                                0.0,
                                1.0
                        ),

                        /*
                         * B wins as WHITE.
                         */
                        completedMatch(
                                tournament,
                                b,
                                d,
                                1.0,
                                0.0
                        )
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        matches
                );

        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
    }

    // =========================================================
    // RATING
    // =========================================================

    @Test
    void ratingShouldUseRatingAtTournamentStart() {

        Tournament tournament =
                createTournament(
                        List.of(TieBreakType.RATING)
                );

        TournamentParticipant a =
                createParticipant(
                        1L,
                        "a",
                        1,
                        2.0,
                        1400
                );

        TournamentParticipant b =
                createParticipant(
                        2L,
                        "b",
                        2,
                        2.0,
                        1700
                );

        /*
         * Current User ratings intentionally say
         * the opposite.
         *
         * Leaderboard must NOT use them.
         */
        a.getUser().setBlitzRating(2500);
        b.getUser().setBlitzRating(500);

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        List.of()
                );

        assertEquals(
                b,
                result.get(0)
        );

        assertEquals(
                a,
                result.get(1)
        );
    }

    @Test
    void equalStartRatingsShouldContinueToNextTieBreak() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.RATING,
                                TieBreakType.ALPHABETICAL
                        )
                );

        TournamentParticipant zulu =
                createParticipant(
                        1L,
                        "Zulu",
                        1,
                        2.0,
                        1600
                );

        TournamentParticipant alpha =
                createParticipant(
                        2L,
                        "Alpha",
                        2,
                        2.0,
                        1600
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(zulu, alpha),
                        List.of()
                );

        assertEquals(alpha, result.get(0));
        assertEquals(zulu, result.get(1));
    }

    // =========================================================
    // ALPHABETICAL
    // =========================================================

    @Test
    void alphabeticalShouldOrderEqualPlayersCaseInsensitively() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.ALPHABETICAL
                        )
                );

        TournamentParticipant bravo =
                createParticipant(
                        1L,
                        "bravo",
                        1,
                        1.0,
                        1500
                );

        TournamentParticipant alpha =
                createParticipant(
                        2L,
                        "Alpha",
                        2,
                        1.0,
                        1500
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(bravo, alpha),
                        List.of()
                );

        assertEquals(alpha, result.get(0));
        assertEquals(bravo, result.get(1));
    }

    // =========================================================
    // DIRECT ENCOUNTER
    // =========================================================

    @Test
    void directEncounterShouldRankHeadToHeadWinnerFirst() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.DIRECT_ENCOUNTER
                        )
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 2.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 2.0, 1500
                );

        TournamentMatch match =
                completedMatch(
                        tournament,
                        a,
                        b,
                        0.0,
                        1.0
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        List.of(match)
                );

        assertEquals(b, result.get(0));
        assertEquals(a, result.get(1));
    }

    @Test
    void directEncounterShouldUseMiniTableForThreeTiedPlayers() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.DIRECT_ENCOUNTER
                        )
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 2.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 2.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 2.0, 1500
                );

        List<TournamentMatch> matches =
                List.of(
                        // A beats B
                        completedMatch(
                                tournament,
                                a,
                                b,
                                1.0,
                                0.0
                        ),

                        // B beats C
                        completedMatch(
                                tournament,
                                b,
                                c,
                                1.0,
                                0.0
                        ),

                        // A draws C
                        completedMatch(
                                tournament,
                                a,
                                c,
                                0.5,
                                0.5
                        )
                );

        /*
         * Direct encounter mini-table:
         *
         * A = 1.5
         * B = 1.0
         * C = 0.5
         */
        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b, c),
                        matches
                );

        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
        assertEquals(c, result.get(2));
    }

    // =========================================================
    // BUCHHOLZ
    // =========================================================

    @Test
    void buchholzShouldUseSumOfOpponentScores() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.BUCHHOLZ
                        )
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 2.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 2.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 3.0, 1500
                );

        TournamentParticipant d =
                createParticipant(
                        4L, "d", 4, 2.0, 1500
                );

        TournamentParticipant e =
                createParticipant(
                        5L, "e", 5, 0.0, 1500
                );

        List<TournamentMatch> matches =
                List.of(
                        completedMatch(
                                tournament,
                                a,
                                c,
                                1.0,
                                0.0
                        ),
                        completedMatch(
                                tournament,
                                a,
                                d,
                                1.0,
                                0.0
                        ),
                        completedMatch(
                                tournament,
                                b,
                                c,
                                1.0,
                                0.0
                        ),
                        completedMatch(
                                tournament,
                                b,
                                e,
                                1.0,
                                0.0
                        )
                );

        /*
         * A Buchholz = 3 + 2 = 5
         * B Buchholz = 3 + 0 = 3
         */
        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        matches
                );

        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
    }

    // =========================================================
    // BUCHHOLZ CUT 1
    // =========================================================

    @Test
    void buchholzCutOneShouldRemoveLowestOpponentScore() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.BUCHHOLZ_CUT_1
                        )
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 3.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 3.0, 1500
                );

        TournamentParticipant a1 =
                createParticipant(
                        3L, "a1", 3, 4.0, 1500
                );

        TournamentParticipant a2 =
                createParticipant(
                        4L, "a2", 4, 3.0, 1500
                );

        TournamentParticipant a3 =
                createParticipant(
                        5L, "a3", 5, 0.0, 1500
                );

        TournamentParticipant b1 =
                createParticipant(
                        6L, "b1", 6, 4.0, 1500
                );

        TournamentParticipant b2 =
                createParticipant(
                        7L, "b2", 7, 2.0, 1500
                );

        TournamentParticipant b3 =
                createParticipant(
                        8L, "b3", 8, 2.0, 1500
                );

        List<TournamentMatch> matches =
                List.of(
                        completedMatch(
                                tournament, a, a1, 1.0, 0.0
                        ),
                        completedMatch(
                                tournament, a, a2, 1.0, 0.0
                        ),
                        completedMatch(
                                tournament, a, a3, 1.0, 0.0
                        ),

                        completedMatch(
                                tournament, b, b1, 1.0, 0.0
                        ),
                        completedMatch(
                                tournament, b, b2, 1.0, 0.0
                        ),
                        completedMatch(
                                tournament, b, b3, 1.0, 0.0
                        )
                );

        /*
         * Normal Buchholz:
         *
         * A = 4 + 3 + 0 = 7
         * B = 4 + 2 + 2 = 8
         *
         * CUT 1:
         *
         * A = 7 - 0 = 7
         * B = 8 - 2 = 6
         *
         * Therefore A must be ahead.
         */
        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        matches
                );

        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
    }

    // =========================================================
    // SONNEBORN-BERGER
    // =========================================================

    @Test
    void sonnebornBergerShouldWeightOpponentScoreByMatchResult() {

        Tournament tournament =
                createTournament(
                        List.of(
                                TieBreakType.SONNEBORN_BERGER
                        )
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 2.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 2.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 4.0, 1500
                );

        TournamentParticipant d =
                createParticipant(
                        4L, "d", 4, 3.0, 1500
                );

        TournamentParticipant e =
                createParticipant(
                        5L, "e", 5, 3.0, 1500
                );

        TournamentParticipant f =
                createParticipant(
                        6L, "f", 6, 2.0, 1500
                );

        List<TournamentMatch> matches =
                List.of(
                        /*
                         * A:
                         * win vs C -> 1 * 4 = 4
                         * loss vs D -> 0 * 3 = 0
                         *
                         * SB = 4
                         */
                        completedMatch(
                                tournament,
                                a,
                                c,
                                1.0,
                                0.0
                        ),

                        completedMatch(
                                tournament,
                                a,
                                d,
                                0.0,
                                1.0
                        ),

                        /*
                         * B:
                         * draw vs E -> 0.5 * 3 = 1.5
                         * win vs F  -> 1 * 2 = 2
                         *
                         * SB = 3.5
                         */
                        completedMatch(
                                tournament,
                                b,
                                e,
                                0.5,
                                0.5
                        ),

                        completedMatch(
                                tournament,
                                b,
                                f,
                                1.0,
                                0.0
                        )
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        matches
                );

        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
    }

    // =========================================================
    // ORDER OF TIE-BREAKS
    // =========================================================

    @Test
    void configuredTieBreakOrderShouldMatter() {

        TournamentParticipant a =
                createParticipant(
                        1L,
                        "a",
                        1,
                        2.0,
                        1200
                );

        TournamentParticipant b =
                createParticipant(
                        2L,
                        "b",
                        2,
                        2.0,
                        1800
                );

        TournamentParticipant c =
                createParticipant(
                        3L,
                        "c",
                        3,
                        0.0,
                        1500
                );

        TournamentParticipant d =
                createParticipant(
                        4L,
                        "d",
                        4,
                        0.0,
                        1500
                );

        List<TournamentMatch> matches =
                List.of(
                        /*
                         * A has a real win.
                         * B has only a draw.
                         */
                        completedMatch(
                                null,
                                a,
                                c,
                                1.0,
                                0.0
                        ),

                        completedMatch(
                                null,
                                b,
                                d,
                                0.5,
                                0.5
                        )
                );

        Tournament ratingFirst =
                createTournament(
                        List.of(
                                TieBreakType.RATING,
                                TieBreakType.WINS
                        )
                );

        List<TournamentParticipant> byRating =
                leaderboardService.calculateStandings(
                        ratingFirst,
                        List.of(a, b),
                        matches
                );

        assertEquals(
                b,
                byRating.get(0)
        );

        Tournament winsFirst =
                createTournament(
                        List.of(
                                TieBreakType.WINS,
                                TieBreakType.RATING
                        )
                );

        List<TournamentParticipant> byWins =
                leaderboardService.calculateStandings(
                        winsFirst,
                        List.of(a, b),
                        matches
                );

        assertEquals(
                a,
                byWins.get(0)
        );
    }

    // =========================================================
    // PENDING / BYE
    // =========================================================

    @Test
    void pendingMatchesShouldNotCountForTieBreaks() {

        Tournament tournament =
                createTournament(
                        List.of(TieBreakType.WINS)
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 1.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 1.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 0.0, 1500
                );

        TournamentParticipant d =
                createParticipant(
                        4L, "d", 4, 0.0, 1500
                );

        TournamentMatch fakeFutureWin =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(a)
                        .blackParticipant(c)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .whiteScore(1.0)
                        .blackScore(0.0)
                        .build();

        TournamentMatch realWin =
                completedMatch(
                        tournament,
                        b,
                        d,
                        1.0,
                        0.0
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        List.of(
                                fakeFutureWin,
                                realWin
                        )
                );

        assertEquals(
                b,
                result.get(0)
        );
    }

    @Test
    void byeShouldNotCountAsAWinTieBreak() {

        Tournament tournament =
                createTournament(
                        List.of(TieBreakType.WINS)
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 1.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 1.0, 1500
                );

        TournamentParticipant c =
                createParticipant(
                        3L, "c", 3, 0.0, 1500
                );

        TournamentMatch bye =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(a)
                        .blackParticipant(null)
                        .status(
                                TournamentMatchStatus.COMPLETED
                        )
                        .termination(
                                TournamentMatchTermination.BYE
                        )
                        .whiteScore(1.0)
                        .blackScore(null)
                        .build();

        TournamentMatch realWin =
                completedMatch(
                        tournament,
                        b,
                        c,
                        1.0,
                        0.0
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        List.of(
                                bye,
                                realWin
                        )
                );

        assertEquals(
                b,
                result.get(0)
        );
    }

    @Test
    void forfeitVictoryShouldCountAsWin() {

        Tournament tournament =
                createTournament(
                        List.of(TieBreakType.WINS)
                );

        TournamentParticipant a =
                createParticipant(
                        1L, "a", 1, 1.0, 1500
                );

        TournamentParticipant b =
                createParticipant(
                        2L, "b", 2, 1.0, 1500
                );

        TournamentParticipant opponent =
                createParticipant(
                        3L, "opponent", 3, 0.0, 1500
                );

        TournamentMatch forfeitWin =
                completedMatch(
                        tournament,
                        a,
                        opponent,
                        1.0,
                        0.0
                );

        forfeitWin.setTermination(
                TournamentMatchTermination.BLACK_FORFEIT
        );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(a, b),
                        List.of(forfeitWin)
                );

        assertEquals(
                a,
                result.get(0)
        );
    }

    // =========================================================
    // FALLBACK
    // =========================================================

    @Test
    void noConfiguredTieBreaksShouldFallbackToSeedOrder() {

        Tournament tournament =
                createTournament(
                        List.of()
                );

        TournamentParticipant seedThree =
                createParticipant(
                        3L,
                        "c",
                        3,
                        2.0,
                        1500
                );

        TournamentParticipant seedOne =
                createParticipant(
                        1L,
                        "a",
                        1,
                        2.0,
                        1500
                );

        TournamentParticipant seedTwo =
                createParticipant(
                        2L,
                        "b",
                        2,
                        2.0,
                        1500
                );

        List<TournamentParticipant> result =
                leaderboardService.calculateStandings(
                        tournament,
                        List.of(
                                seedThree,
                                seedOne,
                                seedTwo
                        ),
                        List.of()
                );

        assertEquals(
                List.of(
                        seedOne,
                        seedTwo,
                        seedThree
                ),
                result
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Tournament createTournament(
            List<TieBreakType> tieBreaks
    ) {

        return Tournament.builder()
                .name("Test Tournament")
                .format(TournamentFormat.SWISS)
                .timeControl(
                        TimeControl.values()[0]
                )
                .status(
                        TournamentStatus.IN_PROGRESS
                )
                .tieBreaks(
                        new ArrayList<>(tieBreaks)
                )
                .build();
    }

    private TournamentParticipant createParticipant(
            Long id,
            String username,
            int seed,
            double score,
            int ratingAtStart
    ) {

        User user =
                User.builder()
                        .username(username)
                        .email(
                                username
                                        + id
                                        + "@test.com"
                        )
                        .password("password")
                        .status(UserStatus.ACTIVE)
                        .bulletRating(400)
                        .blitzRating(400)
                        .rapidRating(400)
                        .classicalRating(400)
                        .build();

        setId(
                user,
                id
        );

        TournamentParticipant participant =
                TournamentParticipant.builder()
                        .user(user)
                        .seed(seed)
                        .score(score)
                        .ratingAtStart(
                                ratingAtStart
                        )
                        .status(
                                TournamentParticipantStatus.ACTIVE
                        )
                        .build();

        setId(
                participant,
                100L + id
        );

        return participant;
    }

    private TournamentMatch completedMatch(
            Tournament tournament,
            TournamentParticipant white,
            TournamentParticipant black,
            double whiteScore,
            double blackScore
    ) {

        return TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(white)
                .blackParticipant(black)
                .status(
                        TournamentMatchStatus.COMPLETED
                )
                .whiteScore(whiteScore)
                .blackScore(blackScore)
                .build();
    }

    private void setId(
            BaseEntity entity,
            Long id
    ) {

        try {

            var field =
                    BaseEntity.class
                            .getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}