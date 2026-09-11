package com.example.chessforge.model.entity.tournament;

import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.enums.tournament.TournamentMatchStatus;
import com.example.chessforge.model.enums.tournament.TournamentMatchTermination;
import com.example.chessforge.model.enums.tournament.TournamentMatchType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tournament_matches",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "tournament_id",
                        "round_number",
                        "board_number"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "white_participant_id", nullable = false)
    private TournamentParticipant whiteParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_participant_id")
    private TournamentParticipant blackParticipant;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private Integer boardNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentMatchStatus status;

    @Enumerated(EnumType.STRING)
    private TournamentMatchTermination termination;

    private Double whiteScore;

    private Double blackScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentMatchType type;

    @PrePersist
    private void onCreate() {
        if (status == null) {
            status = TournamentMatchStatus.PENDING;
        }

        if (type == null) {
            type = TournamentMatchType.MAIN;
        }
    }
}