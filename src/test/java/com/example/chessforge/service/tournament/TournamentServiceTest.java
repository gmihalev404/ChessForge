package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.BaseEntity;
import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.*;
import com.example.chessforge.repository.TournamentMatchRepository;
import com.example.chessforge.repository.TournamentParticipantRepository;
import com.example.chessforge.repository.TournamentRepository;
import com.example.chessforge.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentParticipantRepository participantRepository;

    @Mock
    private TournamentMatchRepository matchRepository;

    @Mock
    private TournamentPairingService pairingService;

    @Mock
    private TournamentLeaderboardService leaderboardService;

    @Mock
    private RatingService ratingService;

    private TournamentService tournamentService;

    private User creator;
    private User player;

    @BeforeEach
    void setUp() {

        tournamentService = new TournamentService(
                tournamentRepository,
                participantRepository,
                matchRepository,
                pairingService,
                leaderboardService,
                ratingService
        );

        creator = createUser(
                1L,
                "creator"
        );

        player = createUser(
                2L,
                "player"
        );
    }

    // =========================================================
    // CREATE TOURNAMENT
    // =========================================================

    @Test
    void createTournamentShouldCreateTournament() {

        when(tournamentRepository.save(
                any(Tournament.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Tournament tournament =
                tournamentService.createTournament(
                        "Chess Cup",
                        creator,
                        TournamentFormat.SINGLE_ELIMINATION,
                        TimeControl.values()[0],
                        true,
                        16,
                        LocalDateTime.now().plusDays(1),
                        1.0,
                        List.of(
                                TieBreakType.BUCHHOLZ,
                                TieBreakType.SONNEBORN_BERGER
                        ),
                        false
                );

        assertEquals(
                "Chess Cup",
                tournament.getName()
        );

        assertEquals(
                creator,
                tournament.getCreator()
        );

        assertEquals(
                TournamentStatus.REGISTRATION,
                tournament.getStatus()
        );

        assertEquals(
                TournamentFormat.SINGLE_ELIMINATION,
                tournament.getFormat()
        );

        assertTrue(tournament.isRated());

        assertEquals(
                16,
                tournament.getMaxPlayers()
        );

        assertEquals(
                1.0,
                tournament.getByePoints()
        );

        assertEquals(
                2,
                tournament.getTieBreaks().size()
        );

        verify(tournamentRepository)
                .save(tournament);
    }

    @Test
    void createTournamentShouldThrowWhenCreatorIsInactive() {

        creator.setStatus(
                UserStatus.DISABLED
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService
                                .createTournament(
                                        "Test",
                                        creator,
                                        TournamentFormat.SWISS,
                                        TimeControl.values()[0],
                                        false,
                                        10,
                                        LocalDateTime.now(),
                                        1.0,
                                        List.of(),
                                        false
                                )
        );

        verifyNoInteractions(
                tournamentRepository
        );
    }

    @Test
    void createTournamentShouldThrowWhenNameIsEmpty() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tournamentService
                                .createTournament(
                                        "   ",
                                        creator,
                                        TournamentFormat.SWISS,
                                        TimeControl.values()[0],
                                        false,
                                        10,
                                        LocalDateTime.now(),
                                        1.0,
                                        List.of(),
                                        false
                                )
        );

        verifyNoInteractions(
                tournamentRepository
        );
    }

    @Test
    void createTournamentShouldThrowWhenMaxPlayersIsLessThanTwo() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tournamentService
                                .createTournament(
                                        "Test",
                                        creator,
                                        TournamentFormat.SWISS,
                                        TimeControl.values()[0],
                                        false,
                                        1,
                                        LocalDateTime.now(),
                                        1.0,
                                        List.of(),
                                        false
                                )
        );

        verifyNoInteractions(
                tournamentRepository
        );
    }

    // =========================================================
    // JOIN
    // =========================================================

    @Test
    void joinTournamentShouldRegisterPlayer() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        when(
                participantRepository
                        .existsByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(false);

        when(
                participantRepository
                        .countByTournamentAndStatus(
                                tournament,
                                TournamentParticipantStatus.ACTIVE
                        )
        ).thenReturn(1L);

        when(participantRepository.save(
                any(TournamentParticipant.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        TournamentParticipant result =
                tournamentService.joinTournament(
                        tournament,
                        player
                );

        assertEquals(
                tournament,
                result.getTournament()
        );

        assertEquals(
                player,
                result.getUser()
        );

        assertEquals(
                TournamentParticipantStatus.ACTIVE,
                result.getStatus()
        );

        assertEquals(
                0.0,
                result.getScore()
        );
    }

    @Test
    void joinTournamentShouldThrowWhenRegistrationIsClosed() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService
                                .joinTournament(
                                        tournament,
                                        player
                                )
        );

        verifyNoInteractions(
                participantRepository
        );
    }

    @Test
    void joinTournamentShouldThrowWhenAlreadyRegistered() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        when(
                participantRepository
                        .existsByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService
                                .joinTournament(
                                        tournament,
                                        player
                                )
        );

        verify(
                participantRepository,
                never()
        ).save(any());
    }

    @Test
    void joinTournamentShouldThrowWhenTournamentIsFull() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        tournament.setMaxPlayers(2);

        when(
                participantRepository
                        .existsByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(false);

        when(
                participantRepository
                        .countByTournamentAndStatus(
                                tournament,
                                TournamentParticipantStatus.ACTIVE
                        )
        ).thenReturn(2L);

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService
                                .joinTournament(
                                        tournament,
                                        player
                                )
        );

        verify(
                participantRepository,
                never()
        ).save(any());
    }

    // =========================================================
    // WITHDRAW
    // =========================================================

    @Test
    void withdrawShouldMarkParticipantWithdrawn() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        TournamentParticipant participant =
                createParticipant(
                        tournament,
                        player
                );

        when(
                participantRepository
                        .findByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(
                Optional.of(participant)
        );

        tournamentService.withdraw(
                tournament,
                player
        );

        assertEquals(
                TournamentParticipantStatus.WITHDRAWN,
                participant.getStatus()
        );
    }

    @Test
    void withdrawShouldThrowWhenUserIsNotRegistered() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        when(
                participantRepository
                        .findByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.withdraw(
                                tournament,
                                player
                        )
        );
    }

    // =========================================================
    // FORFEIT
    // =========================================================

    @Test
    void forfeitShouldMarkParticipantForfeited() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS
                );

        TournamentParticipant participant =
                createParticipant(
                        tournament,
                        player
                );

        when(
                participantRepository
                        .findByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(
                Optional.of(participant)
        );

        tournamentService.forfeit(
                tournament,
                player
        );

        assertEquals(
                TournamentParticipantStatus.FORFEITED,
                participant.getStatus()
        );
    }

    @Test
    void forfeitShouldThrowWhenTournamentIsNotInProgress() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.forfeit(
                                tournament,
                                player
                        )
        );

        verifyNoInteractions(
                participantRepository
        );
    }

    @Test
    void forfeitShouldResolveCurrentRoundMatch() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS
                );

        tournament.setCurrentRound(2);

        TournamentParticipant forfeiting =
                createParticipant(
                        tournament,
                        player
                );

        User opponentUser =
                createUser(
                        3L,
                        "opponent"
                );

        TournamentParticipant opponent =
                createParticipant(
                        tournament,
                        opponentUser
                );

        TournamentMatch match =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(forfeiting)
                        .blackParticipant(opponent)
                        .roundNumber(2)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        when(
                participantRepository
                        .findByTournamentAndUser(
                                tournament,
                                player
                        )
        ).thenReturn(
                java.util.Optional.of(
                        forfeiting
                )
        );

        when(
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                2
                        )
        ).thenReturn(
                List.of(match)
        );

        tournamentService.forfeit(
                tournament,
                player
        );

        assertEquals(
                TournamentParticipantStatus.FORFEITED,
                forfeiting.getStatus()
        );

        assertEquals(
                TournamentMatchStatus.COMPLETED,
                match.getStatus()
        );

        assertEquals(
                TournamentMatchTermination.WHITE_FORFEIT,
                match.getTermination()
        );

        assertEquals(
                0.0,
                match.getWhiteScore()
        );

        assertEquals(
                1.0,
                match.getBlackScore()
        );

        assertEquals(
                1.0,
                opponent.getScore()
        );

        assertEquals(
                0.0,
                forfeiting.getScore()
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancelTournamentShouldCancelTournament() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        tournamentService.cancelTournament(
                tournament,
                creator
        );

        assertEquals(
                TournamentStatus.CANCELLED,
                tournament.getStatus()
        );
    }

    @Test
    void cancelTournamentShouldThrowWhenRequesterIsNotCreator() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                tournamentService
                                        .cancelTournament(
                                                tournament,
                                                player
                                        )
                );

        assertEquals(
                "Only the tournament creator can perform this action.",
                exception.getMessage()
        );

        assertEquals(
                TournamentStatus.REGISTRATION,
                tournament.getStatus()
        );
    }

    @Test
    void cancelTournamentShouldThrowWhenTournamentIsFinished() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.FINISHED
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService
                                .cancelTournament(
                                        tournament,
                                        creator
                                )
        );
    }

    // =========================================================
    // START
    // =========================================================

    @Test
    void startTournamentShouldAssignSeedsCreatePairingsAndStart() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        tournament.setTimeControl(
                findTimeControl(
                        TimeControlType.BLITZ
                )
        );

        User lowerRated =
                createUser(
                        3L,
                        "alpha"
                );

        creator.setBlitzRating(800);
        player.setBlitzRating(600);
        lowerRated.setBlitzRating(600);

        TournamentParticipant pCreator =
                createParticipant(
                        tournament,
                        creator
                );

        TournamentParticipant pPlayer =
                createParticipant(
                        tournament,
                        player
                );

        TournamentParticipant pLower =
                createParticipant(
                        tournament,
                        lowerRated
                );

        List<TournamentParticipant> participants =
                List.of(
                        pPlayer,
                        pLower,
                        pCreator
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        when(
                ratingService.getRating(
                        creator,
                        TimeControlType.BLITZ
                )
        ).thenReturn(800);

        when(
                ratingService.getRating(
                        player,
                        TimeControlType.BLITZ
                )
        ).thenReturn(600);

        when(
                ratingService.getRating(
                        lowerRated,
                        TimeControlType.BLITZ
                )
        ).thenReturn(600);

        List<TournamentMatch> generated =
                List.of(
                        TournamentMatch.builder()
                                .tournament(tournament)
                                .roundNumber(1)
                                .boardNumber(1)
                                .build()
                );

        when(
                pairingService
                        .createInitialPairings(
                                eq(tournament),
                                anyList()
                        )
        ).thenReturn(generated);

        List<TournamentMatch> result =
                tournamentService.startTournament(
                        tournament,
                        creator
                );

        assertEquals(
                TournamentStatus.IN_PROGRESS,
                tournament.getStatus()
        );

        assertEquals(
                1,
                pCreator.getSeed()
        );

        /*
         * player = 600, username "player"
         * lowerRated = 600, username "alpha"
         *
         * Alphabetical tiebreak:
         * alpha comes before player.
         */
        assertEquals(
                2,
                pLower.getSeed()
        );

        assertEquals(
                3,
                pPlayer.getSeed()
        );

        assertEquals(
                generated,
                result
        );

        assertEquals(
                1,
                tournament.getCurrentRound()
        );

        verify(matchRepository)
                .saveAll(generated);

        verify(pairingService)
                .createInitialPairings(
                        tournament,
                        participants
                );
    }

    @Test
    void startTournamentShouldThrowWhenRequesterIsNotCreator() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.startTournament(
                                tournament,
                                player
                        )
        );

        verifyNoInteractions(
                pairingService,
                matchRepository
        );
    }

    @Test
    void startTournamentShouldThrowWhenLessThanTwoActiveParticipants() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        TournamentParticipant participant =
                createParticipant(
                        tournament,
                        creator
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(
                List.of(participant)
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.startTournament(
                                tournament,
                                creator
                        )
        );

        assertEquals(
                TournamentStatus.REGISTRATION,
                tournament.getStatus()
        );

        verifyNoInteractions(
                pairingService,
                matchRepository
        );
    }

    @Test
    void startTournamentShouldResolveAutomaticResultsOnlyForFirstRound() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        tournament.setByePoints(0.5);

        TournamentParticipant p1 =
                createParticipant(
                        tournament,
                        creator
                );

        TournamentParticipant p2 =
                createParticipant(
                        tournament,
                        player
                );

        User thirdUser =
                createUser(
                        3L,
                        "third"
                );

        TournamentParticipant p3 =
                createParticipant(
                        tournament,
                        thirdUser
                );

        List<TournamentParticipant> participants =
                List.of(
                        p1,
                        p2,
                        p3
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        TimeControlType type =
                tournament.getTimeControl().getType();

        when(
                ratingService.getRating(
                        any(User.class),
                        eq(type)
                )
        ).thenReturn(400);

        TournamentMatch round1Bye =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(p1)
                        .blackParticipant(null)
                        .roundNumber(1)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        TournamentMatch round2Bye =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(p2)
                        .blackParticipant(null)
                        .roundNumber(2)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        List<TournamentMatch> schedule =
                List.of(
                        round1Bye,
                        round2Bye
                );

        when(
                pairingService.createInitialPairings(
                        eq(tournament),
                        anyList()
                )
        ).thenReturn(schedule);

        tournamentService.startTournament(
                tournament,
                creator
        );

        assertEquals(
                1,
                tournament.getCurrentRound()
        );

        // Round 1 BYE is now resolved.
        assertEquals(
                TournamentMatchStatus.COMPLETED,
                round1Bye.getStatus()
        );

        assertEquals(
                TournamentMatchTermination.BYE,
                round1Bye.getTermination()
        );

        assertEquals(
                0.5,
                round1Bye.getWhiteScore()
        );

        assertEquals(
                0.5,
                p1.getScore()
        );

        // Round 2 has not started yet.
        assertEquals(
                TournamentMatchStatus.PENDING,
                round2Bye.getStatus()
        );

        assertNull(
                round2Bye.getTermination()
        );

        assertNull(
                round2Bye.getWhiteScore()
        );

        assertEquals(
                0.0,
                p2.getScore()
        );
    }

    @Test
    void startNextRoundShouldUseExistingRoundRobinSchedule() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS
                );

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        tournament.setCurrentRound(1);
        tournament.setByePoints(1.0);

        TournamentParticipant participant =
                createParticipant(
                        tournament,
                        player
                );

        TournamentMatch completedRoundOne =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(participant)
                        .roundNumber(1)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.COMPLETED
                        )
                        .termination(
                                TournamentMatchTermination.BYE
                        )
                        .whiteScore(1.0)
                        .build();

        TournamentMatch roundTwoBye =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(participant)
                        .blackParticipant(null)
                        .roundNumber(2)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        when(
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                1
                        )
        ).thenReturn(
                List.of(completedRoundOne)
        );

        when(
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                2
                        )
        ).thenReturn(
                List.of(roundTwoBye)
        );

        List<TournamentMatch> result =
                tournamentService.startNextRound(
                        tournament
                );

        assertEquals(
                2,
                tournament.getCurrentRound()
        );

        assertEquals(
                List.of(roundTwoBye),
                result
        );

        assertEquals(
                TournamentMatchStatus.COMPLETED,
                roundTwoBye.getStatus()
        );

        assertEquals(
                TournamentMatchTermination.BYE,
                roundTwoBye.getTermination()
        );

        assertEquals(
                1.0,
                roundTwoBye.getWhiteScore()
        );

        verify(
                pairingService,
                never()
        ).createNextRound(
                any(),
                anyList(),
                anyList(),
                anyInt()
        );

        verify(
                matchRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void startNextRoundShouldThrowWhenCurrentRoundIsNotCompleted() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS
                );

        tournament.setFormat(
                TournamentFormat.ROUND_ROBIN
        );

        tournament.setCurrentRound(1);

        TournamentParticipant p1 =
                createParticipant(
                        tournament,
                        creator
                );

        TournamentParticipant p2 =
                createParticipant(
                        tournament,
                        player
                );

        TournamentMatch pending =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(p1)
                        .blackParticipant(p2)
                        .roundNumber(1)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        when(
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                1
                        )
        ).thenReturn(
                List.of(pending)
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.startNextRound(
                                tournament
                        )
        );

        assertEquals(
                1,
                tournament.getCurrentRound()
        );

        verify(
                pairingService,
                never()
        ).createNextRound(
                any(),
                anyList(),
                anyList(),
                anyInt()
        );
    }

    // =========================================================
    // GET PARTICIPANTS
    // =========================================================

    @Test
    void getParticipantsShouldReturnRepositoryResult() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION
                );

        List<TournamentParticipant> expected =
                List.of(
                        createParticipant(
                                tournament,
                                player
                        )
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(expected);

        List<TournamentParticipant> result =
                tournamentService
                        .getParticipants(tournament);

        assertEquals(expected, result);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser(
            Long id,
            String username
    ) {

        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password("password")
                .status(UserStatus.ACTIVE)
                .bulletRating(400)
                .blitzRating(400)
                .rapidRating(400)
                .classicalRating(400)
                .build();

        setId(user, id);

        return user;
    }

    private Tournament createTournament(
            TournamentStatus status
    ) {

        return Tournament.builder()
                .name("Test Tournament")
                .creator(creator)
                .status(status)
                .format(
                        TournamentFormat.SINGLE_ELIMINATION
                )
                .timeControl(TimeControl.values()[0])
                .rated(true)
                .maxPlayers(16)
                .byePoints(1.0)
                .tieBreaks(
                        new ArrayList<>()
                )
                .armageddonForFirstPlaceTie(false)
                .build();
    }

    private TournamentParticipant createParticipant(
            Tournament tournament,
            User user
    ) {

        return TournamentParticipant.builder()
                .tournament(tournament)
                .user(user)
                .score(0.0)
                .status(
                        TournamentParticipantStatus.ACTIVE
                )
                .build();
    }

    private TimeControl findTimeControl(
            TimeControlType type
    ) {

        return java.util.Arrays
                .stream(TimeControl.values())
                .filter(timeControl ->
                        timeControl.getType() == type
                )
                .findFirst()
                .orElseThrow();
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