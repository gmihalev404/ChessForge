package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentMatchRepository
        extends JpaRepository<TournamentMatch, Long> {

    List<TournamentMatch> findByTournament(
            Tournament tournament
    );

    List<TournamentMatch> findByTournamentAndRoundNumberOrderByBoardNumber(
            Tournament tournament,
            Integer roundNumber
    );

    Optional<TournamentMatch>
    findByTournamentAndRoundNumberAndBoardNumber(
            Tournament tournament,
            Integer roundNumber,
            Integer boardNumber
    );
}