package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.enums.TournamentMatchStatus;
import com.example.chessforge.model.enums.TournamentMatchType;
import com.example.chessforge.model.enums.TournamentParticipantStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

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
                            prepareSeededParticipants(
                                    activeParticipants
                            )
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
                            List.of(),
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
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        List<TournamentParticipant> activeParticipants =
                getActiveParticipants(participants);

        return switch (tournament.getFormat()) {

            case SINGLE_ELIMINATION -> {

                List<TournamentMatch> previousRoundMatches =
                        matchHistory.stream()
                                .filter(match ->
                                        match.getRoundNumber()
                                                == roundNumber - 1
                                )
                                .toList();

                yield createSingleEliminationLaterRound(
                        tournament,
                        previousRoundMatches,
                        roundNumber
                );
            }

            case SWISS ->
                    createSwissRound(
                            tournament,
                            activeParticipants,
                            matchHistory,
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

    private List<TournamentMatch>
    createSingleEliminationFirstRound(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        int bracketSize =
                nextPowerOfTwo(
                        participants.size()
                );

        List<Integer> seedOrder =
                createBracketSeedOrder(
                        bracketSize
                );

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        for (int i = 0;
             i < seedOrder.size();
             i += 2) {

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
             * Empty bracket position -> BYE.
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

    private List<TournamentMatch>
    createSingleEliminationLaterRound(
            Tournament tournament,
            List<TournamentMatch> previousRoundMatches,
            int roundNumber
    ) {

        if (previousRoundMatches.isEmpty()) {
            throw new IllegalStateException(
                    "Previous knockout round contains no matches."
            );
        }

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

        /*
         * Two matches in the previous round mean that
         * they were the semifinals.
         *
         * Winners -> final
         * Losers  -> third-place match
         */
        if (sortedMatches.size() == 2) {

            TournamentMatch firstSemi =
                    sortedMatches.get(0);

            TournamentMatch secondSemi =
                    sortedMatches.get(1);

            TournamentParticipant firstWinner =
                    getSingleEliminationWinner(
                            firstSemi
                    );

            TournamentParticipant secondWinner =
                    getSingleEliminationWinner(
                            secondSemi
                    );

            TournamentParticipant firstLoser =
                    getSingleEliminationLoser(
                            firstSemi
                    );

            TournamentParticipant secondLoser =
                    getSingleEliminationLoser(
                            secondSemi
                    );

            List<TournamentMatch> matches =
                    new ArrayList<>();

            /*
             * Final.
             */
            matches.add(
                    createMatch(
                            tournament,
                            firstWinner,
                            secondWinner,
                            roundNumber,
                            1,
                            TournamentMatchType.MAIN
                    )
            );

            /*
             * If both semifinals were actual games,
             * their losers play for third place.
             *
             * With three participants one semifinal
             * may be a BYE, so there is no second loser.
             */
            if (firstLoser != null
                    && secondLoser != null) {

                matches.add(
                        createMatch(
                                tournament,
                                firstLoser,
                                secondLoser,
                                roundNumber,
                                2,
                                TournamentMatchType.THIRD_PLACE
                        )
                );
            }

            return matches;
        }

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        /*
         * Earlier knockout rounds:
         *
         * winner board 1 vs winner board 2
         * winner board 3 vs winner board 4
         * ...
         */
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
         * null = permanent BYE slot.
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

        /*
         * Index 0 stays fixed.
         */
        participants.add(
                1,
                last
        );
    }

    private TournamentMatch createRoundRobinMatch(
            Tournament tournament,
            TournamentParticipant first,
            TournamentParticipant second,
            int roundNumber,
            int boardNumber
    ) {

        /*
         * Produces balanced colors for the standard
         * circle-method rotation.
         */
        int pairIndex =
                boardNumber - 1;

        boolean firstIsWhite;

        if (pairIndex == 0) {
            firstIsWhite =
                    roundNumber % 2 != 0;
        } else {
            firstIsWhite =
                    pairIndex % 2 == 0;
        }

        TournamentParticipant whiteParticipant =
                firstIsWhite
                        ? first
                        : second;

        TournamentParticipant blackParticipant =
                firstIsWhite
                        ? second
                        : first;

        return TournamentMatch.builder()
                .tournament(tournament)
                .whiteParticipant(
                        whiteParticipant
                )
                .blackParticipant(
                        blackParticipant
                )
                .roundNumber(
                        roundNumber
                )
                .boardNumber(
                        boardNumber
                )
                .status(
                        TournamentMatchStatus.PENDING
                )
                .whiteScore(null)
                .blackScore(null)
                .type(
                        TournamentMatchType.MAIN
                )
                .build();
    }

    // =========================================================
    // SWISS
    // =========================================================

    private List<TournamentMatch> createSwissRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required for Swiss pairing."
            );
        }

        validateSeeds(participants);

        if (roundNumber == 1) {
            return createSwissFirstRound(
                    tournament,
                    participants
            );
        }

        return createSwissLaterRound(
                tournament,
                participants,
                matchHistory,
                roundNumber
        );
    }

    // =========================================================
    // SWISS - FIRST ROUND
    // =========================================================

    private List<TournamentMatch> createSwissFirstRound(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        /*
         * Seeds have already been assigned using:
         *
         * rating DESC
         * username ASC
         * id ASC
         */
        sorted.sort(
                Comparator.comparing(
                        TournamentParticipant::getSeed
                )
        );

        TournamentParticipant byeParticipant =
                null;

        /*
         * First round:
         * lowest seed receives the BYE.
         */
        if (sorted.size() % 2 != 0) {

            byeParticipant =
                    sorted.remove(
                            sorted.size() - 1
                    );
        }

        int half =
                sorted.size() / 2;

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        /*
         * Example:
         *
         * 1 2 3 4 | 5 6 7 8
         *
         * 1 - 5
         * 2 - 6
         * 3 - 7
         * 4 - 8
         */
        for (int i = 0;
             i < half;
             i++) {

            TournamentParticipant upper =
                    sorted.get(i);

            TournamentParticipant lower =
                    sorted.get(
                            i + half
                    );

            matches.add(
                    createSwissMatch(
                            tournament,
                            upper,
                            lower,
                            1,
                            boardNumber++,
                            List.of()
                    )
            );
        }

        if (byeParticipant != null) {

            matches.add(
                    createByeMatch(
                            tournament,
                            byeParticipant,
                            1,
                            boardNumber
                    )
            );
        }

        return matches;
    }

    // =========================================================
    // SWISS - LATER ROUNDS
    // =========================================================

    private List<TournamentMatch> createSwissLaterRound(
            Tournament tournament,
            List<TournamentParticipant> participants,
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        List<TournamentParticipant> sorted =
                new ArrayList<>(participants);

        /*
         * Current Swiss standings:
         *
         * score DESC
         * seed ASC
         */
        sorted.sort(
                Comparator
                        .comparingDouble(
                                (TournamentParticipant participant) ->
                                        getScore(
                                                participant
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                TournamentParticipant::getSeed
                        )
        );

        TournamentParticipant byeParticipant =
                null;

        /*
         * Lowest-ranked participant who has never
         * received a BYE gets it.
         */
        if (sorted.size() % 2 != 0) {

            byeParticipant =
                    selectSwissByeParticipant(
                            sorted,
                            matchHistory
                    );

            sorted.remove(
                    byeParticipant
            );
        }

        List<SwissPlayer> swissPlayers =
                createSwissPlayers(
                        sorted
                );

        List<SwissPair> pairs =
                pairSwissPlayers(
                        swissPlayers,
                        matchHistory,
                        roundNumber
                );

        if (pairs == null) {
            throw new IllegalStateException(
                    "No valid Swiss pairing exists without a rematch."
            );
        }

        List<TournamentMatch> matches =
                new ArrayList<>();

        int boardNumber = 1;

        for (SwissPair pair : pairs) {

            matches.add(
                    createSwissMatch(
                            tournament,
                            pair.first()
                                    .participant(),
                            pair.second()
                                    .participant(),
                            roundNumber,
                            boardNumber++,
                            matchHistory
                    )
            );
        }

        if (byeParticipant != null) {

            matches.add(
                    createByeMatch(
                            tournament,
                            byeParticipant,
                            roundNumber,
                            boardNumber
                    )
            );
        }

        return matches;
    }

    // =========================================================
    // SWISS - SCORE GROUPS
    // =========================================================

    private List<SwissPlayer> createSwissPlayers(
            List<TournamentParticipant> sortedParticipants
    ) {

        List<SwissPlayer> result =
                new ArrayList<>();

        int groupIndex = 0;
        int start = 0;

        while (start
                < sortedParticipants.size()) {

            double score =
                    getScore(
                            sortedParticipants.get(
                                    start
                            )
                    );

            int end = start;

            /*
             * Find the complete equal-score group.
             */
            while (end
                    < sortedParticipants.size()
                    &&
                    Double.compare(
                            getScore(
                                    sortedParticipants.get(
                                            end
                                    )
                            ),
                            score
                    ) == 0) {

                end++;
            }

            int groupSize =
                    end - start;

            /*
             * Even group:
             *
             * 6 -> 3 upper / 3 lower
             *
             * Odd group:
             *
             * 5 -> 2 upper / 3 lower
             */
            int upperSize =
                    groupSize / 2;

            for (int i = start;
                 i < end;
                 i++) {

                SwissHalf half =
                        i - start
                                < upperSize
                                ? SwissHalf.UPPER
                                : SwissHalf.LOWER;

                result.add(
                        new SwissPlayer(
                                sortedParticipants.get(
                                        i
                                ),
                                score,
                                groupIndex,
                                half
                        )
                );
            }

            groupIndex++;
            start = end;
        }

        return result;
    }

    // =========================================================
    // SWISS - BACKTRACKING
    // =========================================================

    private List<SwissPair> pairSwissPlayers(
            List<SwissPlayer> players,
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        if (players.isEmpty()) {
            return new ArrayList<>();
        }

        /*
         * Always begin with the highest-ranked
         * remaining player.
         */
        SwissPlayer current =
                players.get(0);

        List<SwissPlayer> candidates =
                getSwissCandidates(
                        current,
                        players,
                        matchHistory,
                        roundNumber
                );

        for (SwissPlayer candidate
                : candidates) {

            List<SwissPlayer> remaining =
                    new ArrayList<>(
                            players
                    );

            remaining.remove(
                    current
            );

            remaining.remove(
                    candidate
            );

            List<SwissPair> remainingPairs =
                    pairSwissPlayers(
                            remaining,
                            matchHistory,
                            roundNumber
                    );

            /*
             * A complete valid pairing was found.
             */
            if (remainingPairs != null) {

                List<SwissPair> result =
                        new ArrayList<>();

                result.add(
                        new SwissPair(
                                current,
                                candidate
                        )
                );

                result.addAll(
                        remainingPairs
                );

                return result;
            }
        }

        /*
         * No candidate allows the rest of the
         * tournament round to be paired.
         *
         * Caller will backtrack and try another
         * opponent.
         */
        return null;
    }

    // =========================================================
    // SWISS - CANDIDATE PRIORITY
    // =========================================================

    private List<SwissPlayer> getSwissCandidates(
            SwissPlayer player,
            List<SwissPlayer> players,
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        List<SwissPlayer> candidates =
                new ArrayList<>();

        /*
         * PRIORITY 1
         *
         * Same score group,
         * opposite half.
         */
        addSwissCandidates(
                candidates,
                player,
                players,
                matchHistory,
                roundNumber,
                candidate ->
                        candidate.groupIndex()
                                == player.groupIndex()
                                &&
                                candidate.half()
                                        != player.half()
        );

        /*
         * PRIORITY 2
         *
         * Same score group,
         * same half.
         */
        addSwissCandidates(
                candidates,
                player,
                players,
                matchHistory,
                roundNumber,
                candidate ->
                        candidate.groupIndex()
                                == player.groupIndex()
                                &&
                                candidate.half()
                                        == player.half()
        );

        /*
         * PRIORITY 3+
         *
         * Lower score groups,
         * one group at a time.
         */
        int maxGroup =
                players.stream()
                        .mapToInt(
                                SwissPlayer::groupIndex
                        )
                        .max()
                        .orElse(
                                player.groupIndex()
                        );

        for (int group =
             player.groupIndex() + 1;
             group <= maxGroup;
             group++) {

            int targetGroup =
                    group;

            addSwissCandidates(
                    candidates,
                    player,
                    players,
                    matchHistory,
                    roundNumber,
                    candidate ->
                            candidate.groupIndex()
                                    == targetGroup
            );
        }

        return candidates;
    }

    private void addSwissCandidates(
            List<SwissPlayer> result,
            SwissPlayer player,
            List<SwissPlayer> players,
            List<TournamentMatch> matchHistory,
            int roundNumber,
            Predicate<SwissPlayer> location
    ) {

        /*
         * First pass:
         *
         * same search location
         * +
         * NO REMATCH
         * +
         * opposite colors in previous round
         */
        for (SwissPlayer candidate
                : players) {

            if (candidate == player) {
                continue;
            }

            if (!location.test(
                    candidate
            )) {
                continue;
            }

            if (havePlayedBefore(
                    player.participant(),
                    candidate.participant(),
                    matchHistory
            )) {
                continue;
            }

            if (hadOppositeColorsLastRound(
                    player.participant(),
                    candidate.participant(),
                    matchHistory,
                    roundNumber
            )) {

                if (!result.contains(
                        candidate
                )) {

                    result.add(
                            candidate
                    );
                }
            }
        }

        /*
         * Second pass:
         *
         * same search location
         * +
         * NO REMATCH
         * +
         * any color combination.
         *
         * Color preference never overrides the
         * search-location priority.
         */
        for (SwissPlayer candidate
                : players) {

            if (candidate == player) {
                continue;
            }

            if (!location.test(
                    candidate
            )) {
                continue;
            }

            if (havePlayedBefore(
                    player.participant(),
                    candidate.participant(),
                    matchHistory
            )) {
                continue;
            }

            if (!result.contains(
                    candidate
            )) {

                result.add(
                        candidate
                );
            }
        }
    }

    // =========================================================
    // SWISS - REMATCH HISTORY
    // =========================================================

    private boolean havePlayedBefore(
            TournamentParticipant first,
            TournamentParticipant second,
            List<TournamentMatch> matchHistory
    ) {

        return matchHistory.stream()
                .filter(match ->
                        match.getBlackParticipant()
                                != null
                )
                .anyMatch(match ->
                        (
                                sameParticipant(
                                        match.getWhiteParticipant(),
                                        first
                                )
                                        &&
                                        sameParticipant(
                                                match.getBlackParticipant(),
                                                second
                                        )
                        )
                                ||
                                (
                                        sameParticipant(
                                                match.getWhiteParticipant(),
                                                second
                                        )
                                                &&
                                                sameParticipant(
                                                        match.getBlackParticipant(),
                                                        first
                                                )
                                )
                );
    }

    // =========================================================
    // SWISS - PREVIOUS COLORS
    // =========================================================

    private SwissColor getColorInPreviousRound(
            TournamentParticipant participant,
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        int previousRound =
                roundNumber - 1;

        return matchHistory.stream()
                .filter(match ->
                        match.getRoundNumber()
                                == previousRound
                )
                /*
                 * BYE has no chess color.
                 */
                .filter(match ->
                        match.getBlackParticipant()
                                != null
                )
                .filter(match ->
                        sameParticipant(
                                match.getWhiteParticipant(),
                                participant
                        )
                                ||
                                sameParticipant(
                                        match.getBlackParticipant(),
                                        participant
                                )
                )
                .findFirst()
                .map(match -> {

                    if (sameParticipant(
                            match.getWhiteParticipant(),
                            participant
                    )) {

                        return SwissColor.WHITE;
                    }

                    return SwissColor.BLACK;
                })
                .orElse(
                        SwissColor.NONE
                );
    }

    private boolean hadOppositeColorsLastRound(
            TournamentParticipant first,
            TournamentParticipant second,
            List<TournamentMatch> matchHistory,
            int roundNumber
    ) {

        SwissColor firstColor =
                getColorInPreviousRound(
                        first,
                        matchHistory,
                        roundNumber
                );

        SwissColor secondColor =
                getColorInPreviousRound(
                        second,
                        matchHistory,
                        roundNumber
                );

        return firstColor
                != SwissColor.NONE
                &&
                secondColor
                        != SwissColor.NONE
                &&
                firstColor
                        != secondColor;
    }

    // =========================================================
    // SWISS - BYE
    // =========================================================

    private TournamentParticipant selectSwissByeParticipant(
            List<TournamentParticipant> sortedParticipants,
            List<TournamentMatch> matchHistory
    ) {

        /*
         * Search from the bottom of the standings.
         */
        for (int i =
             sortedParticipants.size() - 1;
             i >= 0;
             i--) {

            TournamentParticipant participant =
                    sortedParticipants.get(i);

            if (!hasReceivedBye(
                    participant,
                    matchHistory
            )) {

                return participant;
            }
        }

        throw new IllegalStateException(
                "No participant is eligible for another Swiss bye."
        );
    }

    private boolean hasReceivedBye(
            TournamentParticipant participant,
            List<TournamentMatch> matchHistory
    ) {

        return matchHistory.stream()
                .anyMatch(match ->
                        match.getBlackParticipant()
                                == null
                                &&
                                sameParticipant(
                                        match.getWhiteParticipant(),
                                        participant
                                )
                );
    }

    // =========================================================
    // SWISS - MATCH CREATION
    // =========================================================

    private TournamentMatch createSwissMatch(
            Tournament tournament,
            TournamentParticipant first,
            TournamentParticipant second,
            int roundNumber,
            int boardNumber,
            List<TournamentMatch> matchHistory
    ) {

        boolean firstIsWhite =
                determineSwissWhitePlayer(
                        first,
                        second,
                        matchHistory,
                        roundNumber,
                        boardNumber
                );

        TournamentParticipant white =
                firstIsWhite
                        ? first
                        : second;

        TournamentParticipant black =
                firstIsWhite
                        ? second
                        : first;

        return TournamentMatch.builder()
                .tournament(
                        tournament
                )
                .whiteParticipant(
                        white
                )
                .blackParticipant(
                        black
                )
                .roundNumber(
                        roundNumber
                )
                .boardNumber(
                        boardNumber
                )
                .status(
                        TournamentMatchStatus.PENDING
                )
                .whiteScore(null)
                .blackScore(null)
                .type(
                        TournamentMatchType.MAIN
                )
                .build();
    }

    private boolean determineSwissWhitePlayer(
            TournamentParticipant first,
            TournamentParticipant second,
            List<TournamentMatch> matchHistory,
            int roundNumber,
            int boardNumber
    ) {

        SwissColor firstPrevious =
                getColorInPreviousRound(
                        first,
                        matchHistory,
                        roundNumber
                );

        SwissColor secondPrevious =
                getColorInPreviousRound(
                        second,
                        matchHistory,
                        roundNumber
                );

        /*
         * Both had opposite colors.
         * Reverse both colors.
         */
        if (firstPrevious
                == SwissColor.WHITE
                &&
                secondPrevious
                        == SwissColor.BLACK) {

            return false;
        }

        if (firstPrevious
                == SwissColor.BLACK
                &&
                secondPrevious
                        == SwissColor.WHITE) {

            return true;
        }

        /*
         * first played last round,
         * second had no color (e.g. BYE).
         */
        if (firstPrevious
                == SwissColor.WHITE
                &&
                secondPrevious
                        == SwissColor.NONE) {

            return false;
        }

        if (firstPrevious
                == SwissColor.BLACK
                &&
                secondPrevious
                        == SwissColor.NONE) {

            return true;
        }

        /*
         * second played last round,
         * first had no color.
         */
        if (firstPrevious
                == SwissColor.NONE
                &&
                secondPrevious
                        == SwissColor.WHITE) {

            return true;
        }

        if (firstPrevious
                == SwissColor.NONE
                &&
                secondPrevious
                        == SwissColor.BLACK) {

            return false;
        }

        /*
         * Same previous color or neither player
         * had a previous color.
         *
         * Deterministic fallback.
         */
        return (roundNumber + boardNumber)
                % 2 == 0;
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

        /*
         * Pairing service only schedules the BYE.
         *
         * TournamentService awards the points when
         * this round becomes current.
         */
        return TournamentMatch.builder()
                .tournament(
                        tournament
                )
                .whiteParticipant(
                        participant
                )
                .blackParticipant(null)
                .roundNumber(
                        roundNumber
                )
                .boardNumber(
                        boardNumber
                )
                .status(
                        TournamentMatchStatus.PENDING
                )
                .termination(null)
                .whiteScore(null)
                .blackScore(null)
                .type(
                        TournamentMatchType.MAIN
                )
                .build();
    }

    private TournamentMatch createMatch(
            Tournament tournament,
            TournamentParticipant white,
            TournamentParticipant black,
            int round,
            int board
    ) {
        return createMatch(
                tournament,
                white,
                black,
                round,
                board,
                TournamentMatchType.MAIN
        );
    }

    private TournamentMatch createMatch(
            Tournament tournament,
            TournamentParticipant white,
            TournamentParticipant black,
            int round,
            int board,
            TournamentMatchType type
    ) {
        TournamentMatch match = new TournamentMatch();

        match.setTournament(tournament);
        match.setWhiteParticipant(white);
        match.setBlackParticipant(black);
        match.setRoundNumber(round);
        match.setBoardNumber(board);
        match.setType(type);
        match.setStatus(TournamentMatchStatus.PENDING);

        return match;
    }

    private TournamentParticipant getSingleEliminationLoser(
            TournamentMatch match
    ) {
        if (match.getStatus()
                != TournamentMatchStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Cannot determine loser of an incomplete match."
            );
        }

        if (match.getBlackParticipant() == null) {
            return null;
        }

        Double whiteScore = match.getWhiteScore();
        Double blackScore = match.getBlackScore();

        if (whiteScore == null || blackScore == null) {
            throw new IllegalStateException(
                    "Completed knockout match must have scores."
            );
        }

        if (whiteScore > blackScore) {
            return match.getBlackParticipant();
        }

        if (blackScore > whiteScore) {
            return match.getWhiteParticipant();
        }

        throw new IllegalStateException(
                "A knockout match cannot advance after a draw."
        );
    }

    // =========================================================
    // COMMON HELPERS
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
                                participant.getSeed()
                                        == null
                        );

        if (missingSeed) {
            throw new IllegalStateException(
                    "All participants must have a seed before pairings are created."
            );
        }
    }

    private double getScore(
            TournamentParticipant participant
    ) {

        return participant.getScore()
                == null
                ? 0.0
                : participant.getScore();
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

        /*
         * Preferred comparison once entities
         * have been persisted.
         */
        if (first.getId() != null
                && second.getId() != null) {

            return first.getId()
                    .equals(
                            second.getId()
                    );
        }

        /*
         * Useful both as fallback and in tests.
         */
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
    // SINGLE ELIMINATION HELPERS
    // =========================================================

    private List<TournamentParticipant>
    prepareSeededParticipants(
            List<TournamentParticipant> participants
    ) {

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least two active participants are required to create a round."
            );
        }

        List<TournamentParticipant> sorted =
                new ArrayList<>(
                        participants
                );

        validateSeeds(
                sorted
        );

        sorted.sort(
                Comparator.comparing(
                        TournamentParticipant::getSeed
                )
        );

        return sorted;
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

        while (currentSize
                < bracketSize) {

            int nextSize =
                    currentSize * 2;

            List<Integer> expanded =
                    new ArrayList<>();

            for (Integer seed : seeds) {

                expanded.add(
                        seed
                );

                expanded.add(
                        nextSize
                                + 1
                                - seed
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
                        participant.getSeed()
                                == seed
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
        if (match.getBlackParticipant()
                == null) {

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

        /*
         * A drawn chess game does not resolve
         * a knockout matchup.
         */
        throw new IllegalStateException(
                "A knockout match must have a winner before the next round can be created."
        );
    }

    // =========================================================
    // SWISS INTERNAL TYPES
    // =========================================================

    private enum SwissHalf {
        UPPER,
        LOWER
    }

    private enum SwissColor {
        WHITE,
        BLACK,
        NONE
    }

    private record SwissPlayer(
            TournamentParticipant participant,
            double score,
            int groupIndex,
            SwissHalf half
    ) {
    }

    private record SwissPair(
            SwissPlayer first,
            SwissPlayer second
    ) {
    }
}