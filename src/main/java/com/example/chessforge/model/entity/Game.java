package com.example.chessforge.model.entity;

import com.example.chessforge.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game extends BaseEntity{

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
    private Boolean rated;

    private Integer whiteRatingBefore;
    private Integer blackRatingBefore;

    private Integer whiteRatingAfter;
    private Integer blackRatingAfter;

    private Integer whiteTimeRemaining;
    private Integer blackTimeRemaining;

    @Lob
    private String pgn;

    @Column(length = 100)
    private String currentFen;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    private GameTermination termination;
}
