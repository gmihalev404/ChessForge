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

import static org.junit.jupiter.api.Assertions.*;

class TournamentPairingServiceTest {

    private TournamentPairingService pairingService;

    private Tournament tournament;

    @BeforeEach
    void setUp() {

        pairingService =
                new TournamentPairingService();

        tournament = Tournament.builder()
                .name("Test tournament")
                .format(TournamentFormat.SINGLE_ELIMINATION)
                .byePoints(1.0)
                .status(TournamentStatus.IN_PROGRESS)
                .build();
    }

    // =========================================================
    // FIRST ROUND
    // =========================================================

    @Test
    void createInitialPairingsShouldCreateFourMatchesForEightPlayers() {

        List<TournamentParticipant> participants =
                createParticipants(8);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(4, matches.size());

        assertTrue(
                matches.stream()
                        .noneMatch(match ->
                                match.getBlackParticipant() == null
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
    void createInitialPairingsShouldGiveTwoByesForSixPlayers() {

        List<TournamentParticipant> participants =
                createParticipants(6);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(4, matches.size());

        List<TournamentMatch> byeMatches =
                matches.stream()
                        .filter(match ->
                                match.getTermination()
                                        == TournamentMatchTermination.BYE
                        )
                        .toList();

        assertEquals(2, byeMatches.size());

        assertEquals(
                1,
                byeMatches.get(0)
                        .getWhiteParticipant()
                        .getSeed()
        );

        assertEquals(
                2,
                byeMatches.get(1)
                        .getWhiteParticipant()
                        .getSeed()
        );
    }

    @Test
    void createInitialPairingsShouldGiveThreeByesForFivePlayers() {

        List<TournamentParticipant> participants =
                createParticipants(5);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(4, matches.size());

        long byeCount =
                matches.stream()
                        .filter(match ->
                                match.getTermination()
                                        == TournamentMatchTermination.BYE
                        )
                        .count();

        assertEquals(3, byeCount);
    }

    @Test
    void byeShouldBeCompletedAndReceiveConfiguredPoints() {

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
                                match.getTermination()
                                        == TournamentMatchTermination.BYE
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                TournamentMatchStatus.COMPLETED,
                bye.getStatus()
        );

        assertEquals(
                TournamentMatchTermination.BYE,
                bye.getTermination()
        );

        assertEquals(
                0.5,
                bye.getWhiteScore()
        );

        assertNull(bye.getBlackScore());
        assertNull(bye.getBlackParticipant());

        assertEquals(
                1,
                bye.getWhiteParticipant().getSeed()
        );
    }

    @Test
    void firstRoundShouldPairHigherSeedsAgainstLowerSeeds() {

        List<TournamentParticipant> participants =
                createParticipants(8);

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertContainsPair(matches, 1, 8);
        assertContainsPair(matches, 2, 7);
        assertContainsPair(matches, 3, 6);
        assertContainsPair(matches, 4, 5);
    }

    @Test
    void createInitialPairingsShouldIgnoreInactiveParticipants() {

        List<TournamentParticipant> participants =
                new ArrayList<>(createParticipants(4));

        participants.get(3).setStatus(
                TournamentParticipantStatus.FORFEITED
        );

        /*
         * ACTIVE:
         * seeds 1, 2, 3
         *
         * bracket size = 4
         * one BYE + one normal match
         */
        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        assertEquals(2, matches.size());

        boolean forfeitedIncluded =
                matches.stream()
                        .anyMatch(match ->
                                containsParticipant(
                                        match,
                                        participants.get(3)
                                )
                        );

        assertFalse(forfeitedIncluded);
    }

    @Test
    void createInitialPairingsShouldIgnoreAllNonActiveStatuses() {

        List<TournamentParticipant> participants =
                new ArrayList<>(createParticipants(5));

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

        // only seeds 1 and 2 remain ACTIVE
        assertEquals(1, matches.size());

        assertContainsPair(matches, 1, 2);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void createInitialPairingsShouldThrowWhenParticipantHasNoSeed() {

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
    void createInitialPairingsShouldThrowWithLessThanTwoActiveParticipants() {

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
    // LATER ROUND
    // =========================================================

    @Test
    void createNextRoundShouldPairRemainingActiveParticipants() {

        List<TournamentParticipant> participants =
                createParticipants(6);

        participants.get(2).setStatus(
                TournamentParticipantStatus.ELIMINATED
        );

        participants.get(4).setStatus(
                TournamentParticipantStatus.ELIMINATED
        );

        /*
         * remaining seeds:
         * 1, 2, 4, 6
         *
         * reseeded pairing:
         * 1 vs 6
         * 2 vs 4
         */
        List<TournamentMatch> matches =
                pairingService.createNextRound(
                        tournament,
                        participants,
                        2
                );

        assertEquals(2, matches.size());

        assertContainsPair(matches, 1, 6);
        assertContainsPair(matches, 2, 4);

        assertTrue(
                matches.stream()
                        .allMatch(match ->
                                match.getRoundNumber() == 2
                        )
        );
    }

    @Test
    void createNextRoundShouldThrowForOddNumberOfActiveParticipants() {

        List<TournamentParticipant> participants =
                createParticipants(3);

        assertThrows(
                IllegalStateException.class,
                () ->
                        pairingService.createNextRound(
                                tournament,
                                participants,
                                2
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

            User user = User.builder()
                    .username("player" + i)
                    .email("player" + i + "@test.com")
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

    private void assertContainsPair(
            List<TournamentMatch> matches,
            int seed1,
            int seed2
    ) {

        boolean found =
                matches.stream()
                        .anyMatch(match -> {

                            if (match.getBlackParticipant() == null) {
                                return false;
                            }

                            int whiteSeed =
                                    match.getWhiteParticipant()
                                            .getSeed();

                            int blackSeed =
                                    match.getBlackParticipant()
                                            .getSeed();

                            return (whiteSeed == seed1
                                    && blackSeed == seed2)
                                    ||
                                    (whiteSeed == seed2
                                            && blackSeed == seed1);
                        });

        assertTrue(
                found,
                "Expected pairing "
                        + seed1
                        + " vs "
                        + seed2
        );
    }

    private boolean containsParticipant(
            TournamentMatch match,
            TournamentParticipant participant
    ) {

        return match.getWhiteParticipant() == participant
                || match.getBlackParticipant() == participant;
    }
}