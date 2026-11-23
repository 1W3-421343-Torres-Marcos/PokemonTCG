package com.pokemontcg.auth.service;

import com.pokemontcg.auth.domain.UserEntity;
import com.pokemontcg.auth.dto.AuthResponseDTO;
import com.pokemontcg.auth.dto.LoginRequest;
import com.pokemontcg.auth.dto.RegisterRequest;
import com.pokemontcg.auth.dto.UserProfileDTO;
import com.pokemontcg.auth.repository.UserFavoriteCardRepository;
import com.pokemontcg.auth.repository.UserRepository;
import com.pokemontcg.auth.security.JwtService;
import com.pokemontcg.common.exception.ConflictException;
import com.pokemontcg.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserFavoriteCardRepository favoriteCardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            UserFavoriteCardRepository favoriteCardRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.favoriteCardRepository = favoriteCardRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO register(RegisterRequest request) {
        String normalizedUsername = request.username().trim();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("Username is already registered");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        user.setMatchesPlayed(0);
        user.setMatchesWon(0);
        user.setCreatedAt(Instant.now());

        UserEntity saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getUsername(), saved.getRole());
        return new AuthResponseDTO(token, toProfileDto(saved, List.of()));
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequest request) {
        String identifier = request.identifier().trim();
        UserEntity user = userRepository.findByEmailIgnoreCase(identifier.toLowerCase(Locale.ROOT))
                .or(() -> userRepository.findByUsernameIgnoreCase(identifier))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        List<String> favorites = favoriteCardRepository.findCardIdsByUserId(user.getId());
        return new AuthResponseDTO(token, toProfileDto(user, favorites));
    }

    static UserProfileDTO toProfileDto(UserEntity user, List<String> favoriteCardIds) {
        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getMatchesPlayed(),
                user.getMatchesWon(),
                favoriteCardIds,
                user.getCreatedAt()
        );
    }
}
