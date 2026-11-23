package com.pokemontcg.auth.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-pokemon-tcg-at-least-32-bytes";

    @Test
    void generateAndParseValidToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "ash_ketchum", "USER");
        Optional<AuthenticatedUser> parsed = jwtService.parseToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(userId);
        assertThat(parsed.get().username()).isEqualTo("ash_ketchum");
        assertThat(parsed.get().role()).isEqualTo("USER");
    }

    @Test
    void parseReturnsEmptyOnTamperedOrExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(UUID.randomUUID(), "misty", "USER");

        assertThat(jwtService.parseToken(token + "tampered")).isEmpty();

        JwtService expiredJwtService = new JwtService(SECRET, -1_000);
        String expiredToken = expiredJwtService.generateToken(UUID.randomUUID(), "brock", "USER");
        assertThat(jwtService.parseToken(expiredToken)).isEmpty();
    }
}
