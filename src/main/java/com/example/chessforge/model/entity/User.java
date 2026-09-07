package com.example.chessforge.model.entity;

import com.example.chessforge.model.enums.Role;
import com.example.chessforge.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity{

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Integer bulletRating;

    @Column(nullable = false)
    private Integer blitzRating;

    @Column(nullable = false)
    private Integer rapidRating;

    @Column(nullable = false)
    private Integer classicalRating;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (role == null) {
            role = Role.USER;
        }

        if (bulletRating == null) {
            bulletRating = 400;
        }

        if (blitzRating == null) {
            blitzRating = 400;
        }

        if (rapidRating == null) {
            rapidRating = 400;
        }

        if (classicalRating == null) {
            classicalRating = 400;
        }

        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }
}
