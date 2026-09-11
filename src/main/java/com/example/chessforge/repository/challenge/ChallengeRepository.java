package com.example.chessforge.repository.challenge;

import com.example.chessforge.model.entity.challenge.Challenge;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.challenge.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByOpponentAndStatusAndExpiresAtAfter(
            User opponent,
            ChallengeStatus status,
            LocalDateTime time
    );

    List<Challenge> findByChallengerAndStatusAndExpiresAtAfter(
            User challenger,
            ChallengeStatus status,
            LocalDateTime time
    );

    boolean existsByChallengerAndOpponentAndStatusAndExpiresAtAfter(
            User challenger,
            User opponent,
            ChallengeStatus status,
            LocalDateTime time
    );

    List<Challenge> findByStatusAndExpiresAtBefore(
            ChallengeStatus status,
            LocalDateTime time
    );
}