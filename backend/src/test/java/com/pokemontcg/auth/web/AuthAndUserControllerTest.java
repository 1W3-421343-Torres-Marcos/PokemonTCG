package com.pokemontcg.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.auth.dto.AuthResponseDTO;
import com.pokemontcg.auth.dto.LoginRequest;
import com.pokemontcg.auth.dto.RegisterRequest;
import com.pokemontcg.auth.dto.UserProfileDTO;
import com.pokemontcg.auth.security.JwtAuthenticationFilter;
import com.pokemontcg.auth.security.JwtService;
import com.pokemontcg.auth.security.SecurityConfig;
import com.pokemontcg.auth.service.AuthService;
import com.pokemontcg.auth.service.UserService;
import com.pokemontcg.common.exception.ConflictException;
import com.pokemontcg.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "pokemontcg.jwt.secret=test-secret-key-for-pokemon-tcg-at-least-32-bytes",
        "pokemontcg.jwt.expiration-ms=3600000"
})
class AuthAndUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @Test
    void registerReturns201AndConflictReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileDTO profile = new UserProfileDTO(userId, "Ash", "ash@pallet.town", "USER", 0, 0, List.of(), Instant.now());
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AuthResponseDTO("jwt-token", profile));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Ash", "ash@pallet.town", "pikachu123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.username").value("Ash"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        when(authService.register(any(RegisterRequest.class))).thenThrow(new ConflictException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Ash", "ASH@pallet.town", "pikachu123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void loginReturnsUniform401OnInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ash", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void protectedUserEndpointsRequireValidJwtAndManageFavorites() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "misty", "USER");
        UserProfileDTO profile = new UserProfileDTO(userId, "misty", "misty@cerulean.gym", "USER", 3, 2, List.of("base1-2"), Instant.now());
        when(userService.getProfile(userId)).thenReturn(profile);
        when(userService.addFavorite(userId, "base1-2")).thenReturn(profile);
        when(userService.removeFavorite(userId, "base1-2")).thenReturn(
                new UserProfileDTO(userId, "misty", "misty@cerulean.gym", "USER", 3, 2, List.of(), Instant.now())
        );

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("misty"))
                .andExpect(jsonPath("$.matchesWon").value(2));

        mockMvc.perform(put("/api/users/me/favorites/base1-2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteCardIds[0]").value("base1-2"));

        mockMvc.perform(delete("/api/users/me/favorites/base1-2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteCardIds").isEmpty());
    }
}
