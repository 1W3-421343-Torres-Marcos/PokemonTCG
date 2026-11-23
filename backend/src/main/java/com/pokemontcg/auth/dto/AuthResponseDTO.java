package com.pokemontcg.auth.dto;

public record AuthResponseDTO(
        String token,
        UserProfileDTO user
) {
}
