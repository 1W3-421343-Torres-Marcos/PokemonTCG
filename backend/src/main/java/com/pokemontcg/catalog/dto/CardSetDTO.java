package com.pokemontcg.catalog.dto;

import java.time.Instant;
import java.time.LocalDate;

public record CardSetDTO(
        String id,
        String name,
        String series,
        LocalDate releaseDate,
        Integer totalCards,
        String logoUrl,
        Instant syncedAt
) {
}
