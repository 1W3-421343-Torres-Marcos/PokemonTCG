package com.pokemontcg.deck.dto;

import java.time.Instant;
import java.util.UUID;

public record DeckSummaryDTO(
        UUID id,
        String name,
        String description,
        boolean starter,
        boolean cpuEligible,
        boolean valid,
        int totalCards,
        Instant createdAt,
        Instant updatedAt
) {
}
