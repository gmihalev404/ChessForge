package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.enums.TournamentMatchStatus;
import com.example.chessforge.model.enums.TournamentMatchTermination;
import com.example.chessforge.model.enums.TournamentParticipantStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
class TournamentPairingService {

    List<TournamentMatch> createInitialPairings(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        List<TournamentParticipant> activeParticipants =
                getActiveParticipants(participants);

        return switch (tournament.getFormat()) {

            case SINGLE_ELIMINATION ->
                    createSingleEliminationRound(
                            tournament,
                            activeParticipants,
                            1
                    );

            case ROUND_ROBIN ->
                    createRoundRobinRound(
                            tournament,
                            activeParticipants,
                            1
                    );

            case SWISS ->
                    createSwissRound(
                            tournament,
                            activeParticipants,
                            1
                    );
        };
    }

    List<TournamentMatch> createNextRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            int roundNumber
    ) {

        List<TournamentParticipant> activeParticipants =
                getActiveParticipants(participants);

        return switch (tournament.getFormat()) {

            case SINGLE_ELIMINATION ->
                    createSingleEliminationRound(
                            tournament,
                            activeParticipants,
                            roundNumber
                    );

            case ROUND_ROBIN ->
                    createRoundRobinRound(
                            tournament,
                            activeParticipants,
                            roundNumber
                    );

            case SWISS ->
                    createSwissRound(
                            tournament,
                            activeParticipants,
                            roundNumber
                    );
        };
    }

    // =========================================================
    // SINGLE ELIMINATION
    // =========================================================

    private List<TournamentMatch> createSingleEliminationRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            int roundNumber
    ) {

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required to create a round."
            );
        }

        List<TournamentParticipant> sortedParticipants =
                new ArrayList<>(participants);

        validateSeeds(sortedParticipants);

        sortedParticipants.sort(
                Comparator.comparing(
                        TournamentParticipant::getSeed
                )
        );

        if (roundNumber == 1) {
            return createSingleEliminationFirstRound(
                    tournament,
                    sortedParticipants
            );
        }

        return createSingleEliminationLaterRound(
                tournament,
                sortedParticipants,
                roundNumber
        );
    }

    private List<TournamentMatch> createSingleEliminationFirstRound(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        int bracketSize =
                nextPowerOfTwo(participants.size());

        int byeCount =
                bracketSize - participants.size();

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        for (int i = 0; i < byeCount; i++) {

            TournamentParticipant participant =
                    participants.get(i);

            matches.add(
                    createByeMatch(
                            tournament,
                            participant,
                            1,
                            boardNumber++
                    )
            );
        }

        int left = byeCount;
        int right = participants.size() - 1;

        while (left < right) {

            TournamentParticipant higherSeed =
                    participants.get(left);

            TournamentParticipant lowerSeed =
                    participants.get(right);

            matches.add(
                    createMatch(
                            tournament,
                            higherSeed,
                            lowerSeed,
                            1,
                            boardNumber++
                    )
            );

            left++;
            right--;
        }

        return matches;
    }

    private List<TournamentMatch> createSingleEliminationLaterRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            int roundNumber
    ) {

        if (participants.size() % 2 != 0) {
            throw new IllegalStateException(
                    "A later single-elimination round must have an even number of active participants."
            );
        }

        List<TournamentMatch> matches =
                new ArrayList<>();

        int left = 0;
        int right = participants.size() - 1;
        int boardNumber = 1;

        while (left < right) {

            TournamentParticipant higherSeed =
                    participants.get(left);

            TournamentParticipant lowerSeed =
                    participants.get(right);

            matches.add(
                    createMatch(
                            tournament,
                            higherSeed,
                            lowerSeed,
                            roundNumber,
                            boardNumber++
                    )
            );

            left++;
            right--;
        }

        return matches;
    }

    // =========================================================
    // ROUND ROBIN
    // =========================================================

    private List<TournamentMatch> createRoundRobinRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            int roundNumber
    ) {

        // TODO: implement after single elimination lifecycle
        throw new UnsupportedOperationException(
                "Round-robin pairing is not implemented yet."
        );
    }

    // =========================================================
    // SWISS
    // =========================================================

    private List<TournamentMatch> createSwissRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            int roundNumber
    ) {

        // TODO: implement after round robin
        throw new UnsupportedOperationException(
                "Swiss pairing is not implemented yet."
        );
    }

    // =========================================================
    // MATCH CREATION
    // =========================================================

    private TournamentMatch createByeMatch(
            Tournament tournament,
            TournamentParticipant participant,
            int roundNumber,
            int boardNumber
    ) {

        return TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(participant)
                .blackParticipant(null)
                .roundNumber(roundNumber)
                .boardNumber(boardNumber)
                .status(TournamentMatchStatus.COMPLETED)
                .termination(TournamentMatchTermination.BYE)
                .whiteScore(tournament.getByePoints())
                .blackScore(null)
                .build();
    }

    private TournamentMatch createMatch(
            Tournament tournament,
            TournamentParticipant first,
            TournamentParticipant second,
            int roundNumber,
            int boardNumber
    ) {

        boolean firstIsWhite =
                boardNumber % 2 != 0;

        TournamentParticipant whiteParticipant =
                firstIsWhite ? first : second;

        TournamentParticipant blackParticipant =
                firstIsWhite ? second : first;

        return TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(whiteParticipant)
                .blackParticipant(blackParticipant)
                .roundNumber(roundNumber)
                .boardNumber(boardNumber)
                .status(TournamentMatchStatus.PENDING)
                .whiteScore(null)
                .blackScore(null)
                .build();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private List<TournamentParticipant> getActiveParticipants(
            List<TournamentParticipant> participants
    ) {

        return participants.stream()
                .filter(participant ->
                        participant.getStatus()
                                == TournamentParticipantStatus.ACTIVE
                )
                .toList();
    }

    private void validateSeeds(
            List<TournamentParticipant> participants
    ) {

        boolean missingSeed =
                participants.stream()
                        .anyMatch(participant ->
                                participant.getSeed() == null
                        );

        if (missingSeed) {
            throw new IllegalStateException(
                    "All participants must have a seed before pairings are created."
            );
        }
    }

    private int nextPowerOfTwo(int number) {

        int power = 1;

        while (power < number) {
            power *= 2;
        }

        return power;
    }
}