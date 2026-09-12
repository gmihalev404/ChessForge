package com.example.chessforge.service.user;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.user.UserStatus;
import com.example.chessforge.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChessForgeUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    private ChessForgeUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {

        userDetailsService =
                new ChessForgeUserDetailsService(
                        userRepository
                );
    }

    @Test
    void loadUserByUsernameShouldLoadActiveUser() {

        when(userRepository.findByUsername(
                "georgi"
        )).thenReturn(
                Optional.of(user)
        );

        when(user.getUsername())
                .thenReturn(
                        "georgi"
                );

        when(user.getPassword())
                .thenReturn(
                        "$2a$10$test"
                );

        when(user.getStatus())
                .thenReturn(
                        UserStatus.ACTIVE
                );

        /*
         * We do not need to mock role separately here.
         * This test focuses on identity/account state.
         */

        when(user.getRole())
                .thenReturn(
                        com.example.chessforge.model.enums.user.Role.USER
                );

        UserDetails result =
                userDetailsService.loadUserByUsername(
                        "georgi"
                );

        assertEquals(
                "georgi",
                result.getUsername()
        );

        assertEquals(
                "$2a$10$test",
                result.getPassword()
        );

        assertTrue(
                result.isEnabled()
        );

        assertEquals(
                "USER",
                result.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );

        verify(userRepository)
                .findByUsername(
                        "georgi"
                );
    }

    @Test
    void loadUserByUsernameShouldRejectMissingUser() {

        when(userRepository.findByUsername(
                "missing"
        )).thenReturn(
                Optional.empty()
        );

        assertThrows(
                UsernameNotFoundException.class,
                () ->
                        userDetailsService
                                .loadUserByUsername(
                                        "missing"
                                )
        );

        verify(userRepository)
                .findByUsername(
                        "missing"
                );
    }

    @Test
    void loadUserByUsernameShouldDisableInactiveUser() {

        when(userRepository.findByUsername(
                "georgi"
        )).thenReturn(
                Optional.of(user)
        );

        when(user.getUsername())
                .thenReturn(
                        "georgi"
                );

        when(user.getPassword())
                .thenReturn(
                        "$2a$10$test"
                );

        when(user.getStatus())
                .thenReturn(
                        UserStatus.SUSPENDED
                );

        when(user.getRole())
                .thenReturn(
                        com.example.chessforge.model.enums.user.Role.USER
                );

        UserDetails result =
                userDetailsService.loadUserByUsername(
                        "georgi"
                );

        assertFalse(
                result.isEnabled()
        );
    }
}