package com.example.chessforge.repository.game;

import com.example.chessforge.model.entity.game.GameMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameMoveRepository
        extends JpaRepository<GameMove, Long> {

    List<GameMove> findByGameIdOrderByPlyNumberAsc(
            Long gameId
    );
}