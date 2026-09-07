package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Game;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
            User whitePlayer,
            User blackPlayer
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
}