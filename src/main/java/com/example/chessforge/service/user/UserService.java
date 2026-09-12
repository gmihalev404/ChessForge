package com.example.chessforge.service.user;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    public User register(
            String username,
            String email,
            String rawPassword
    ) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email cannot be empty."
            );
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        if (username.length() > 30) {
            throw new IllegalArgumentException(
                    "Username cannot exceed 30 characters."
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException(
                    "Username is already taken."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Email is already registered."
            );
        }

        User user =
                User.builder()
                        .username(username)
                        .email(email)
                        .password(
                                passwordEncoder.encode(
                                        rawPassword
                                )
                        )
                        .build();

        return userRepository.save(
                user
        );
    }
}