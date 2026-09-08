package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Challenge;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.ChallengeStatus;
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