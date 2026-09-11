package com.example.chessforge.repository.tournament;

import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.tournament.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long> {

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByCreator(User creator);

}