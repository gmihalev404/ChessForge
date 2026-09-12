package com.example.chessforge.controller.user;

import com.example.chessforge.controller.user.dto.RegisterRequest;
import com.example.chessforge.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    private RegistrationController controller;

    @BeforeEach
    void setUp() {

        controller =
                new RegistrationController(
                        userService
                );
    }

    @Test
    void showRegisterPageShouldReturnRegisterView() {

        String result =
                controller.showRegisterPage(
                        model
                );

        assertEquals(
                "register",
                result
        );

        verify(model)
                .addAttribute(
                        eq("registerRequest"),
                        any(RegisterRequest.class)
                );
    }

    @Test
    void registerShouldCreateUserAndRedirectToLogin() {

        RegisterRequest request =
                createRequest();

        String result =
                controller.register(
                        request,
                        model
                );

        assertEquals(
                "redirect:/login",
                result
        );

        verify(userService)
                .register(
                        "georgi",
                        "georgi@test.com",
                        "secret123"
                );
    }

    @Test
    void registerShouldRejectDifferentPasswords() {

        RegisterRequest request =
                createRequest();

        request.setConfirmPassword(
                "different"
        );

        String result =
                controller.register(
                        request,
                        model
                );

        assertEquals(
                "register",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "Passwords do not match."
                );

        verifyNoInteractions(
                userService
        );
    }

    @Test
    void registerShouldReturnRegisterPageWhenServiceRejectsUser() {

        RegisterRequest request =
                createRequest();

        doThrow(
                new IllegalStateException(
                        "Username is already taken."
                )
        ).when(userService)
                .register(
                        "georgi",
                        "georgi@test.com",
                        "secret123"
                );

        String result =
                controller.register(
                        request,
                        model
                );

        assertEquals(
                "register",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "Username is already taken."
                );
    }

    private RegisterRequest createRequest() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername(
                "georgi"
        );

        request.setEmail(
                "georgi@test.com"
        );

        request.setPassword(
                "secret123"
        );

        request.setConfirmPassword(
                "secret123"
        );

        return request;
    }
}