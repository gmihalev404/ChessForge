package com.example.chessforge.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HomeControllerTest {

    private HomeController controller;
    private Model model;

    @BeforeEach
    void setUp() {

        controller =
                new HomeController();

        model =
                mock(Model.class);
    }

    @Test
    void homeShouldAddAuthenticatedUsername() {

        Principal principal =
                () -> "testplayer";

        String result =
                controller.home(
                        principal,
                        model
                );

        assertEquals(
                "home",
                result
        );

        verify(model)
                .addAttribute(
                        "username",
                        "testplayer"
                );
    }

    @Test
    void homeShouldNotAddUsernameForAnonymousUser() {

        String result =
                controller.home(
                        null,
                        model
                );

        assertEquals(
                "home",
                result
        );

        verifyNoInteractions(
                model
        );
    }
}