package com.example.chessforge.service;

import com.example.chessforge.model.entity.Challenge;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.ChallengeStatus;
import com.example.chessforge.model.enums.ColorPreference;
import com.example.chessforge.model.enums.TimeControl;
import com.example.chessforge.model.enums.UserStatus;
import com.example.chessforge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private static final long CHALLENGE_EXPIRATION_HOURS = 24;

    private final ChallengeRepository challengeRepository;

    @Transactional
    public Challenge sendChallenge(
            User challenger,
            User opponent,
            TimeControl timeControl,
            ColorPreference colorPreference,
            boolean rated
    ) {

        validateUsers(challenger, opponent);

        if (hasPendingChallenge(challenger, opponent)) {
            throw new IllegalStateException(
                    "A pending challenge already exists between these users."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        Challenge challenge = Challenge.builder()
                .challenger(challenger)
                .opponent(opponent)
                .timeControl(timeControl)
                .colorPreference(colorPreference)
                .rated(rated)
                .expiresAt(now.plusHours(CHALLENGE_EXPIRATION_HOURS))
                .build();

        return challengeRepository.save(challenge);
    }

    @Transactional
    public void acceptChallenge(
            Challenge challenge,
            User opponent
    ) {

        validatePendingChallenge(challenge);

        if (!challenge.getOpponent().getId().equals(opponent.getId())) {
            throw new IllegalStateException(
                    "Only the opponent can accept this challenge."
            );
        }

        if (isExpired(challenge)) {
            throw new IllegalStateException(
                    "This challenge has expired."
            );
        }

        challenge.setStatus(ChallengeStatus.ACCEPTED);
        //todo: starts a game
    }

    @Transactional
    public void declineChallenge(
            Challenge challenge,
            User opponent
    ) {

        validatePendingChallenge(challenge);

        if (!challenge.getOpponent().getId().equals(opponent.getId())) {
            throw new IllegalStateException(
                    "Only the opponent can decline this challenge."
            );
        }

        challenge.setStatus(ChallengeStatus.DECLINED);
    }

    @Transactional
    public void cancelChallenge(
            Challenge challenge,
            User challenger
    ) {

        validatePendingChallenge(challenge);

        if (!challenge.getChallenger().getId().equals(challenger.getId())) {
            throw new IllegalStateException(
                    "Only the challenger can cancel this challenge."
            );
        }

        challenge.setStatus(ChallengeStatus.CANCELLED);
    }

    @Transactional
    public void expireChallenges() {

        List<Challenge> expiredChallenges =
                challengeRepository.findByStatusAndExpiresAtBefore(
                        ChallengeStatus.PENDING,
                        LocalDateTime.now()
                );

        expiredChallenges.forEach(
                challenge ->
                        challenge.setStatus(ChallengeStatus.EXPIRED)
        );
    }

    public List<Challenge> getIncomingPendingChallenges(User user) {
        return challengeRepository
                .findByOpponentAndStatusAndExpiresAtAfter(
                        user,
                        ChallengeStatus.PENDING,
                        LocalDateTime.now()
                );
    }

    public List<Challenge> getOutgoingPendingChallenges(User user) {
        return challengeRepository
                .findByChallengerAndStatusAndExpiresAtAfter(
                        user,
                        ChallengeStatus.PENDING,
                        LocalDateTime.now()
                );
    }

    private void validateUsers(
            User challenger,
            User opponent
    ) {

        if (challenger.getId().equals(opponent.getId())) {
            throw new IllegalArgumentException(
                    "You cannot challenge yourself."
            );
        }

        if (challenger.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "The challenger account is not active."
            );
        }

        if (opponent.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "The opponent account is not active."
            );
        }
    }

    private boolean hasPendingChallenge(
            User userA,
            User userB
    ) {

        LocalDateTime now = LocalDateTime.now();

        return challengeRepository
                .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                        userA,
                        userB,
                        ChallengeStatus.PENDING,
                        now
                )
                ||
                challengeRepository
                        .existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
                                userB,
                                userA,
                                ChallengeStatus.PENDING,
                                now
                        );
    }
    private void validatePendingChallenge(Challenge challenge) {

        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new IllegalStateException(
                    "Challenge is no longer pending."
            );
        }
    }

    private boolean isExpired(Challenge challenge) {
        return challenge.getExpiresAt()
                .isBefore(LocalDateTime.now());
    }
}