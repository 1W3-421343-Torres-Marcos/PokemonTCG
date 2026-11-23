package com.pokemontcg.auth.service;

import com.pokemontcg.auth.domain.UserEntity;
import com.pokemontcg.auth.dto.AuthResponseDTO;
import com.pokemontcg.auth.dto.LoginRequest;
import com.pokemontcg.auth.dto.RegisterRequest;
import com.pokemontcg.auth.repository.UserRepository;
import com.pokemontcg.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "pokemontcg.catalog.startup-sync=false")
@Transactional
class UserStatsAndMigrationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerEnforcesCaseInsensitiveUniquenessInServiceAndPostgresIndex() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "Trainer_" + suffix;
        String email = "Trainer." + suffix + "@Example.COM";

        AuthResponseDTO registered = authService.register(new RegisterRequest(username, email, "secret123"));
        assertThat(registered.user().email()).isEqualTo(email.toLowerCase());
        assertThat(registered.token()).isNotBlank();

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest(username.toLowerCase(), "other." + suffix + "@example.com", "secret123")))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Other_" + suffix, email.toLowerCase(), "secret123")))
                .isInstanceOf(ConflictException.class);

        UserEntity duplicate = new UserEntity();
        duplicate.setId(UUID.randomUUID());
        duplicate.setUsername(username.toUpperCase());
        duplicate.setEmail("unique." + suffix + "@example.com");
        duplicate.setPasswordHash("hash");
        duplicate.setRole("USER");
        duplicate.setCreatedAt(Instant.now());
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void loginFavoritesAndMatchStatsPersistInPostgres() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "StatsUser_" + suffix;
        String email = "Stats." + suffix + "@Example.COM";

        AuthResponseDTO registered = authService.register(new RegisterRequest(username, email, "secret123"));

        AuthResponseDTO loggedIn = authService.login(new LoginRequest(email.toUpperCase(), "secret123"));
        assertThat(loggedIn.user().id()).isEqualTo(registered.user().id());

        var withFavorite = userService.addFavorite(registered.user().id(), "base1-4");
        assertThat(withFavorite.favoriteCardIds()).containsExactly("base1-4");

        userStatsService.recordMatchResult(registered.user().id(), true);
        userStatsService.recordMatchResult(registered.user().id(), false);

        var updatedProfile = userService.getProfile(registered.user().id());
        assertThat(updatedProfile.matchesPlayed()).isEqualTo(2);
        assertThat(updatedProfile.matchesWon()).isEqualTo(1);
    }
}
