package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Game;
import com.example.chessforge.model.entity.TournamentMatch;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
            User whitePlayer,
            User blackPlayer
    );

    List<Game> findByTournamentMatchOrderById(
            TournamentMatch tournamentMatch
    );

    List<Game> findByStatus(GameStatus status);

    List<Game> findByWhitePlayerAndStatus(
            User whitePlayer,
            GameStatus status
    );

    List<Game> findByBlackPlayerAndStatus(
            User blackPlayer,
            GameStatus status
    );

    @Query("""
    SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END
    FROM Game g
    WHERE (g.whitePlayer = :player OR g.blackPlayer = :player)
      AND g.status = :status
""")
    boolean existsByPlayerAndStatus(
            @Param("player") User player,
            @Param("status") GameStatus status
    );
}