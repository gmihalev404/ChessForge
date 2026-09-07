package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.TournamentParticipant;
import com.example.chessforge.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository
        extends JpaRepository<TournamentParticipant, Long> {

    List<TournamentParticipant> findByTournament(Tournament tournament);

    List<TournamentParticipant> findByUser(User user);

    Optional<TournamentParticipant> findByTournamentAndUser(
            Tournament tournament,
            User user
    );

    boolean existsByTournamentAndUser(
            Tournament tournament,
            User user
    );
}