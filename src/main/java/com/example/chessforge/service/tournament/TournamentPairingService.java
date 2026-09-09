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

    // =========================================================
    // INITIAL PAIRINGS
    // =========================================================

    List<TournamentMatch> createInitialPairings(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        List<TournamentParticipant> activeParticipants =
                getActiveParticipants(participants);

        return switch (tournament.getFormat()) {

            case SINGLE_ELIMINATION ->
                    createSingleEliminationFirstRound(
                            tournament,
                            prepareSeededParticipants(activeParticipants)
                    );

            case ROUND_ROBIN ->
                    createRoundRobinSchedule(
                            tournament,
                            activeParticipants
                    );

            case SWISS ->
                    createSwissRound(
                            tournament,
                            activeParticipants,
                            1
                    );
        };
    }

    // =========================================================
    // NEXT ROUND
    // =========================================================

    List<TournamentMatch> createNextRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            List<TournamentMatch> previousRoundMatches,
            int roundNumber
    ) {

        List<TournamentParticipant> activeParticipants =
                getActiveParticipants(participants);

        return switch (tournament.getFormat()) {

            case SINGLE_ELIMINATION ->
                    createSingleEliminationLaterRound(
                            tournament,
                            previousRoundMatches,
                            roundNumber
                    );

            case SWISS ->
                    createSwissRound(
                            tournament,
                            activeParticipants,
                            roundNumber
                    );

            case ROUND_ROBIN ->
                    throw new IllegalStateException(
                            "Round-robin pairings are created in advance."
                    );
        };
    }

    // =========================================================
    // SINGLE ELIMINATION
    // =========================================================

    private List<TournamentMatch> createSingleEliminationFirstRound(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        int bracketSize =
                nextPowerOfTwo(participants.size());

        List<Integer> seedOrder =
                createBracketSeedOrder(bracketSize);

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        for (int i = 0; i < seedOrder.size(); i += 2) {

            TournamentParticipant first =
                    findParticipantBySeed(
                            participants,
                            seedOrder.get(i)
                    );

            TournamentParticipant second =
                    findParticipantBySeed(
                            participants,
                            seedOrder.get(i + 1)
                    );

            /*
             * One of the bracket positions may be empty
             * when the number of players is not a power of two.
             */
            if (first == null || second == null) {

                TournamentParticipant participant =
                        first != null
                                ? first
                                : second;

                matches.add(
                        createByeMatch(
                                tournament,
                                participant,
                                1,
                                boardNumber++
                        )
                );

                continue;
            }

            matches.add(
                    createMatch(
                            tournament,
                            first,
                            second,
                            1,
                            boardNumber++
                    )
            );
        }

        return matches;
    }

    private List<TournamentMatch> createSingleEliminationLaterRound(
            Tournament tournament,
            List<TournamentMatch> previousRoundMatches,
            int roundNumber
    ) {

        if (previousRoundMatches.size() % 2 != 0) {
            throw new IllegalStateException(
                    "A knockout round must contain an even number of matches."
            );
        }

        List<TournamentMatch> sortedMatches =
                previousRoundMatches.stream()
                        .sorted(
                                Comparator.comparing(
                                        TournamentMatch::getBoardNumber
                                )
                        )
                        .toList();

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        for (int i = 0;
             i < sortedMatches.size();
             i += 2) {

            TournamentParticipant first =
                    getSingleEliminationWinner(
                            sortedMatches.get(i)
                    );

            TournamentParticipant second =
                    getSingleEliminationWinner(
                            sortedMatches.get(i + 1)
                    );

            matches.add(
                    createMatch(
                            tournament,
                            first,
                            second,
                            roundNumber,
                            boardNumber++
                    )
            );
        }

        return matches;
    }

    // =========================================================
    // ROUND ROBIN
    // =========================================================

    private List<TournamentMatch> createRoundRobinSchedule(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required."
            );
        }

        List<TournamentParticipant> rotation =
                new ArrayList<>(participants);

        validateSeeds(rotation);

        rotation.sort(
                Comparator.comparing(
                        TournamentParticipant::getSeed
                )
        );

        /*
         * Circle method requires an even number of slots.
         *
         * For an odd number of players, null represents
         * a permanent BYE slot.
         */
        if (rotation.size() % 2 != 0) {
            rotation.add(null);
        }

        int totalRounds =
                rotation.size() - 1;

        List<TournamentMatch> matches =
                new ArrayList<>();

        for (int roundNumber = 1;
             roundNumber <= totalRounds;
             roundNumber++) {

            int boardNumber = 1;

            for (int i = 0;
                 i < rotation.size() / 2;
                 i++) {

                TournamentParticipant first =
                        rotation.get(i);

                TournamentParticipant second =
                        rotation.get(
                                rotation.size() - 1 - i
                        );

                /*
                 * A null participant means that the other
                 * participant receives a real BYE.
                 */
                if (first == null || second == null) {

                    TournamentParticipant participant =
                            first != null
                                    ? first
                                    : second;

                    matches.add(
                            createByeMatch(
                                    tournament,
                                    participant,
                                    roundNumber,
                                    boardNumber++
                            )
                    );

                    continue;
                }

                matches.add(
                        createRoundRobinMatch(
                                tournament,
                                first,
                                second,
                                roundNumber,
                                boardNumber++
                        )
                );
            }

            rotateRoundRobin(rotation);
        }

        return matches;
    }

    private void rotateRoundRobin(
            List<TournamentParticipant> participants
    ) {

        TournamentParticipant last =
                participants.remove(
                        participants.size() - 1
                );

        participants.add(1, last);
    }

    private TournamentMatch createRoundRobinMatch(
            Tournament tournament,
            TournamentParticipant first,
            TournamentParticipant second,
            int roundNumber,
            int boardNumber
    ) {

        int pairIndex = boardNumber - 1;

        boolean firstIsWhite;

        if (pairIndex == 0) {
            firstIsWhite =
                    roundNumber % 2 != 0;
        } else {
            firstIsWhite =
                    pairIndex % 2 == 0;
        }

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
    // COMMON MATCH CREATION
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
                .status(TournamentMatchStatus.PENDING)
                .termination(null)
                .whiteScore(null)
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

    private int nextPowerOfTwo(
            int number
    ) {

        int power = 1;

        while (power < number) {
            power *= 2;
        }

        return power;
    }

    private List<Integer> createBracketSeedOrder(
            int bracketSize
    ) {

        List<Integer> seeds =
                new ArrayList<>();

        seeds.add(1);
        seeds.add(2);

        int currentSize = 2;

        while (currentSize < bracketSize) {

            int nextSize =
                    currentSize * 2;

            List<Integer> expanded =
                    new ArrayList<>();

            for (Integer seed : seeds) {

                expanded.add(seed);
                expanded.add(
                        nextSize + 1 - seed
                );
            }

            seeds = expanded;
            currentSize = nextSize;
        }

        return seeds;
    }

    private TournamentParticipant findParticipantBySeed(
            List<TournamentParticipant> participants,
            int seed
    ) {

        return participants.stream()
                .filter(participant ->
                        participant.getSeed() == seed
                )
                .findFirst()
                .orElse(null);
    }

    private TournamentParticipant getSingleEliminationWinner(
            TournamentMatch match
    ) {

        if (match.getStatus()
                != TournamentMatchStatus.COMPLETED) {

            throw new IllegalStateException(
                    "All matches from the previous round must be completed."
            );
        }

        /*
         * Real BYE.
         */
        if (match.getBlackParticipant() == null) {
            return match.getWhiteParticipant();
        }

        if (match.getWhiteScore() == null
                || match.getBlackScore() == null) {

            throw new IllegalStateException(
                    "Completed knockout match must have a result."
            );
        }

        if (match.getWhiteScore()
                > match.getBlackScore()) {

            return match.getWhiteParticipant();
        }

        if (match.getBlackScore()
                > match.getWhiteScore()) {

            return match.getBlackParticipant();
        }

        throw new IllegalStateException(
                "A knockout match must have a winner before the next round can be created."
        );
    }

    private List<TournamentParticipant> prepareSeededParticipants(
            List<TournamentParticipant> participants
    ) {

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required to create a round."
            );
        }

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        validateSeeds(sorted);

        sorted.sort(
                Comparator.comparing(
                        TournamentParticipant::getSeed
                )
        );

        return sorted;
    }
}