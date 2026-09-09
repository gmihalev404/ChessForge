package com.example.chessforge.model.entity;

import com.example.chessforge.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeControl timeControl;

    @Column(nullable = false)
    private boolean rated;

    private Integer maxPlayers;

    @Column(nullable = false)
    private Double byePoints;

    @Column(nullable = false)
    private boolean armageddonForFirstPlaceTie;

    @ElementCollection
    @CollectionTable(
            name = "tournament_tiebreaks",
            joinColumns = @JoinColumn(name = "tournament_id")
    )
    @Enumerated(EnumType.STRING)
    @OrderColumn(name = "priority")
    @Column(name = "tiebreak_type", nullable = false)
    private List<TieBreakType> tieBreaks;

    @Column(nullable = false)
    private Integer numberOfRounds;

    @Column(nullable = false)
    private Integer currentRound;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startsAt;

    private LocalDateTime finishedAt;

    @PrePersist
    private void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = TournamentStatus.REGISTRATION;
        }

        if (byePoints == null) {
            byePoints = 1.0;
        }

        if (tieBreaks == null) {
            tieBreaks = new ArrayList<>();
        }
    }
}