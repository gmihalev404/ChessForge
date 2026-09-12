package com.example.chessforge.service.user;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {

        userService =
                new UserService(
                        userRepository,
                        passwordEncoder
                );
    }

    @Test
    void registerShouldCreateUserWithEncodedPassword() {

        when(userRepository.existsByUsername(
                "georgi"
        )).thenReturn(
                false
        );

        when(userRepository.existsByEmail(
                "georgi@test.com"
        )).thenReturn(
                false
        );

        when(passwordEncoder.encode(
                "secret123"
        )).thenReturn(
                "encoded-password"
        );

        when(userRepository.save(
                any(User.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        User result =
                userService.register(
                        "georgi",
                        "georgi@test.com",
                        "secret123"
                );

        assertEquals(
                "georgi",
                result.getUsername()
        );

        assertEquals(
                "georgi@test.com",
                result.getEmail()
        );

        assertEquals(
                "encoded-password",
                result.getPassword()
        );

        verify(passwordEncoder)
                .encode(
                        "secret123"
                );

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(
                        User.class
                );

        verify(userRepository)
                .save(
                        captor.capture()
                );

        assertEquals(
                "encoded-password",
                captor.getValue()
                        .getPassword()
        );
    }

    @Test
    void registerShouldRejectExistingUsername() {

        when(userRepository.existsByUsername(
                "georgi"
        )).thenReturn(
                true
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        userService.register(
                                "georgi",
                                "new@test.com",
                                "secret123"
                        )
        );

        verify(userRepository, never())
                .save(
                        any()
                );

        verifyNoInteractions(
                passwordEncoder
        );
    }

    @Test
    void registerShouldRejectExistingEmail() {

        when(userRepository.existsByUsername(
                "georgi"
        )).thenReturn(
                false
        );

        when(userRepository.existsByEmail(
                "georgi@test.com"
        )).thenReturn(
                true
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        userService.register(
                                "georgi",
                                "georgi@test.com",
                                "secret123"
                        )
        );

        verify(userRepository, never())
                .save(
                        any()
                );

        verifyNoInteractions(
                passwordEncoder
        );
    }

    @Test
    void registerShouldRejectEmptyUsername() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        userService.register(
                                " ",
                                "georgi@test.com",
                                "secret123"
                        )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registerShouldRejectEmptyEmail() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        userService.register(
                                "georgi",
                                " ",
                                "secret123"
                        )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registerShouldRejectEmptyPassword() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        userService.register(
                                "georgi",
                                "georgi@test.com",
                                " "
                        )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registerShouldRejectUsernameLongerThanThirtyCharacters() {

        String username =
                "a".repeat(
                        31
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        userService.register(
                                username,
                                "georgi@test.com",
                                "secret123"
                        )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }
}