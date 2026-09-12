package com.example.chessforge.model.entity.game;

import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.game.GameResult;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.model.enums.game.GameTermination;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "games",
        indexes = {
                @Index(
                        name = "idx_games_status_turn_expires_at",
                        columnList = "status, turn_expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "white_player_id", nullable = false)
    private User whitePlayer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "black_player_id", nullable = false)
    private User blackPlayer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeControl timeControl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    private GameResult result;

    @Column(nullable = false)
    private boolean rated;

    private Integer whiteRatingBefore;
    private Integer blackRatingBefore;

    private Integer whiteRatingAfter;
    private Integer blackRatingAfter;

    @Column(nullable = false)
    private Long whiteTimeRemainingMillis;

    @Column(nullable = false)
    private Long blackTimeRemainingMillis;

    @Lob
    private String pgn;

    @Column(length = 100)
    private String currentFen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_match_id")
    private TournamentMatch tournamentMatch;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    private GameTermination termination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_offer_by_id")
    private User drawOfferBy;

    @Column(name = "turn_started_at")
    private LocalDateTime turnStartedAt;

    @Column(name = "turn_expires_at")
    private LocalDateTime turnExpiresAt;
}
