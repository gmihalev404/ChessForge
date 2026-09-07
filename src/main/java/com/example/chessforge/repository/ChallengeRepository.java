package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Challenge;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository
        extends JpaRepository<Challenge, Long> {

    List<Challenge> findByOpponentAndStatus(
            User opponent,
            ChallengeStatus status
    );

    List<Challenge> findByChallengerAndStatus(
            User challenger,
            ChallengeStatus status
    );

    boolean existsByChallengerAndOpponentAndStatus(
            User challenger,
            User opponent,
            ChallengeStatus status
    );

    List<Challenge> findByStatusAndExpiresAtBefore(
            ChallengeStatus status,
            LocalDateTime time
    );
}