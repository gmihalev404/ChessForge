package com.example.chessforge.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tournament_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "white_participant_id", nullable = false)
    private TournamentParticipant whiteParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_participant_id")
    private TournamentParticipant blackParticipant;
    //A match with no black participant represents a bye.

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(nullable = false)
    private Integer roundNumber;
}
