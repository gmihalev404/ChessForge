package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.enums.TieBreakType;
import com.example.chessforge.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
class TournamentLeaderboardService {

    private final RatingService ratingService;

    List<TournamentParticipant> calculateStandings(
            Tournament tournament,
            List<TournamentParticipant> participants,
            List<TournamentMatch> matches
    ) {

        List<TournamentParticipant> standings =
                new ArrayList<>(participants);

        Comparator<TournamentParticipant> comparator =
                Comparator.comparingDouble(
                        TournamentParticipant::getScore
                ).reversed();

        for (TieBreakType tieBreak : tournament.getTieBreaks()) {
            comparator = comparator.thenComparing(
                    comparatorFor(
                            tournament,
                            matches,
                            tieBreak
                    )
            );
        }

        standings.sort(comparator);

        return standings;
    }

    private Comparator<TournamentParticipant> comparatorFor(
            Tournament tournament,
            List<TournamentMatch> matches,
            TieBreakType type
    ) {

        return switch (type) {

            case BUCHHOLZ ->
                    Comparator.comparingDouble(
                            (TournamentParticipant p) -> calculateBuchholz(p, matches)
                    ).reversed();

            case BUCHHOLZ_CUT_1 ->
                    Comparator.comparingDouble(
                            (TournamentParticipant p) -> calculateBuchholzCut1(p, matches)
                    ).reversed();

            case SONNEBORN_BERGER ->
                    Comparator.comparingDouble(
                            (TournamentParticipant p) -> calculateSonnebornBerger(p, matches)
                    ).reversed();

            case DIRECT_ENCOUNTER ->
                    directEncounterComparator(matches);

            case WINS ->
                    Comparator.comparingInt(
                            (TournamentParticipant p) -> countWins(p, matches)
                    ).reversed();

            case BLACK_WINS ->
                    Comparator.comparingInt(
                            (TournamentParticipant p) -> countBlackWins(p, matches)
                    ).reversed();

            case RATING ->
                    Comparator.comparingInt(
                            (TournamentParticipant p) -> ratingService.getRating(
                                    p.getUser(),
                                    tournament
                                            .getTimeControl()
                                            .getType()
                            )
                    ).reversed();

            case ALPHABETICAL ->
                    Comparator.comparing(
                            p -> p.getUser().getUsername(),
                            String.CASE_INSENSITIVE_ORDER
                    );
        };
    }

    private double calculateBuchholz(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {
        // implement after pairing
        return 0.0;
    }

    private double calculateBuchholzCut1(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {
        return 0.0;
    }

    private double calculateSonnebornBerger(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {
        return 0.0;
    }

    private Comparator<TournamentParticipant>
    directEncounterComparator(
            List<TournamentMatch> matches
    ) {
        return (a, b) -> 0;
    }

    private int countWins(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {
        return 0;
    }

    private int countBlackWins(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {
        return 0;
    }
}