package com.pokemontcg.auth.web;

import com.pokemontcg.auth.dto.UserProfileDTO;
import com.pokemontcg.auth.security.AuthenticatedUser;
import com.pokemontcg.auth.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileDTO getMe(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getProfile(currentUser.userId());
    }

    @PutMapping("/me/favorites/{cardId}")
    public UserProfileDTO addFavorite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String cardId
    ) {
        return userService.addFavorite(currentUser.userId(), cardId);
    }

    @DeleteMapping("/me/favorites/{cardId}")
    public UserProfileDTO removeFavorite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String cardId
    ) {
        return userService.removeFavorite(currentUser.userId(), cardId);
    }
}
