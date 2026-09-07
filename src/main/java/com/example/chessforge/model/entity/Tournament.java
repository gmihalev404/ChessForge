package com.example.chessforge.model.entity;

import com.example.chessforge.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament extends BaseEntity{

    @Column(nullable = false)
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
    private LocalDateTime createdAt;

    private LocalDateTime startsAt;

    private LocalDateTime finishedAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = TournamentStatus.REGISTRATION;
        }
    }
}