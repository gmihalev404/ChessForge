package com.example.chessforge.repository.game;

import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.game.GameStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameRepository
        extends JpaRepository<Game, Long> {

    List<Game> findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
            User whitePlayer,
            User blackPlayer
    );

    List<Game> findByTournamentMatchOrderById(
            TournamentMatch tournamentMatch
    );

    List<Game> findByStatus(
            GameStatus status
    );

    List<Game> findByWhitePlayerAndStatus(
            User whitePlayer,
            GameStatus status
    );

    List<Game> findByBlackPlayerAndStatus(
            User blackPlayer,
            GameStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT g
            FROM Game g
            WHERE g.id = :id
            """)
    Optional<Game> findByIdForUpdate(
            @Param("id") Long id
    );

    @Query("""
            SELECT CASE WHEN COUNT(g) > 0
                        THEN true
                        ELSE false
                   END
            FROM Game g
            WHERE (g.whitePlayer = :player
                   OR g.blackPlayer = :player)
              AND g.status = :status
            """)
    boolean existsByPlayerAndStatus(
            @Param("player") User player,
            @Param("status") GameStatus status
    );

    @Query("""
        SELECT g.id
        FROM Game g
        WHERE g.status = :status
          AND g.turnExpiresAt IS NOT NULL
          AND g.turnExpiresAt <= :now
        ORDER BY g.turnExpiresAt ASC
        """)
    List<Long> findExpiredGameIds(
            @Param("status") GameStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}