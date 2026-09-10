package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.*;
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
            boolean armageddonForFirstPlaceTie,
            Integer numberOfRounds
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

        if (format == TournamentFormat.SWISS
                && (numberOfRounds == null
                || numberOfRounds < 1)) {

            throw new IllegalArgumentException(
                    "A Swiss tournament must have at least one round."
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
                .numberOfRounds(numberOfRounds)
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

        List<TournamentMatch> currentRoundMatches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                tournament.getCurrentRound()
                        );

        resolveAutomaticResults(
                tournament,
                currentRoundMatches
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

        validateTournamentCanStart(
                tournament,
                participants
        );

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

        tournament.setCurrentRound(1);

        resolveAutomaticResults(
                tournament,
                matches.stream()
                        .filter(match ->
                                match.getRoundNumber() == 1
                        )
                        .toList()
        );

        return matches;
    }

    @Transactional
    public List<TournamentMatch> startNextRound(
            Tournament tournament
    ) {

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Only an active tournament can advance to the next round."
            );
        }

        if (tournament.getCurrentRound() == null) {

            throw new IllegalStateException(
                    "Tournament has no active round."
            );
        }

        // =========================================================
        // SWISS - LAST ROUND CHECK
        // =========================================================

        if (tournament.getFormat()
                == TournamentFormat.SWISS
                && tournament.getCurrentRound()
                >= tournament.getNumberOfRounds()) {

            throw new IllegalStateException(
                    "The Swiss tournament has no more rounds."
            );
        }

        // =========================================================
        // CURRENT ROUND MUST BE FINISHED
        // =========================================================

        validateCurrentRoundCompleted(
                tournament
        );

        int nextRoundNumber =
                tournament.getCurrentRound() + 1;

        List<TournamentMatch> matches;

        // =========================================================
        // ROUND ROBIN
        // =========================================================

        if (tournament.getFormat()
                == TournamentFormat.ROUND_ROBIN) {

            /*
             * Round-robin pairings already exist.
             * We only activate the next part of the schedule.
             */
            matches =
                    matchRepository
                            .findByTournamentAndRoundNumberOrderByBoardNumber(
                                    tournament,
                                    nextRoundNumber
                            );

            if (matches.isEmpty()) {

                throw new IllegalStateException(
                        "There are no more rounds."
                );
            }
        }

        // =========================================================
        // SINGLE ELIMINATION / SWISS
        // =========================================================

        else {

            List<TournamentParticipant> participants =
                    participantRepository
                            .findByTournament(
                                    tournament
                            );

            /*
             * Swiss needs the COMPLETE history:
             * - rematches
             * - previous colors
             * - previous BYEs
             *
             * Single elimination will internally select
             * only the previous round from this history.
             */
            List<TournamentMatch> matchHistory =
                    matchRepository
                            .findByTournament(
                                    tournament
                            );

            matches =
                    pairingService.createNextRound(
                            tournament,
                            participants,
                            matchHistory,
                            nextRoundNumber
                    );

            matchRepository.saveAll(
                    matches
            );
        }

        // =========================================================
        // ACTIVATE ROUND
        // =========================================================

        tournament.setCurrentRound(
                nextRoundNumber
        );

        /*
         * Resolve only automatic results belonging
         * to the newly started round.
         *
         * Examples:
         * - BYE
         * - already FORFEITED participant
         */
        resolveAutomaticResults(
                tournament,
                matches
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

    @Transactional
    public void recordGameResult(
            Game game
    ) {

        if (game.getTournamentMatch() == null) {
            throw new IllegalArgumentException(
                    "Game does not belong to a tournament match."
            );
        }

        if (game.getStatus() != GameStatus.FINISHED) {
            throw new IllegalStateException(
                    "Only a finished game can update a tournament match."
            );
        }

        if (game.getResult() == null) {
            throw new IllegalStateException(
                    "Finished game must have a result."
            );
        }

        TournamentMatch match =
                game.getTournamentMatch();

        if (match.getStatus()
                == TournamentMatchStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Tournament match is already completed."
            );
        }

        Tournament tournament =
                match.getTournament();

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Tournament is not in progress."
            );
        }

        if (tournament.getFormat()
                == TournamentFormat.SINGLE_ELIMINATION) {

            recordKnockoutGameResult(
                    match,
                    game
            );

            return;
        }

        recordStandardGameResult(
                match,
                game
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

        /*
         * Snapshot the rating exactly once,
         * when the tournament starts.
         */
        for (TournamentParticipant participant : participants) {

            int rating =
                    ratingService.getRating(
                            participant.getUser(),
                            type
                    );

            participant.setRatingAtStart(
                    rating
            );
        }

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        sorted.sort(
                Comparator
                        .comparingInt(
                                (TournamentParticipant participant) ->
                                        participant.getRatingAtStart()
                        )
                        .reversed()
                        .thenComparing(
                                participant ->
                                        participant.getUser()
                                                .getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                participant ->
                                        participant.getUser()
                                                .getId()
                        )
        );

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setSeed(
                    i + 1
            );
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

    private int determineNextRoundNumber(
            Tournament tournament
    ) {

        return matchRepository
                .findByTournament(tournament)
                .stream()
                .map(TournamentMatch::getRoundNumber)
                .max(Integer::compareTo)
                .orElse(0)
                + 1;
    }

    private void resolveAutomaticResults(
            Tournament tournament,
            List<TournamentMatch> matches
    ) {

        for (TournamentMatch match : matches) {

            if (match.getStatus()
                    != TournamentMatchStatus.PENDING) {
                continue;
            }

            TournamentParticipant white =
                    match.getWhiteParticipant();

            TournamentParticipant black =
                    match.getBlackParticipant();

            // =====================================================
            // TRUE BYE
            // =====================================================

            if (black == null) {

                double points =
                        white.getStatus()
                                == TournamentParticipantStatus.ACTIVE
                                ? tournament.getByePoints()
                                : 0.0;

                match.setWhiteScore(points);
                match.setBlackScore(null);

                match.setTermination(
                        TournamentMatchTermination.BYE
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                addScore(white, points);

                continue;
            }

            boolean whiteForfeited =
                    white.getStatus()
                            == TournamentParticipantStatus.FORFEITED;

            boolean blackForfeited =
                    black.getStatus()
                            == TournamentParticipantStatus.FORFEITED;

            // =====================================================
            // DOUBLE FORFEIT
            // =====================================================

            if (whiteForfeited && blackForfeited) {

                match.setWhiteScore(0.0);
                match.setBlackScore(0.0);

                match.setTermination(
                        TournamentMatchTermination.DOUBLE_FORFEIT
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                continue;
            }

            // =====================================================
            // WHITE FORFEIT
            // =====================================================

            if (whiteForfeited) {

                match.setWhiteScore(0.0);
                match.setBlackScore(1.0);

                match.setTermination(
                        TournamentMatchTermination.WHITE_FORFEIT
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                addScore(black, 1.0);

                continue;
            }

            // =====================================================
            // BLACK FORFEIT
            // =====================================================

            if (blackForfeited) {

                match.setWhiteScore(1.0);
                match.setBlackScore(0.0);

                match.setTermination(
                        TournamentMatchTermination.BLACK_FORFEIT
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                addScore(white, 1.0);
            }
        }
    }

    private void addScore(
            TournamentParticipant participant,
            double points
    ) {

        double currentScore =
                participant.getScore() == null
                        ? 0.0
                        : participant.getScore();

        participant.setScore(
                currentScore + points
        );
    }

    private void validateCurrentRoundCompleted(
            Tournament tournament
    ) {

        List<TournamentMatch> matches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                tournament.getCurrentRound()
                        );

        boolean unfinished =
                matches.stream()
                        .anyMatch(match ->
                                match.getStatus()
                                        != TournamentMatchStatus.COMPLETED
                        );

        if (unfinished) {
            throw new IllegalStateException(
                    "The current round must be completed first."
            );
        }
    }

    private void validateTournamentCanStart(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required."
            );
        }

        if (tournament.getFormat()
                == TournamentFormat.SWISS) {

            Integer numberOfRounds =
                    tournament.getNumberOfRounds();

            if (numberOfRounds == null
                    || numberOfRounds < 1) {

                throw new IllegalStateException(
                        "Swiss tournament must have a valid number of rounds."
                );
            }

            if (numberOfRounds
                    >= participants.size()) {

                throw new IllegalStateException(
                        "A Swiss tournament must have fewer rounds than active participants."
                );
            }
        }
    }

    private void recordStandardGameResult(
            TournamentMatch match,
            Game game
    ) {

        switch (game.getResult()) {

            case WHITE_WIN ->
                    completeTournamentMatch(
                            match,
                            1.0,
                            0.0,
                            TournamentMatchTermination.PLAYED
                    );

            case BLACK_WIN ->
                    completeTournamentMatch(
                            match,
                            0.0,
                            1.0,
                            TournamentMatchTermination.PLAYED
                    );

            case DRAW ->
                    completeTournamentMatch(
                            match,
                            0.5,
                            0.5,
                            TournamentMatchTermination.PLAYED
                    );
        }
    }

    private void completeTournamentMatch(
            TournamentMatch match,
            double whiteScore,
            double blackScore,
            TournamentMatchTermination termination
    ) {

        match.setWhiteScore(
                whiteScore
        );

        match.setBlackScore(
                blackScore
        );

        match.setTermination(
                termination
        );

        match.setStatus(
                TournamentMatchStatus.COMPLETED
        );

        addScore(
                match.getWhiteParticipant(),
                whiteScore
        );

        addScore(
                match.getBlackParticipant(),
                blackScore
        );
    }

    private void recordKnockoutGameResult(
            TournamentMatch match,
            Game game
    ) {

        switch (game.getResult()) {

            case WHITE_WIN ->
                    completeKnockoutMatch(
                            match,
                            match.getWhiteParticipant(),
                            match.getBlackParticipant(),
                            1.0,
                            0.0
                    );

            case BLACK_WIN ->
                    completeKnockoutMatch(
                            match,
                            match.getBlackParticipant(),
                            match.getWhiteParticipant(),
                            0.0,
                            1.0
                    );

            case DRAW -> {
                /*
                 * The Game is finished, but the TournamentMatch
                 * remains unresolved.
                 *
                 * Another Game may later be created for this match.
                 */
            }
        }
    }

    private void completeKnockoutMatch(
            TournamentMatch match,
            TournamentParticipant winner,
            TournamentParticipant loser,
            double whiteScore,
            double blackScore
    ) {

        match.setWhiteScore(
                whiteScore
        );

        match.setBlackScore(
                blackScore
        );

        match.setTermination(
                TournamentMatchTermination.PLAYED
        );

        match.setStatus(
                TournamentMatchStatus.COMPLETED
        );

        loser.setStatus(
                TournamentParticipantStatus.ELIMINATED
        );
    }
}