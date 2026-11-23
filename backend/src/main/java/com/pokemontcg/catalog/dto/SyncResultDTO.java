package com.pokemontcg.catalog.dto;

import java.time.Instant;

public record SyncResultDTO(
        String setId,
        String language,
        int syncedCards,
        Instant syncedAt
) {
}
