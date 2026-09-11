package com.example.chessforge.repository.tournament;

import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.tournament.TournamentParticipantStatus;
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

    long countByTournamentAndStatus(
            Tournament tournament,
            TournamentParticipantStatus status
    );
}