package com.example.chessforge.controller.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginControllerTest {

    private final LoginController controller =
            new LoginController();

    @Test
    void loginShouldReturnLoginView() {

        assertEquals(
                "login",
                controller.login()
        );
    }
}