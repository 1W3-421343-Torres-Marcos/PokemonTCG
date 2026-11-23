package com.pokemontcg.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String username,
        String email,
        String role,
        int matchesPlayed,
        int matchesWon,
        List<String> favoriteCardIds,
        Instant createdAt
) {
}
