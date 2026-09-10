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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @InjectMocks
    private TournamentService tournamentService;

    private User creator;
    private User player;

    @BeforeEach
    void setUp() {

        creator =
                createUser(
                        1L,
                        "creator"
                );

        player =
                createUser(
                        2L,
                        "player"
                );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void createTournamentShouldCreateSwissTournament() {

        when(
                tournamentRepository.save(
                        any(Tournament.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Tournament tournament =
                tournamentService.createTournament(
                        "Swiss Cup",
                        creator,
                        TournamentFormat.SWISS,
                        TimeControl.values()[0],
                        true,
                        16,
                        LocalDateTime.now()
                                .plusDays(1),
                        1.0,
                        List.of(
                                TieBreakType.BUCHHOLZ,
                                TieBreakType.SONNEBORN_BERGER
                        ),
                        false,
                        5
                );

        assertEquals(
                "Swiss Cup",
                tournament.getName()
        );

        assertEquals(
                TournamentFormat.SWISS,
                tournament.getFormat()
        );

        assertEquals(
                TournamentStatus.REGISTRATION,
                tournament.getStatus()
        );

        assertEquals(
                5,
                tournament.getNumberOfRounds()
        );

        assertTrue(
                tournament.isRated()
        );

        verify(
                tournamentRepository
        ).save(tournament);
    }

    @Test
    void createSwissTournamentShouldRejectMissingRoundCount() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tournamentService.createTournament(
                                "Swiss",
                                creator,
                                TournamentFormat.SWISS,
                                TimeControl.values()[0],
                                false,
                                16,
                                LocalDateTime.now(),
                                1.0,
                                List.of(),
                                false,
                                null
                        )
        );

        verifyNoInteractions(
                tournamentRepository
        );
    }

    @Test
    void createSwissTournamentShouldRejectZeroRounds() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tournamentService.createTournament(
                                "Swiss",
                                creator,
                                TournamentFormat.SWISS,
                                TimeControl.values()[0],
                                false,
                                16,
                                LocalDateTime.now(),
                                1.0,
                                List.of(),
                                false,
                                0
                        )
        );
    }

    @Test
    void createTournamentShouldRejectInactiveCreator() {

        creator.setStatus(
                UserStatus.DISABLED
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.createTournament(
                                "Test",
                                creator,
                                TournamentFormat.ROUND_ROBIN,
                                TimeControl.values()[0],
                                false,
                                10,
                                LocalDateTime.now(),
                                1.0,
                                List.of(),
                                false,
                                null
                        )
        );
    }

    @Test
    void createTournamentShouldRejectBlankName() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tournamentService.createTournament(
                                "   ",
                                creator,
                                TournamentFormat.ROUND_ROBIN,
                                TimeControl.values()[0],
                                false,
                                10,
                                LocalDateTime.now(),
                                1.0,
                                List.of(),
                                false,
                                null
                        )
        );
    }

    @Test
    void createTournamentShouldRejectMaxPlayersBelowTwo() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tournamentService.createTournament(
                                "Test",
                                creator,
                                TournamentFormat.ROUND_ROBIN,
                                TimeControl.values()[0],
                                false,
                                1,
                                LocalDateTime.now(),
                                1.0,
                                List.of(),
                                false,
                                null
                        )
        );
    }

    // =========================================================
    // JOIN
    // =========================================================

    @Test
    void joinTournamentShouldRegisterPlayer() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
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

        when(
                participantRepository.save(
                        any(TournamentParticipant.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        TournamentParticipant participant =
                tournamentService.joinTournament(
                        tournament,
                        player
                );

        assertEquals(
                player,
                participant.getUser()
        );

        assertEquals(
                tournament,
                participant.getTournament()
        );

        assertEquals(
                TournamentParticipantStatus.ACTIVE,
                participant.getStatus()
        );

        assertEquals(
                0.0,
                participant.getScore()
        );
    }

    @Test
    void joinTournamentShouldRejectClosedRegistration() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
                        TournamentFormat.ROUND_ROBIN
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.joinTournament(
                                tournament,
                                player
                        )
        );
    }

    @Test
    void joinTournamentShouldRejectDuplicateRegistration() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
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
                        tournamentService.joinTournament(
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
    void joinTournamentShouldRejectFullTournament() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
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
                        tournamentService.joinTournament(
                                tournament,
                                player
                        )
        );
    }

    // =========================================================
    // WITHDRAW
    // =========================================================

    @Test
    void withdrawShouldMarkParticipantWithdrawn() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
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

    // =========================================================
    // START
    // =========================================================

    @Test
    void startTournamentShouldAssignSeedsAndSetCurrentRound() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.SINGLE_ELIMINATION
                );

        User third =
                createUser(
                        3L,
                        "alpha"
                );

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

        TournamentParticipant pThird =
                createParticipant(
                        tournament,
                        third
                );

        List<TournamentParticipant> participants =
                List.of(
                        pPlayer,
                        pThird,
                        pCreator
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        TimeControlType type =
                tournament.getTimeControl()
                        .getType();

        when(
                ratingService.getRating(
                        creator,
                        type
                )
        ).thenReturn(800);

        when(
                ratingService.getRating(
                        player,
                        type
                )
        ).thenReturn(600);

        when(
                ratingService.getRating(
                        third,
                        type
                )
        ).thenReturn(600);

        List<TournamentMatch> generated =
                List.of(
                        TournamentMatch.builder()
                                .tournament(tournament)
                                .whiteParticipant(pCreator)
                                .blackParticipant(pThird)
                                .roundNumber(1)
                                .boardNumber(1)
                                .status(
                                        TournamentMatchStatus.PENDING
                                )
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
                tournament.getCurrentRound()
        );

        assertEquals(
                1,
                pCreator.getSeed()
        );

        /*
         * Same rating:
         * "alpha" before "player".
         */
        assertEquals(
                2,
                pThird.getSeed()
        );

        assertEquals(
                3,
                pPlayer.getSeed()
        );

        assertEquals(
                800,
                pCreator.getRatingAtStart()
        );

        assertEquals(
                600,
                pThird.getRatingAtStart()
        );

        assertEquals(
                600,
                pPlayer.getRatingAtStart()
        );

        assertEquals(
                generated,
                result
        );

        verify(
                matchRepository
        ).saveAll(generated);
    }

    @Test
    void startTournamentShouldSnapshotParticipantRatings() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
                );

        TournamentParticipant first =
                createParticipant(
                        tournament,
                        creator
                );

        TournamentParticipant second =
                createParticipant(
                        tournament,
                        player
                );

        List<TournamentParticipant> participants =
                List.of(
                        first,
                        second
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        TimeControlType type =
                tournament.getTimeControl()
                        .getType();

        when(
                ratingService.getRating(
                        creator,
                        type
                )
        ).thenReturn(1450);

        when(
                ratingService.getRating(
                        player,
                        type
                )
        ).thenReturn(1720);

        when(
                pairingService.createInitialPairings(
                        eq(tournament),
                        anyList()
                )
        ).thenReturn(
                List.of()
        );

        tournamentService.startTournament(
                tournament,
                creator
        );

        assertEquals(
                1450,
                first.getRatingAtStart()
        );

        assertEquals(
                1720,
                second.getRatingAtStart()
        );

        /*
         * Higher rating receives better seed.
         */
        assertEquals(
                2,
                first.getSeed()
        );

        assertEquals(
                1,
                second.getSeed()
        );

        verify(
                ratingService,
                times(1)
        ).getRating(
                creator,
                type
        );

        verify(
                ratingService,
                times(1)
        ).getRating(
                player,
                type
        );
    }

    @Test
    void startSwissTournamentShouldRejectRoundsEqualToPlayerCount() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.SWISS
                );

        tournament.setNumberOfRounds(4);

        List<TournamentParticipant> participants =
                createParticipants(
                        tournament,
                        4
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

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
                pairingService
        );
    }

    @Test
    void startSwissTournamentShouldRejectMoreRoundsThanPlayers() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.SWISS
                );

        tournament.setNumberOfRounds(6);

        List<TournamentParticipant> participants =
                createParticipants(
                        tournament,
                        5
                );

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.startTournament(
                                tournament,
                                creator
                        )
        );
    }

    @Test
    void startTournamentShouldResolveOnlyFirstRoundAutomaticResults() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
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

        User third =
                createUser(
                        3L,
                        "third"
                );

        TournamentParticipant p3 =
                createParticipant(
                        tournament,
                        third
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

        when(
                ratingService.getRating(
                        any(User.class),
                        any(TimeControlType.class)
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
                pairingService
                        .createInitialPairings(
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

        /*
         * Future BYE still unresolved.
         */
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

    // =========================================================
    // NEXT ROUND - ROUND ROBIN
    // =========================================================

    @Test
    void startNextRoundShouldUseExistingRoundRobinSchedule() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
                        TournamentFormat.ROUND_ROBIN
                );

        tournament.setCurrentRound(1);

        TournamentParticipant participant =
                createParticipant(
                        tournament,
                        player
                );

        TournamentMatch round1 =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(participant)
                        .blackParticipant(null)
                        .roundNumber(1)
                        .status(
                                TournamentMatchStatus.COMPLETED
                        )
                        .whiteScore(1.0)
                        .build();

        TournamentMatch round2 =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(participant)
                        .blackParticipant(null)
                        .roundNumber(2)
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
                List.of(round1)
        );

        when(
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                2
                        )
        ).thenReturn(
                List.of(round2)
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
                List.of(round2),
                result
        );

        assertEquals(
                TournamentMatchStatus.COMPLETED,
                round2.getStatus()
        );

        assertEquals(
                TournamentMatchTermination.BYE,
                round2.getTermination()
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
        ).saveAll(
                eq(List.of(round2))
        );
    }

    // =========================================================
    // NEXT ROUND - SWISS
    // =========================================================

    @Test
    void startNextRoundShouldGenerateSwissRoundUsingFullHistory() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
                        TournamentFormat.SWISS
                );

        tournament.setCurrentRound(1);
        tournament.setNumberOfRounds(4);

        List<TournamentParticipant> participants =
                createParticipants(
                        tournament,
                        4
                );

        TournamentMatch previous1 =
                completedMatch(
                        tournament,
                        participants.get(0),
                        participants.get(2),
                        1
                );

        TournamentMatch previous2 =
                completedMatch(
                        tournament,
                        participants.get(1),
                        participants.get(3),
                        1
                );

        List<TournamentMatch> history =
                List.of(
                        previous1,
                        previous2
                );

        when(
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                1
                        )
        ).thenReturn(history);

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        when(
                matchRepository
                        .findByTournament(tournament)
        ).thenReturn(history);

        TournamentMatch generated =
                TournamentMatch.builder()
                        .tournament(tournament)
                        .whiteParticipant(
                                participants.get(0)
                        )
                        .blackParticipant(
                                participants.get(3)
                        )
                        .roundNumber(2)
                        .boardNumber(1)
                        .status(
                                TournamentMatchStatus.PENDING
                        )
                        .build();

        List<TournamentMatch> generatedMatches =
                List.of(generated);

        when(
                pairingService.createNextRound(
                        tournament,
                        participants,
                        history,
                        2
                )
        ).thenReturn(generatedMatches);

        List<TournamentMatch> result =
                tournamentService.startNextRound(
                        tournament
                );

        assertEquals(
                2,
                tournament.getCurrentRound()
        );

        assertEquals(
                generatedMatches,
                result
        );

        verify(
                pairingService
        ).createNextRound(
                tournament,
                participants,
                history,
                2
        );

        verify(
                matchRepository
        ).saveAll(
                generatedMatches
        );
    }

    @Test
    void startNextRoundShouldRejectSwissTournamentAfterLastRound() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
                        TournamentFormat.SWISS
                );

        tournament.setNumberOfRounds(4);
        tournament.setCurrentRound(4);

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService
                                .startNextRound(
                                        tournament
                                )
        );

        assertEquals(
                4,
                tournament.getCurrentRound()
        );
    }

    // =========================================================
    // NEXT ROUND VALIDATION
    // =========================================================

    @Test
    void startNextRoundShouldRejectIncompleteCurrentRound() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
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
    }

    @Test
    void startNextRoundShouldRejectInactiveTournament() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
                );

        tournament.setCurrentRound(1);

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.startNextRound(
                                tournament
                        )
        );
    }

    // =========================================================
    // FORFEIT
    // =========================================================

    @Test
    void forfeitShouldResolveCurrentRoundMatch() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
                        TournamentFormat.SWISS
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
                Optional.of(
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
    }

    @Test
    void forfeitShouldRejectInactiveTournament() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.SWISS
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.forfeit(
                                tournament,
                                player
                        )
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancelTournamentShouldCancelTournament() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
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
    void cancelTournamentShouldRejectNonCreator() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.REGISTRATION,
                        TournamentFormat.ROUND_ROBIN
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tournamentService.cancelTournament(
                                tournament,
                                player
                        )
        );

        assertEquals(
                TournamentStatus.REGISTRATION,
                tournament.getStatus()
        );
    }

    // =========================================================
    // STANDINGS
    // =========================================================

    @Test
    void getStandingsShouldDelegateToLeaderboardService() {

        Tournament tournament =
                createTournament(
                        TournamentStatus.IN_PROGRESS,
                        TournamentFormat.SWISS
                );

        List<TournamentParticipant> participants =
                List.of(
                        createParticipant(
                                tournament,
                                creator
                        ),
                        createParticipant(
                                tournament,
                                player
                        )
                );

        List<TournamentMatch> matches =
                new ArrayList<>();

        when(
                participantRepository
                        .findByTournament(tournament)
        ).thenReturn(participants);

        when(
                matchRepository
                        .findByTournament(tournament)
        ).thenReturn(matches);

        when(
                leaderboardService
                        .calculateStandings(
                                tournament,
                                participants,
                                matches
                        )
        ).thenReturn(participants);

        List<TournamentParticipant> result =
                tournamentService.getStandings(
                        tournament
                );

        assertEquals(
                participants,
                result
        );

        verify(
                leaderboardService
        ).calculateStandings(
                tournament,
                participants,
                matches
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser(
            Long id,
            String username
    ) {

        User user =
                User.builder()
                        .username(username)
                        .email(
                                username
                                        + "@test.com"
                        )
                        .password("password")
                        .status(
                                UserStatus.ACTIVE
                        )
                        .bulletRating(400)
                        .blitzRating(400)
                        .rapidRating(400)
                        .classicalRating(400)
                        .build();

        setId(
                user,
                id
        );

        return user;
    }

    private Tournament createTournament(
            TournamentStatus status,
            TournamentFormat format
    ) {

        Tournament tournament =
                Tournament.builder()
                        .name("Test Tournament")
                        .creator(creator)
                        .status(status)
                        .format(format)
                        .timeControl(
                                TimeControl.values()[0]
                        )
                        .rated(true)
                        .maxPlayers(16)
                        .byePoints(1.0)
                        .tieBreaks(
                                new ArrayList<>()
                        )
                        .armageddonForFirstPlaceTie(false)
                        .build();

        if (format == TournamentFormat.SWISS) {
            tournament.setNumberOfRounds(3);
        }

        return tournament;
    }

    private TournamentParticipant createParticipant(
            Tournament tournament,
            User user
    ) {

        TournamentParticipant participant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(user)
                        .score(0.0)
                        .status(
                                TournamentParticipantStatus.ACTIVE
                        )
                        .build();

        return participant;
    }

    private List<TournamentParticipant> createParticipants(
            Tournament tournament,
            int count
    ) {

        List<TournamentParticipant> participants =
                new ArrayList<>();

        for (int i = 1;
             i <= count;
             i++) {

            User user =
                    createUser(
                            100L + i,
                            "player" + i
                    );

            TournamentParticipant participant =
                    createParticipant(
                            tournament,
                            user
                    );

            participants.add(
                    participant
            );
        }

        return participants;
    }

    private TournamentMatch completedMatch(
            Tournament tournament,
            TournamentParticipant white,
            TournamentParticipant black,
            int round
    ) {

        return TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(white)
                .blackParticipant(black)
                .roundNumber(round)
                .boardNumber(1)
                .status(
                        TournamentMatchStatus.COMPLETED
                )
                .whiteScore(1.0)
                .blackScore(0.0)
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