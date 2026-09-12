package com.example.chessforge.controller.game;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final UserService userService;

    @GetMapping("/games")
    public String games(
            Principal principal,
            Model model
    ) {

        User currentUser =
                userService.findByUsername(
                        principal.getName()
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found."
                        )
                );

        model.addAttribute(
                "games",
                gameService.getGameSummariesForUser(
                        currentUser
                )
        );

        model.addAttribute(
                "username",
                currentUser.getUsername()
        );

        return "game/games";
    }
}