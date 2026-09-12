package com.example.chessforge.model.entity.game;

import com.example.chessforge.model.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "game_moves",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_move_ply",
                        columnNames = {
                                "game_id",
                                "ply_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_game_move_game",
                        columnList = "game_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameMove extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "game_id",
            nullable = false
    )
    private Game game;

    /**
     * Half-move number:
     *
     * 1 = White's first move
     * 2 = Black's first move
     * 3 = White's second move
     * ...
     */
    @Column(
            name = "ply_number",
            nullable = false
    )
    private Integer plyNumber;

    @Column(
            name = "from_square",
            nullable = false,
            length = 2
    )
    private String fromSquare;

    @Column(
            name = "to_square",
            nullable = false,
            length = 2
    )
    private String toSquare;

    /**
     * NORMAL
     * CASTLE_KING_SIDE
     * CASTLE_QUEEN_SIDE
     * EN_PASSANT
     * PROMOTION
     */
    @Column(
            name = "move_type",
            nullable = false,
            length = 32
    )
    private String moveType;

    /**
     * QUEEN / ROOK / BISHOP / KNIGHT
     *
     * Null for non-promotion moves.
     */
    @Column(
            name = "promotion_piece",
            length = 16
    )
    private String promotionPiece;

    /**
     * Complete position after this move.
     */
    @Column(
            name = "fen_after",
            nullable = false,
            length = 120
    )
    private String fenAfter;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(length = 16)
    private String san;

    @PrePersist
    private void prePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}