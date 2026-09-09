package com.example.chessforge.repository;

import com.example.chessforge.model.entity.Tournament;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.TournamentParticipantStatus;
import com.example.chessforge.model.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long> {

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByCreator(User creator);

}