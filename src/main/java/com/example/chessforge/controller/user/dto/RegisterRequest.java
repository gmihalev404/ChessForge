package com.example.chessforge.controller.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    private String username;
    private String email;
    private String password;
    private String confirmPassword;
}