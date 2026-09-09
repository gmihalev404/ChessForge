package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.BaseEntity;
import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TournamentPairingServiceTest {

    private TournamentPairingService pairingService;
    private Tournament tournament;

    @BeforeEach
    void setUp() {

        pairingService =
                new TournamentPairingService();

        tournament = Tournament.builder()
                .name("Test Tournament")
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .status(TournamentStatus.IN_PROGRESS)
                .byePoints(1.0)
                .build();
    }

    // =========================================================
    // SINGLE ELIMINATION - FIRST ROUND
    // =========================================================

    @Test
    void singleEliminationShouldCreateFixedBracketForEightPlayers() {

        List<TournamentParticipant> participants =
                createParticipants(8);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(4, matches.size());

        assertPairOnBoard(
                matches,
                1,
                1,
                8
        );

        assertPairOnBoard(
                matches,
                2,
                4,
                5
        );

        assertPairOnBoard(
                matches,
                3,
                2,
                7
        );

        assertPairOnBoard(
                matches,
                4,
                3,
                6
        );

        assertTrue(
                matches.stream()
                        .allMatch(match ->
                                match.getStatus()
                                        == TournamentMatchStatus.PENDING
                        )
        );
    }

    @Test
    void singleEliminationShouldGiveByesToTopSeedsForSixPlayers() {

        List<TournamentParticipant> participants =
                createParticipants(6);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(4, matches.size());

        TournamentMatch board1 =
                getBoard(matches, 1);

        TournamentMatch board3 =
                getBoard(matches, 3);

        assertEquals(
                1,
                board1.getWhiteParticipant().getSeed()
        );

        assertNull(
                board1.getBlackParticipant()
        );

        assertEquals(
                2,
                board3.getWhiteParticipant().getSeed()
        );

        assertNull(
                board3.getBlackParticipant()
        );

        assertPairOnBoard(
                matches,
                2,
                4,
                5
        );

        assertPairOnBoard(
                matches,
                4,
                3,
                6
        );
    }

    @Test
    void byeShouldRemainPendingDuringPairing() {

        tournament.setByePoints(0.5);

        List<TournamentParticipant> participants =
                createParticipants(3);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        TournamentMatch bye =
                findBye(matches);

        assertEquals(
                TournamentMatchStatus.PENDING,
                bye.getStatus()
        );

        assertNull(
                bye.getTermination()
        );

        assertNull(
                bye.getWhiteScore()
        );

        assertNull(
                bye.getBlackScore()
        );
    }

    @Test
    void singleEliminationShouldIgnoreInactiveParticipants() {

        List<TournamentParticipant> participants =
                createParticipants(5);

        participants.get(2).setStatus(
                TournamentParticipantStatus.WITHDRAWN
        );

        participants.get(3).setStatus(
                TournamentParticipantStatus.FORFEITED
        );

        participants.get(4).setStatus(
                TournamentParticipantStatus.ELIMINATED
        );

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(
                1,
                matches.size()
        );

        assertContainsPair(
                matches.get(0),
                1,
                2
        );
    }

    @Test
    void singleEliminationShouldRejectMissingSeed() {

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.get(2).setSeed(null);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                pairingService
                                        .createInitialPairings(
                                                tournament,
                                                participants
                                        )
                );

        assertEquals(
                "All participants must have a seed before pairings are created.",
                exception.getMessage()
        );
    }

    @Test
    void singleEliminationShouldRequireTwoActivePlayers() {

        List<TournamentParticipant> participants =
                createParticipants(2);

        participants.get(1).setStatus(
                TournamentParticipantStatus.FORFEITED
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        pairingService.createInitialPairings(
                                tournament,
                                participants
                        )
        );
    }

    // =========================================================
    // SINGLE ELIMINATION - FIXED BRACKET
    // =========================================================

    @Test
    void nextSingleEliminationRoundShouldFollowBracketLineage() {

        List<TournamentParticipant> participants =
                createParticipants(8);

        List<TournamentMatch> firstRound =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        /*
         * Board 1: 1 vs 8 -> 8
         * Board 2: 4 vs 5 -> 4
         * Board 3: 2 vs 7 -> 7
         * Board 4: 3 vs 6 -> 3
         *
         * Fixed bracket:
         * 8 vs 4
         * 7 vs 3
         */
        completeWithWinner(
                getBoard(firstRound, 1),
                8
        );

        completeWithWinner(
                getBoard(firstRound, 2),
                4
        );

        completeWithWinner(
                getBoard(firstRound, 3),
                7
        );

        completeWithWinner(
                getBoard(firstRound, 4),
                3
        );

        List<TournamentMatch> history =
                new ArrayList<>(firstRound);

        List<TournamentMatch> secondRound =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        2
                );

        assertEquals(
                2,
                secondRound.size()
        );

        assertPairOnBoard(
                secondRound,
                1,
                8,
                4
        );

        assertPairOnBoard(
                secondRound,
                2,
                7,
                3
        );
    }

    @Test
    void nextSingleEliminationRoundShouldUseOnlyPreviousRoundFromHistory() {

        List<TournamentParticipant> participants =
                createParticipants(8);

        List<TournamentMatch> round1 =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        for (TournamentMatch match : round1) {
            completeWithWinner(
                    match,
                    Math.min(
                            match.getWhiteParticipant().getSeed(),
                            match.getBlackParticipant().getSeed()
                    )
            );
        }

        List<TournamentMatch> round2 =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        round1,
                        2
                );

        for (TournamentMatch match : round2) {
            completeWithWinner(
                    match,
                    Math.min(
                            match.getWhiteParticipant().getSeed(),
                            match.getBlackParticipant().getSeed()
                    )
            );
        }

        List<TournamentMatch> fullHistory =
                new ArrayList<>();

        fullHistory.addAll(round1);
        fullHistory.addAll(round2);

        List<TournamentMatch> finalRound =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        fullHistory,
                        3
                );

        assertEquals(
                1,
                finalRound.size()
        );

        assertContainsPair(
                finalRound.get(0),
                1,
                2
        );
    }

    @Test
    void nextSingleEliminationRoundShouldRejectIncompletePreviousMatch() {

        List<TournamentParticipant> participants =
                createParticipants(4);

        List<TournamentMatch> firstRound =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        completeWithWinner(
                getBoard(firstRound, 1),
                1
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        pairingService.createNextRound(
                                tournament,
                                participants,
                                firstRound,
                                2
                        )
        );
    }

    @Test
    void nextSingleEliminationRoundShouldRejectDraw() {

        List<TournamentParticipant> participants =
                createParticipants(4);

        List<TournamentMatch> firstRound =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        TournamentMatch first =
                getBoard(firstRound, 1);

        first.setStatus(
                TournamentMatchStatus.COMPLETED
        );

        first.setWhiteScore(0.5);
        first.setBlackScore(0.5);

        completeWithWinner(
                getBoard(firstRound, 2),
                2
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        pairingService.createNextRound(
                                tournament,
                                participants,
                                firstRound,
                                2
                        )
        );
    }

    // =========================================================
    // ROUND ROBIN
    // =========================================================

    @Test
    void roundRobinShouldGenerateWholeScheduleForFourPlayers() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(
                6,
                matches.size()
        );

        for (int round = 1;
             round <= 3;
             round++) {

            int currentRound =
                    round;

            long count =
                    matches.stream()
                            .filter(match ->
                                    match.getRoundNumber()
                                            == currentRound
                            )
                            .count();

            assertEquals(
                    2,
                    count
            );
        }
    }

    @Test
    void roundRobinShouldMakeEveryPairPlayExactlyOnce() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        List<TournamentParticipant> participants =
                createParticipants(6);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        Set<String> pairs =
                new HashSet<>();

        for (TournamentMatch match : matches) {

            assertNotNull(
                    match.getBlackParticipant()
            );

            assertTrue(
                    pairs.add(
                            pairKey(match)
                    ),
                    "Duplicate pair: "
                            + pairKey(match)
            );
        }

        /*
         * C(6,2) = 15
         */
        assertEquals(
                15,
                pairs.size()
        );
    }

    @Test
    void roundRobinWithOddPlayersShouldGiveEachPlayerOneBye() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        List<TournamentParticipant> participants =
                createParticipants(5);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        List<TournamentMatch> byes =
                matches.stream()
                        .filter(match ->
                                match.getBlackParticipant()
                                        == null
                        )
                        .toList();

        assertEquals(
                5,
                byes.size()
        );

        for (TournamentParticipant participant
                : participants) {

            long count =
                    byes.stream()
                            .filter(match ->
                                    match.getWhiteParticipant()
                                            == participant
                            )
                            .count();

            assertEquals(
                    1,
                    count
            );
        }
    }

    @Test
    void roundRobinByesShouldNotAwardPointsWhenScheduleIsCreated() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        tournament.setByePoints(0.5);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        createParticipants(5)
                );

        for (TournamentMatch bye :
                matches.stream()
                        .filter(match ->
                                match.getBlackParticipant()
                                        == null
                        )
                        .toList()) {

            assertEquals(
                    TournamentMatchStatus.PENDING,
                    bye.getStatus()
            );

            assertNull(
                    bye.getTermination()
            );

            assertNull(
                    bye.getWhiteScore()
            );
        }
    }

    @Test
    void roundRobinShouldKeepColorsBalanced() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        List<TournamentParticipant> participants =
                createParticipants(6);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        for (TournamentParticipant participant
                : participants) {

            long white =
                    matches.stream()
                            .filter(match ->
                                    match.getWhiteParticipant()
                                            == participant
                            )
                            .count();

            long black =
                    matches.stream()
                            .filter(match ->
                                    match.getBlackParticipant()
                                            == participant
                            )
                            .count();

            assertEquals(
                    5,
                    white + black
            );

            assertTrue(
                    Math.abs(white - black)
                            <= 1
            );
        }
    }

    @Test
    void roundRobinShouldRejectCreateNextRound() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        pairingService.createNextRound(
                                tournament,
                                createParticipants(4),
                                List.of(),
                                2
                        )
        );
    }

    // =========================================================
    // SWISS - FIRST ROUND
    // =========================================================

    @Test
    void swissFirstRoundShouldSplitPlayersBySeed() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(8);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(
                4,
                matches.size()
        );

        assertPairOnBoard(
                matches,
                1,
                1,
                5
        );

        assertPairOnBoard(
                matches,
                2,
                2,
                6
        );

        assertPairOnBoard(
                matches,
                3,
                3,
                7
        );

        assertPairOnBoard(
                matches,
                4,
                4,
                8
        );
    }

    @Test
    void swissFirstRoundShouldGiveByeToLastSeedWhenOdd() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(7);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(
                4,
                matches.size()
        );

        assertPairOnBoard(
                matches,
                1,
                1,
                4
        );

        assertPairOnBoard(
                matches,
                2,
                2,
                5
        );

        assertPairOnBoard(
                matches,
                3,
                3,
                6
        );

        TournamentMatch bye =
                findBye(matches);

        assertEquals(
                7,
                bye.getWhiteParticipant()
                        .getSeed()
        );

        assertEquals(
                TournamentMatchStatus.PENDING,
                bye.getStatus()
        );

        assertNull(
                bye.getWhiteScore()
        );
    }

    // =========================================================
    // SWISS - NO REMATCH
    // =========================================================

    @Test
    void swissLaterRoundShouldNeverCreateRematch() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.forEach(
                participant ->
                        participant.setScore(1.0)
        );

        /*
         * Previous:
         * 1-3
         * 2-4
         *
         * Current halves:
         * 1 2 | 3 4
         *
         * 1 cannot play 3,
         * so 1 must play 4.
         */
        List<TournamentMatch> history =
                List.of(
                        completedMatch(
                                participants.get(0),
                                participants.get(2),
                                1
                        ),
                        completedMatch(
                                participants.get(1),
                                participants.get(3),
                                1
                        )
                );

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        2
                );

        assertContainsPair(
                matches,
                1,
                4
        );

        assertContainsPair(
                matches,
                2,
                3
        );

        for (TournamentMatch match : matches) {

            assertFalse(
                    history.stream()
                            .anyMatch(previous ->
                                    samePair(
                                            match,
                                            previous
                                    )
                            )
            );
        }
    }

    // =========================================================
    // SWISS - COLOR PREFERENCE
    // =========================================================

    @Test
    void swissShouldPreferOppositePreviousColorInsideSameSearchLocation() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(6);

        participants.forEach(
                participant ->
                        participant.setScore(1.0)
        );

        TournamentParticipant p1 =
                participants.get(0);

        TournamentParticipant p2 =
                participants.get(1);

        TournamentParticipant p3 =
                participants.get(2);

        TournamentParticipant p4 =
                participants.get(3);

        TournamentParticipant p5 =
                participants.get(4);

        TournamentParticipant p6 =
                participants.get(5);

        /*
         * Previous colors:
         *
         * p1 WHITE
         * p4 WHITE
         *
         * p2 BLACK
         *
         * p6 WHITE
         * p5 BLACK
         *
         * For p1, lower half = p4,p5,p6.
         *
         * p5 has opposite previous color,
         * so p1 should prefer p5.
         */
        List<TournamentMatch> history =
                List.of(
                        completedMatch(
                                p1,
                                p3,
                                1
                        ),
                        completedMatch(
                                p4,
                                p2,
                                1
                        ),
                        completedMatch(
                                p6,
                                p5,
                                1
                        )
                );

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        2
                );

        assertContainsPair(
                matches,
                1,
                5
        );
    }

    @Test
    void swissShouldReverseColorsWhenPreviousColorsAreOpposite() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.forEach(
                participant ->
                        participant.setScore(1.0)
        );

        TournamentParticipant p1 =
                participants.get(0);

        TournamentParticipant p2 =
                participants.get(1);

        TournamentParticipant p3 =
                participants.get(2);

        TournamentParticipant p4 =
                participants.get(3);

        /*
         * p1 was WHITE.
         * p4 was BLACK.
         */
        List<TournamentMatch> history =
                List.of(
                        completedMatch(
                                p1,
                                p3,
                                1
                        ),
                        completedMatch(
                                p2,
                                p4,
                                1
                        )
                );

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        2
                );

        TournamentMatch match =
                findPair(
                        matches,
                        1,
                        4
                );

        /*
         * Colors should reverse:
         * p1 -> BLACK
         * p4 -> WHITE
         */
        assertEquals(
                4,
                match.getWhiteParticipant()
                        .getSeed()
        );

        assertEquals(
                1,
                match.getBlackParticipant()
                        .getSeed()
        );
    }

    // =========================================================
    // SWISS - SCORE GROUP SEARCH
    // =========================================================

    @Test
    void swissShouldPairLeaderDownWhenAloneInScoreGroup() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.get(0).setScore(3.0);
        participants.get(1).setScore(2.0);
        participants.get(2).setScore(2.0);
        participants.get(3).setScore(1.0);

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        List.of(),
                        2
                );

        /*
         * Seed 1 is alone on 3 points.
         * First available player in the next
         * lower score group is seed 2.
         */
        assertContainsPair(
                matches,
                1,
                2
        );
    }

    @Test
    void swissShouldSearchSameHalfBeforeLowerScoreGroup() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(6);

        participants.get(0).setScore(2.0);
        participants.get(1).setScore(2.0);
        participants.get(2).setScore(2.0);
        participants.get(3).setScore(2.0);

        participants.get(4).setScore(1.0);
        participants.get(5).setScore(1.0);

        /*
         * Group 2.0:
         *
         * upper = 1,2
         * lower = 3,4
         *
         * Seed 1 has already played both lower-half
         * players 3 and 4.
         *
         * It must therefore search its own half
         * before dropping to the 1.0 group.
         */
        List<TournamentMatch> history =
                List.of(
                        completedMatch(
                                participants.get(0),
                                participants.get(2),
                                1
                        ),
                        completedMatch(
                                participants.get(0),
                                participants.get(3),
                                2
                        )
                );

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        3
                );

        assertContainsPair(
                matches,
                1,
                2
        );
    }

    // =========================================================
    // SWISS - BACKTRACKING
    // =========================================================

    @Test
    void swissShouldBacktrackWhenFirstValidChoiceBlocksRemainingPlayers() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.forEach(
                participant ->
                        participant.setScore(1.0)
        );

        /*
         * halves:
         * 1,2 | 3,4
         *
         * Seed 1 initially tries seed 3.
         *
         * But 2 and 4 have already played,
         * so that leaves an impossible remainder.
         *
         * Algorithm must backtrack:
         *
         * 1-4
         * 2-3
         */
        List<TournamentMatch> history =
                List.of(
                        completedMatch(
                                participants.get(1),
                                participants.get(3),
                                1
                        )
                );

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        2
                );

        assertContainsPair(
                matches,
                1,
                4
        );

        assertContainsPair(
                matches,
                2,
                3
        );
    }

    @Test
    void swissShouldThrowWhenNoPairingWithoutRematchExists() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.forEach(
                participant ->
                        participant.setScore(1.0)
        );

        List<TournamentMatch> history =
                new ArrayList<>();

        for (int i = 0;
             i < participants.size();
             i++) {

            for (int j = i + 1;
                 j < participants.size();
                 j++) {

                history.add(
                        completedMatch(
                                participants.get(i),
                                participants.get(j),
                                1
                        )
                );
            }
        }

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                pairingService.createNextRound(
                                        tournament,
                                        participants,
                                        history,
                                        2
                                )
                );

        assertEquals(
                "No valid Swiss pairing exists without a rematch.",
                exception.getMessage()
        );
    }

    // =========================================================
    // SWISS - BYE
    // =========================================================

    @Test
    void swissByeShouldGoToLowestRankedPlayerWithoutPreviousBye() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(5);

        participants.get(0).setScore(3.0);
        participants.get(1).setScore(2.0);
        participants.get(2).setScore(1.5);
        participants.get(3).setScore(1.0);
        participants.get(4).setScore(0.5);

        /*
         * Lowest-ranked seed 5 already had a BYE.
         */
        TournamentMatch oldBye =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(
                                participants.get(4)
                        )
                        .blackParticipant(null)
                        .roundNumber(1)
                        .boardNumber(3)
                        .status(
                                TournamentMatchStatus.COMPLETED
                        )
                        .whiteScore(1.0)
                        .build();

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        List.of(oldBye),
                        2
                );

        TournamentMatch bye =
                findBye(matches);

        /*
         * Seed 5 is skipped because it already
         * received a BYE.
         *
         * Next lowest is seed 4.
         */
        assertEquals(
                4,
                bye.getWhiteParticipant()
                        .getSeed()
        );
    }

    @Test
    void swissByeShouldNotCountAsPlayedMatch() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(3);

        TournamentMatch previousBye =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(
                                participants.get(2)
                        )
                        .blackParticipant(null)
                        .roundNumber(1)
                        .status(
                                TournamentMatchStatus.COMPLETED
                        )
                        .whiteScore(1.0)
                        .build();

        participants.get(0).setScore(1.0);
        participants.get(1).setScore(1.0);
        participants.get(2).setScore(1.0);

        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        List.of(previousBye),
                        2
                );

        assertNotNull(matches);
    }

    // =========================================================
    // SWISS - ACTIVE PARTICIPANTS
    // =========================================================

    @Test
    void swissShouldIgnoreForfeitedParticipants() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(6);

        TournamentParticipant forfeited =
                participants.get(5);

        forfeited.setStatus(
                TournamentParticipantStatus.FORFEITED
        );

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertTrue(
                matches.stream()
                        .noneMatch(match ->
                                match.getWhiteParticipant()
                                        == forfeited
                                        ||
                                        match.getBlackParticipant()
                                                == forfeited
                        )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private List<TournamentParticipant> createParticipants(
            int count
    ) {

        List<TournamentParticipant> participants =
                new ArrayList<>();

        for (int i = 1;
             i <= count;
             i++) {

            User user =
                    User.builder()
                            .username(
                                    "player" + i
                            )
                            .email(
                                    "player"
                                            + i
                                            + "@test.com"
                            )
                            .password("password")
                            .status(
                                    UserStatus.ACTIVE
                            )
                            .build();

            setId(
                    user,
                    (long) i
            );

            TournamentParticipant participant =
                    TournamentParticipant.builder()
                            .tournament(
                                    tournament
                            )
                            .user(user)
                            .seed(i)
                            .score(0.0)
                            .status(
                                    TournamentParticipantStatus.ACTIVE
                            )
                            .build();

            setId(
                    participant,
                    100L + i
            );

            participants.add(
                    participant
            );
        }

        return participants;
    }

    private TournamentMatch completedMatch(
            TournamentParticipant white,
            TournamentParticipant black,
            int roundNumber
    ) {

        return TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(white)
                .blackParticipant(black)
                .roundNumber(roundNumber)
                .boardNumber(1)
                .status(
                        TournamentMatchStatus.COMPLETED
                )
                .whiteScore(1.0)
                .blackScore(0.0)
                .build();
    }

    private TournamentMatch getBoard(
            List<TournamentMatch> matches,
            int boardNumber
    ) {

        return matches.stream()
                .filter(match ->
                        match.getBoardNumber()
                                == boardNumber
                )
                .findFirst()
                .orElseThrow();
    }

    private TournamentMatch findBye(
            List<TournamentMatch> matches
    ) {

        return matches.stream()
                .filter(match ->
                        match.getBlackParticipant()
                                == null
                )
                .findFirst()
                .orElseThrow();
    }

    private TournamentMatch findPair(
            List<TournamentMatch> matches,
            int firstSeed,
            int secondSeed
    ) {

        return matches.stream()
                .filter(match ->
                        containsPair(
                                match,
                                firstSeed,
                                secondSeed
                        )
                )
                .findFirst()
                .orElseThrow();
    }

    private void assertPairOnBoard(
            List<TournamentMatch> matches,
            int board,
            int firstSeed,
            int secondSeed
    ) {

        assertContainsPair(
                getBoard(matches, board),
                firstSeed,
                secondSeed
        );
    }

    private void assertContainsPair(
            List<TournamentMatch> matches,
            int firstSeed,
            int secondSeed
    ) {

        assertTrue(
                matches.stream()
                        .anyMatch(match ->
                                containsPair(
                                        match,
                                        firstSeed,
                                        secondSeed
                                )
                        ),
                "Expected pair "
                        + firstSeed
                        + "-"
                        + secondSeed
        );
    }

    private void assertContainsPair(
            TournamentMatch match,
            int firstSeed,
            int secondSeed
    ) {

        assertTrue(
                containsPair(
                        match,
                        firstSeed,
                        secondSeed
                ),
                "Expected pair "
                        + firstSeed
                        + "-"
                        + secondSeed
        );
    }

    private boolean containsPair(
            TournamentMatch match,
            int firstSeed,
            int secondSeed
    ) {

        if (match.getWhiteParticipant() == null
                || match.getBlackParticipant() == null) {

            return false;
        }

        int white =
                match.getWhiteParticipant()
                        .getSeed();

        int black =
                match.getBlackParticipant()
                        .getSeed();

        return (white == firstSeed
                && black == secondSeed)
                ||
                (white == secondSeed
                        && black == firstSeed);
    }

    private boolean samePair(
            TournamentMatch first,
            TournamentMatch second
    ) {

        if (first.getBlackParticipant() == null
                || second.getBlackParticipant() == null) {

            return false;
        }

        return containsPair(
                second,
                first.getWhiteParticipant()
                        .getSeed(),
                first.getBlackParticipant()
                        .getSeed()
        );
    }

    private String pairKey(
            TournamentMatch match
    ) {

        int first =
                match.getWhiteParticipant()
                        .getSeed();

        int second =
                match.getBlackParticipant()
                        .getSeed();

        return Math.min(first, second)
                + "-"
                + Math.max(first, second);
    }

    private void completeWithWinner(
            TournamentMatch match,
            int winnerSeed
    ) {

        assertNotNull(
                match.getBlackParticipant()
        );

        int whiteSeed =
                match.getWhiteParticipant()
                        .getSeed();

        int blackSeed =
                match.getBlackParticipant()
                        .getSeed();

        assertTrue(
                winnerSeed == whiteSeed
                        || winnerSeed == blackSeed
        );

        match.setStatus(
                TournamentMatchStatus.COMPLETED
        );

        if (winnerSeed == whiteSeed) {

            match.setWhiteScore(1.0);
            match.setBlackScore(0.0);

        } else {

            match.setWhiteScore(0.0);
            match.setBlackScore(1.0);
        }
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