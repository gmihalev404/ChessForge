package com.example.chessforge.controller.user;

import com.example.chessforge.controller.user.dto.RegisterRequest;
import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterPage(
            Model model
    ) {

        model.addAttribute(
                "registerRequest",
                new RegisterRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute RegisterRequest request,
            Model model
    ) {

        if (!passwordsMatch(request)) {

            model.addAttribute(
                    "error",
                    "Passwords do not match."
            );

            return "register";
        }

        try {

            userService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );

            return "register";
        }

        return "redirect:/login";
    }

    private boolean passwordsMatch(
            RegisterRequest request
    ) {

        if (request.getPassword() == null) {
            return request.getConfirmPassword() == null;
        }

        return request.getPassword()
                .equals(
                        request.getConfirmPassword()
                );
    }
}