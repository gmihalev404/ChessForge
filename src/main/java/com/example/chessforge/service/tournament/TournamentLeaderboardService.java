package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.enums.tournament.TieBreakType;
import com.example.chessforge.model.enums.tournament.TournamentMatchStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

@Component
class TournamentLeaderboardService {

    // =========================================================
    // PUBLIC ENTRY POINT
    // =========================================================

    List<TournamentParticipant> calculateStandings(
            Tournament tournament,
            List<TournamentParticipant> participants,
            List<TournamentMatch> matches
    ) {

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        /*
         * Primary tournament criterion is ALWAYS score.
         *
         * Seed is used here only to give us a deterministic
         * starting order. Configured tie-breaks are applied
         * afterwards to players with equal scores.
         */
        sorted.sort(
                Comparator
                        .comparingDouble(
                                (TournamentParticipant participant) ->
                                        getScore(participant)
                        )
                        .reversed()
                        .thenComparingInt(
                                this::getSeedForOrdering
                        )
        );

        List<TieBreakType> tieBreaks =
                tournament.getTieBreaks() == null
                        ? List.of()
                        : tournament.getTieBreaks();

        List<TournamentParticipant> result =
                new ArrayList<>();

        /*
         * First split everyone into groups with
         * exactly the same tournament score.
         */
        int start = 0;

        while (start < sorted.size()) {

            double score =
                    getScore(
                            sorted.get(start)
                    );

            int end =
                    start + 1;

            while (end < sorted.size()
                    && Double.compare(
                    getScore(sorted.get(end)),
                    score
            ) == 0) {

                end++;
            }

            List<TournamentParticipant> scoreGroup =
                    new ArrayList<>(
                            sorted.subList(
                                    start,
                                    end
                            )
                    );

            result.addAll(
                    resolveTieGroup(
                            tournament,
                            scoreGroup,
                            matches,
                            tieBreaks,
                            0
                    )
            );

            start = end;
        }

        return result;
    }

    // =========================================================
    // TIE-BREAK RESOLUTION
    // =========================================================

    private List<TournamentParticipant> resolveTieGroup(
            Tournament tournament,
            List<TournamentParticipant> group,
            List<TournamentMatch> matches,
            List<TieBreakType> tieBreaks,
            int tieBreakIndex
    ) {

        if (group.size() <= 1) {
            return group;
        }

        /*
         * All configured tie-breaks have been exhausted.
         * Keep the result deterministic.
         */
        if (tieBreakIndex >= tieBreaks.size()) {
            return deterministicOrder(group);
        }

        TieBreakType tieBreak =
                tieBreaks.get(
                        tieBreakIndex
                );

        return switch (tieBreak) {

            case BUCHHOLZ ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    calculateBuchholz(
                                            participant,
                                            matches
                                    )
                    );

            case BUCHHOLZ_CUT_1 ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    calculateBuchholzCut1(
                                            participant,
                                            matches
                                    )
                    );

