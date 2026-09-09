package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.*;
import com.example.chessforge.repository.TournamentMatchRepository;
import com.example.chessforge.repository.TournamentParticipantRepository;
import com.example.chessforge.repository.TournamentRepository;
import com.example.chessforge.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentMatchRepository matchRepository;

    private final TournamentPairingService pairingService;
    private final TournamentLeaderboardService leaderboardService;
    private final RatingService ratingService;

    // =========================================================
    // CREATE TOURNAMENT
    // =========================================================

    @Transactional
    public Tournament createTournament(
            String name,
            User creator,
            TournamentFormat format,
            TimeControl timeControl,
            boolean rated,
            Integer maxPlayers,
            LocalDateTime startsAt,
            double byePoints,
            List<TieBreakType> tieBreaks,
            boolean armageddonForFirstPlaceTie
    ) {

        if (creator.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active users can create tournaments."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tournament name cannot be empty."
            );
        }

        if (format == null) {
            throw new IllegalArgumentException(
                    "Tournament format cannot be null."
            );
        }

        if (timeControl == null) {
            throw new IllegalArgumentException(
                    "Time control cannot be null."
            );
        }

        if (maxPlayers != null && maxPlayers < 2) {
            throw new IllegalArgumentException(
                    "A tournament must allow at least two players."
            );
        }

        if (byePoints < 0.0 || byePoints > 1.0) {
            throw new IllegalArgumentException(
                    "Bye points must be between 0 and 1."
            );
        }

        Tournament tournament = Tournament.builder()
                .name(name)
                .creator(creator)
                .format(format)
                .timeControl(timeControl)
                .rated(rated)
                .maxPlayers(maxPlayers)
                .startsAt(startsAt)
                .byePoints(byePoints)
                .tieBreaks(
                        tieBreaks == null
                                ? new ArrayList<>()
                                : new ArrayList<>(tieBreaks)
                )
                .armageddonForFirstPlaceTie(
                        armageddonForFirstPlaceTie
                )
                .status(TournamentStatus.REGISTRATION)
                .build();

        return tournamentRepository.save(tournament);
    }

    // =========================================================
    // JOIN TOURNAMENT
    // =========================================================

    @Transactional
    public TournamentParticipant joinTournament(
            Tournament tournament,
            User user
    ) {

        if (tournament.getStatus()
                != TournamentStatus.REGISTRATION) {

            throw new IllegalStateException(
                    "Tournament registration is closed."
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active users can join tournaments."
            );
        }

        if (participantRepository
                .existsByTournamentAndUser(
                        tournament,
                        user
                )) {

            throw new IllegalStateException(
                    "User is already registered for this tournament."
            );
        }

        if (isFull(tournament)) {
            throw new IllegalStateException(
                    "Tournament is full."
            );
        }

        TournamentParticipant participant =
                TournamentParticipant.builder()
                        .tournament(tournament)
                        .user(user)
                        .score(0.0)
                        .status(
                                TournamentParticipantStatus.ACTIVE
                        )
                        .build();

        return participantRepository.save(participant);
    }

    // =========================================================
    // WITHDRAW BEFORE START
    // =========================================================

    @Transactional
    public void withdraw(
            Tournament tournament,
            User user
    ) {

        if (tournament.getStatus()
                != TournamentStatus.REGISTRATION) {

            throw new IllegalStateException(
                    "Players can only withdraw during registration."
            );
        }

        TournamentParticipant participant =
                getParticipant(tournament, user);

        if (participant.getStatus()
                != TournamentParticipantStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Participant is not active."
            );
        }

        participant.setStatus(
                TournamentParticipantStatus.WITHDRAWN
        );
    }

    // =========================================================
    // FORFEIT AFTER START
    // =========================================================

    @Transactional
    public void forfeit(
            Tournament tournament,
            User user
    ) {

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "A participant can only forfeit an active tournament."
            );
        }

        TournamentParticipant participant =
                getParticipant(tournament, user);

        if (participant.getStatus()
                != TournamentParticipantStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Participant is not active."
            );
        }

        participant.setStatus(
                TournamentParticipantStatus.FORFEITED
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Transactional
    public void cancelTournament(
            Tournament tournament,
            User requester
    ) {

        validateCreator(tournament, requester);

        if (tournament.getStatus()
                == TournamentStatus.FINISHED) {

            throw new IllegalStateException(
                    "A finished tournament cannot be cancelled."
            );
        }

        if (tournament.getStatus()
                == TournamentStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Tournament is already cancelled."
            );
        }

        tournament.setStatus(
                TournamentStatus.CANCELLED
        );
    }

    // =========================================================
    // START
    // =========================================================

    @Transactional
    public List<TournamentMatch> startTournament(
            Tournament tournament,
            User requester
    ) {

        validateCreator(tournament, requester);

        if (tournament.getStatus()
                != TournamentStatus.REGISTRATION) {

            throw new IllegalStateException(
                    "Only a tournament in registration can be started."
            );
        }

        List<TournamentParticipant> participants =
                getActiveParticipants(tournament);

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required."
            );
        }

        assignSeeds(
                tournament,
                participants
        );

        List<TournamentMatch> matches =
                pairingService.createInitialPairings(
                        tournament,
                        participants
                );

        matchRepository.saveAll(matches);

        tournament.setStatus(
                TournamentStatus.IN_PROGRESS
        );

        return matches;
    }

    // =========================================================
    // READ
    // =========================================================

    public List<TournamentParticipant> getParticipants(
            Tournament tournament
    ) {

        return participantRepository
                .findByTournament(tournament);
    }

    public List<TournamentParticipant> getStandings(
            Tournament tournament
    ) {

        List<TournamentParticipant> participants =
                participantRepository
                        .findByTournament(tournament);

        List<TournamentMatch> matches =
                matchRepository
                        .findByTournament(tournament);

        return leaderboardService.calculateStandings(
                tournament,
                participants,
                matches
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void assignSeeds(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        TimeControlType type =
                tournament.getTimeControl().getType();

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        sorted.sort(
                Comparator
                        .comparingInt(
                                (TournamentParticipant p) ->
                                        ratingService.getRating(
                                                p.getUser(),
                                                type
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                p ->
                                        p.getUser()
                                                .getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                p ->
                                        p.getUser()
                                                .getId()
                        )
        );

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setSeed(i + 1);
        }
    }

    private boolean isFull(
            Tournament tournament
    ) {

        if (tournament.getMaxPlayers() == null) {
            return false;
        }

        long activeParticipants =
                participantRepository
                        .countByTournamentAndStatus(
                                tournament,
                                TournamentParticipantStatus.ACTIVE
                        );

        return activeParticipants
                >= tournament.getMaxPlayers();
    }

    private List<TournamentParticipant>
    getActiveParticipants(
            Tournament tournament
    ) {

        return participantRepository
                .findByTournament(tournament)
                .stream()
                .filter(participant ->
                        participant.getStatus()
                                == TournamentParticipantStatus.ACTIVE
                )
                .toList();
    }

    private TournamentParticipant getParticipant(
            Tournament tournament,
            User user
    ) {

        return participantRepository
                .findByTournamentAndUser(
                        tournament,
                        user
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User is not registered for this tournament."
                        )
                );
    }

    private void validateCreator(
            Tournament tournament,
            User requester
    ) {

        if (!tournament.getCreator()
                .getId()
                .equals(requester.getId())) {

            throw new IllegalStateException(
                    "Only the tournament creator can perform this action."
            );
        }
    }
}