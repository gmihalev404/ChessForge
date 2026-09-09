package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        /*
         * Fixed bracket:
         *
         * Board 1: 1 vs 8
         * Board 2: 4 vs 5
         *
         * Board 3: 2 vs 7
         * Board 4: 3 vs 6
         *
         * Winners of boards 1+2 meet.
         * Winners of boards 3+4 meet.
         */
        assertPairOnBoard(matches, 1, 1, 8);
        assertPairOnBoard(matches, 2, 4, 5);
        assertPairOnBoard(matches, 3, 2, 7);
        assertPairOnBoard(matches, 4, 3, 6);

        assertTrue(
                matches.stream()
                        .allMatch(match ->
                                match.getRoundNumber() == 1
                        )
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
    void singleEliminationShouldGiveByesToTopSeedsWithSixPlayers() {

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

        TournamentMatch board2 =
                getBoard(matches, 2);

        TournamentMatch board3 =
                getBoard(matches, 3);

        TournamentMatch board4 =
                getBoard(matches, 4);

        // Seed 1 receives BYE.
        assertEquals(
                1,
                board1.getWhiteParticipant().getSeed()
        );

        assertNull(
                board1.getBlackParticipant()
        );

        // 4 vs 5
        assertContainsPair(
                board2,
                4,
                5
        );

        // Seed 2 receives BYE.
        assertEquals(
                2,
                board3.getWhiteParticipant().getSeed()
        );

        assertNull(
                board3.getBlackParticipant()
        );

        // 3 vs 6
        assertContainsPair(
                board4,
                3,
                6
        );
    }

    @Test
    void byeShouldRemainPendingUntilRoundIsProcessed() {

        tournament.setByePoints(0.5);

        List<TournamentParticipant> participants =
                createParticipants(3);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        TournamentMatch bye =
                matches.stream()
                        .filter(match ->
                                match.getBlackParticipant() == null
                        )
                        .findFirst()
                        .orElseThrow();

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
                new ArrayList<>(
                        createParticipants(5)
                );

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

        // Only seeds 1 and 2 remain active.
        assertEquals(1, matches.size());

        assertContainsPair(
                matches.get(0),
                1,
                2
        );
    }

    @Test
    void singleEliminationShouldThrowWhenParticipantHasNoSeed() {

        List<TournamentParticipant> participants =
                createParticipants(4);

        participants.get(2).setSeed(null);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                pairingService.createInitialPairings(
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
    void singleEliminationShouldRequireAtLeastTwoActiveParticipants() {

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
    // SINGLE ELIMINATION - FIXED BRACKET NEXT ROUND
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
         * Round 1:
         *
         * board 1: 1 vs 8 -> 8 wins
         * board 2: 4 vs 5 -> 4 wins
         * board 3: 2 vs 7 -> 7 wins
         * board 4: 3 vs 6 -> 3 wins
         *
         * Fixed bracket MUST produce:
         *
         * 8 vs 4
         * 7 vs 3
         *
         * Re-seeding would instead produce:
         * 3 vs 8
         * 4 vs 7
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

        List<TournamentMatch> secondRound =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        firstRound,
                        2
                );

        assertEquals(2, secondRound.size());

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

        // Board 2 stays PENDING.

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
    void nextSingleEliminationRoundShouldRejectDrawnMatch() {

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

        IllegalStateException exception =
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

        assertEquals(
                "A knockout match must have a winner before the next round can be created.",
                exception.getMessage()
        );
    }

    // =========================================================
    // ROUND ROBIN
    // =========================================================

    @Test
    void roundRobinWithFourPlayersShouldCreateEntireSchedule() {

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

        /*
         * 4 players:
         *
         * rounds = 4 - 1 = 3
         * games per round = 2
         * total = 6
         */
        assertEquals(6, matches.size());

        for (int round = 1; round <= 3; round++) {

            int currentRound = round;

            long matchesInRound =
                    matches.stream()
                            .filter(match ->
                                    match.getRoundNumber()
                                            == currentRound
                            )
                            .count();

            assertEquals(
                    2,
                    matchesInRound
            );
        }
    }

    @Test
    void roundRobinShouldMakeEveryPairPlayExactlyOnce() {

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

        Map<String, Long> pairCounts =
                matches.stream()
                        .filter(match ->
                                match.getBlackParticipant() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        this::pairKey,
                                        Collectors.counting()
                                )
                        );

        /*
         * C(4,2) = 6 unique pairs.
         */
        assertEquals(
                6,
                pairCounts.size()
        );

        assertTrue(
                pairCounts.values()
                        .stream()
                        .allMatch(count ->
                                count == 1
                        )
        );
    }

    @Test
    void roundRobinWithFivePlayersShouldCreateOneByePerPlayer() {

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

        /*
         * Virtual sixth slot:
         *
         * rounds = 5
         * slots per round = 3
         *
         * 10 real games + 5 BYEs = 15 TournamentMatches.
         */
        assertEquals(
                15,
                matches.size()
        );

        List<TournamentMatch> byes =
                matches.stream()
                        .filter(match ->
                                match.getBlackParticipant() == null
                        )
                        .toList();

        assertEquals(
                5,
                byes.size()
        );

        for (TournamentParticipant participant
                : participants) {

            long byeCount =
                    byes.stream()
                            .filter(match ->
                                    match.getWhiteParticipant()
                                            == participant
                            )
                            .count();

            assertEquals(
                    1,
                    byeCount,
                    "Seed "
                            + participant.getSeed()
                            + " should receive exactly one BYE."
            );
        }
    }

    @Test
    void futureRoundRobinByesShouldNotAwardPointsDuringPairing() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        tournament.setByePoints(0.5);

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
                                match.getBlackParticipant() == null
                        )
                        .toList();

        assertTrue(
                byes.stream()
                        .allMatch(match ->
                                match.getStatus()
                                        == TournamentMatchStatus.PENDING
                        )
        );

        assertTrue(
                byes.stream()
                        .allMatch(match ->
                                match.getTermination() == null
                        )
        );

        assertTrue(
                byes.stream()
                        .allMatch(match ->
                                match.getWhiteScore() == null
                        )
        );
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

            long whiteGames =
                    matches.stream()
                            .filter(match ->
                                    match.getWhiteParticipant()
                                            == participant
                            )
                            .count();

            long blackGames =
                    matches.stream()
                            .filter(match ->
                                    match.getBlackParticipant()
                                            == participant
                            )
                            .count();

            assertEquals(
                    5,
                    whiteGames + blackGames
            );

            assertTrue(
                    Math.abs(
                            whiteGames - blackGames
                    ) <= 1,
                    "Unbalanced colors for seed "
                            + participant.getSeed()
                            + ": white="
                            + whiteGames
                            + ", black="
                            + blackGames
            );
        }
    }

    @Test
    void roundRobinShouldRejectCreatingAnotherRound() {

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        assertThrows(
                IllegalStateException.class,
                () ->
                        pairingService.createNextRound(
                                tournament,
                                participants,
                                List.of(),
                                2
                        )
        );
    }

    // =========================================================
    // SWISS
    // =========================================================

    @Test
    void swissShouldRemainUnsupportedForNow() {

        tournament.setFormat(
                TournamentFormat.SWISS
        );

        List<TournamentParticipant> participants =
                createParticipants(4);

        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        pairingService.createInitialPairings(
                                tournament,
                                participants
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

        for (int i = 1; i <= count; i++) {

            User user =
                    User.builder()
                            .username("player" + i)
                            .email(
                                    "player"
                                            + i
                                            + "@test.com"
                            )
                            .password("password")
                            .status(UserStatus.ACTIVE)
                            .build();

            TournamentParticipant participant =
                    TournamentParticipant.builder()
                            .tournament(tournament)
                            .user(user)
                            .seed(i)
                            .score(0.0)
                            .status(
                                    TournamentParticipantStatus.ACTIVE
                            )
                            .build();

            participants.add(participant);
        }

        return participants;
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

    private void assertPairOnBoard(
            List<TournamentMatch> matches,
            int boardNumber,
            int firstSeed,
            int secondSeed
    ) {

        TournamentMatch match =
                getBoard(
                        matches,
                        boardNumber
                );

        assertContainsPair(
                match,
                firstSeed,
                secondSeed
        );
    }

    private void assertContainsPair(
            TournamentMatch match,
            int firstSeed,
            int secondSeed
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
                (whiteSeed == firstSeed
                        && blackSeed == secondSeed)
                        ||
                        (whiteSeed == secondSeed
                                && blackSeed == firstSeed),
                "Expected "
                        + firstSeed
                        + " vs "
                        + secondSeed
                        + ", but got "
                        + whiteSeed
                        + " vs "
                        + blackSeed
        );
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

    private String pairKey(
            TournamentMatch match
    ) {

        int first =
                match.getWhiteParticipant()
                        .getSeed();

        int second =
                match.getBlackParticipant()
                        .getSeed();

        int lower =
                Math.min(first, second);

        int higher =
                Math.max(first, second);

        return lower + "-" + higher;
    }
}