            case SONNEBORN_BERGER ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    calculateSonnebornBerger(
                                            participant,
                                            matches
                                    )
                    );

            case DIRECT_ENCOUNTER ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    calculateDirectEncounterScore(
                                            participant,
                                            group,
                                            matches
                                    )
                    );

            case WINS ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    countWins(
                                            participant,
                                            matches
                                    )
                    );

            case BLACK_WINS ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    countBlackWins(
                                            participant,
                                            matches
                                    )
                    );

            case RATING ->
                    resolveNumericTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex,
                            participant ->
                                    participant.getRatingAtStart() == null
                                            ? 0
                                            : participant.getRatingAtStart()
                    );

            case ALPHABETICAL ->
                    resolveAlphabeticalTie(
                            tournament,
                            group,
                            matches,
                            tieBreaks,
                            tieBreakIndex
                    );
        };
    }

    // =========================================================
    // NUMERIC TIE-BREAK
    // =========================================================

    private List<TournamentParticipant> resolveNumericTie(
            Tournament tournament,
            List<TournamentParticipant> group,
            List<TournamentMatch> matches,
            List<TieBreakType> tieBreaks,
            int tieBreakIndex,
            ToDoubleFunction<TournamentParticipant> valueFunction
    ) {

        List<TournamentParticipant> sorted =
                new ArrayList<>(group);

        sorted.sort(
                Comparator
                        .comparingDouble(
                                valueFunction
                        )
                        .reversed()
                        .thenComparingInt(
                                this::getSeedForOrdering
                        )
        );

        List<TournamentParticipant> result =
                new ArrayList<>();

        int start = 0;

        while (start < sorted.size()) {

            double value =
                    valueFunction.applyAsDouble(
                            sorted.get(start)
                    );

            int end =
                    start + 1;

            while (end < sorted.size()
                    && Double.compare(
                    valueFunction.applyAsDouble(
                            sorted.get(end)
                    ),
                    value
            ) == 0) {

                end++;
            }

            List<TournamentParticipant> tied =
                    new ArrayList<>(
                            sorted.subList(
                                    start,
                                    end
                            )
                    );

            result.addAll(
                    resolveTieGroup(
                            tournament,
                            tied,
                            matches,
                            tieBreaks,
                            tieBreakIndex + 1
                    )
            );

            start = end;
        }

        return result;
    }

    // =========================================================
    // ALPHABETICAL
    // =========================================================

    private List<TournamentParticipant> resolveAlphabeticalTie(
            Tournament tournament,
            List<TournamentParticipant> group,
            List<TournamentMatch> matches,
            List<TieBreakType> tieBreaks,
            int tieBreakIndex
    ) {

        List<TournamentParticipant> sorted =
                new ArrayList<>(group);

        sorted.sort(
                Comparator.comparing(
                        participant ->
                                participant.getUser()
                                        .getUsername(),
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        List<TournamentParticipant> result =
                new ArrayList<>();

        int start = 0;

        while (start < sorted.size()) {

            String username =
                    sorted.get(start)
                            .getUser()
                            .getUsername();

            int end =
                    start + 1;

            while (end < sorted.size()
                    && String.CASE_INSENSITIVE_ORDER.compare(
                    sorted.get(end)
                            .getUser()
                            .getUsername(),
                    username
            ) == 0) {

                end++;
            }

            List<TournamentParticipant> tied =
                    new ArrayList<>(
                            sorted.subList(
                                    start,
                                    end
                            )
                    );

            result.addAll(
                    resolveTieGroup(
                            tournament,
                            tied,
                            matches,
                            tieBreaks,
                            tieBreakIndex + 1
                    )
            );

            start = end;
        }

        return result;
    }

    // =========================================================
    // BUCHHOLZ
    // =========================================================

    private double calculateBuchholz(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {

        return matches.stream()
                .filter(
                        this::isCompletedPlayedMatch
                )
                .filter(match ->
                        containsParticipant(
                                match,
                                participant
                        )
                )
                .map(match ->
                        getOpponent(
                                match,
                                participant
                        )
                )
                .mapToDouble(
                        this::getScore
                )
                .sum();
    }

    // =========================================================
    // BUCHHOLZ CUT 1
    // =========================================================

    private double calculateBuchholzCut1(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {

        List<Double> opponentScores =
                matches.stream()
                        .filter(
                                this::isCompletedPlayedMatch
                        )
                        .filter(match ->
                                containsParticipant(
                                        match,
                                        participant
                                )
                        )
                        .map(match ->
                                getOpponent(
                                        match,
                                        participant
                                )
                        )
                        .map(
                                this::getScore
                        )
                        .toList();

        if (opponentScores.isEmpty()) {
            return 0.0;
        }

        double total =
                opponentScores.stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .sum();

        double lowest =
                opponentScores.stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .min()
                        .orElse(0.0);

        return total - lowest;
    }

    // =========================================================
    // SONNEBORN-BERGER
    // =========================================================

    private double calculateSonnebornBerger(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {

        double result = 0.0;

        for (TournamentMatch match : matches) {

            if (!isCompletedPlayedMatch(match)) {
                continue;
            }

            if (!containsParticipant(
                    match,
                    participant
            )) {
                continue;
            }

            TournamentParticipant opponent =
                    getOpponent(
                            match,
                            participant
                    );

            Double matchScore =
                    getParticipantMatchScore(
                            match,
                            participant
                    );

            if (matchScore == null) {
                continue;
            }

            /*
             * Win:
             * 1.0 * opponent score
             *
             * Draw:
             * 0.5 * opponent score
             *
             * Loss:
             * 0.0
             */
            result +=
                    matchScore
                            * getScore(opponent);
        }

        return result;
    }

    // =========================================================
    // DIRECT ENCOUNTER
    // =========================================================

    private double calculateDirectEncounterScore(
            TournamentParticipant participant,
            List<TournamentParticipant> tiedGroup,
            List<TournamentMatch> matches
    ) {

        double result = 0.0;

        for (TournamentMatch match : matches) {

            if (!isCompletedPlayedMatch(match)) {
                continue;
            }

            if (!containsParticipant(
                    match,
                    participant
            )) {
                continue;
            }

            TournamentParticipant opponent =
                    getOpponent(
                            match,
                            participant
                    );

            if (!containsParticipant(
                    tiedGroup,
                    opponent
            )) {
                continue;
            }

            Double matchScore =
                    getParticipantMatchScore(
                            match,
                            participant
                    );

            if (matchScore != null) {
                result += matchScore;
            }
        }

        return result;
    }

    // =========================================================
    // WINS
    // =========================================================

    private int countWins(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {

        int wins = 0;

        for (TournamentMatch match : matches) {

            if (!isCompletedPlayedMatch(match)) {
                continue;
            }

            if (!containsParticipant(
                    match,
                    participant
            )) {
                continue;
            }

            Double participantScore =
                    getParticipantMatchScore(
                            match,
                            participant
                    );

            Double opponentScore =
                    getOpponentMatchScore(
                            match,
                            participant
                    );

            if (participantScore != null
                    && opponentScore != null
                    && participantScore
                    > opponentScore) {

                wins++;
            }
        }

        return wins;
    }

    // =========================================================
    // BLACK WINS
    // =========================================================

    private int countBlackWins(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {

        int wins = 0;

        for (TournamentMatch match : matches) {

            if (!isCompletedPlayedMatch(match)) {
                continue;
            }

            if (!sameParticipant(
                    match.getBlackParticipant(),
                    participant
            )) {
                continue;
            }

            if (match.getBlackScore() != null
                    && match.getWhiteScore() != null
                    && match.getBlackScore()
                    > match.getWhiteScore()) {

                wins++;
            }
        }

        return wins;
    }

    // =========================================================
    // MATCH HELPERS
    // =========================================================

    private boolean isCompletedPlayedMatch(
            TournamentMatch match
    ) {

        return match.getStatus()
                == TournamentMatchStatus.COMPLETED
                &&
                match.getWhiteParticipant()
                        != null
                &&
                match.getBlackParticipant()
                        != null;
    }

    private boolean containsParticipant(
            TournamentMatch match,
            TournamentParticipant participant
    ) {

        return sameParticipant(
                match.getWhiteParticipant(),
                participant
        )
                ||
                sameParticipant(
                        match.getBlackParticipant(),
                        participant
                );
    }

    private boolean containsParticipant(
            List<TournamentParticipant> participants,
            TournamentParticipant participant
    ) {

        return participants.stream()
                .anyMatch(current ->
                        sameParticipant(
                                current,
                                participant
                        )
                );
    }

    private TournamentParticipant getOpponent(
            TournamentMatch match,
            TournamentParticipant participant
    ) {

        if (sameParticipant(
                match.getWhiteParticipant(),
                participant
        )) {

            return match.getBlackParticipant();
        }

        if (sameParticipant(
                match.getBlackParticipant(),
                participant
        )) {

            return match.getWhiteParticipant();
        }

        throw new IllegalArgumentException(
                "Participant is not part of this match."
        );
    }

    private Double getParticipantMatchScore(
            TournamentMatch match,
            TournamentParticipant participant
    ) {

        if (sameParticipant(
                match.getWhiteParticipant(),
                participant
        )) {

            return match.getWhiteScore();
        }

        if (sameParticipant(
                match.getBlackParticipant(),
                participant
        )) {

            return match.getBlackScore();
        }

        return null;
    }

    private Double getOpponentMatchScore(
            TournamentMatch match,
            TournamentParticipant participant
    ) {

        if (sameParticipant(
                match.getWhiteParticipant(),
                participant
        )) {

            return match.getBlackScore();
        }

        if (sameParticipant(
                match.getBlackParticipant(),
                participant
        )) {

            return match.getWhiteScore();
        }

        return null;
    }

    // =========================================================
    // PARTICIPANT HELPERS
    // =========================================================

    private double getScore(
            TournamentParticipant participant
    ) {

        return participant.getScore()
                == null
                ? 0.0
                : participant.getScore();
    }

    private int getSeedForOrdering(
            TournamentParticipant participant
    ) {

        return participant.getSeed()
                == null
                ? Integer.MAX_VALUE
                : participant.getSeed();
    }

    private boolean sameParticipant(
            TournamentParticipant first,
            TournamentParticipant second
    ) {

        if (first == second) {
            return true;
        }

        if (first == null
                || second == null) {

            return false;
        }

        if (first.getId() != null
                && second.getId() != null) {

            return first.getId()
                    .equals(
                            second.getId()
                    );
        }

        if (first.getUser() != null
                && second.getUser() != null
                && first.getUser().getId() != null
                && second.getUser().getId() != null) {

            return first.getUser()
                    .getId()
                    .equals(
                            second.getUser()
                                    .getId()
                    );
        }

        return false;
    }

    // =========================================================
    // DETERMINISTIC FALLBACK
    // =========================================================

    private List<TournamentParticipant> deterministicOrder(
            List<TournamentParticipant> participants
    ) {

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        sorted.sort(
                Comparator
                        .comparingInt(
                                this::getSeedForOrdering
                        )
                        .thenComparing(
                                participant ->
                                        participant.getUser()
                                                .getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparingLong(
                                participant -> {

                                    if (participant.getUser()
                                            .getId() == null) {

                                        return Long.MAX_VALUE;
                                    }

                                    return participant
                                            .getUser()
                                            .getId();
                                }
                        )
        );

        return sorted;
    }
}