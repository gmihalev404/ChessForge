package com.example.chessforge.model.entity;

import com.example.chessforge.model.enums.TournamentParticipantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tournament_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"tournament_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentParticipant extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double score;

    private Integer seed;

    private Integer finalRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentParticipantStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    private void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }

        if (score == null) {
            score = 0.0;
        }

        if (status == null) {
            status = TournamentParticipantStatus.ACTIVE;
        }
    }
}