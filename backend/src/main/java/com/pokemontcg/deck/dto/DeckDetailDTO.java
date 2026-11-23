package com.pokemontcg.deck.dto;

import com.pokemontcg.deck.validator.DeckValidationResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeckDetailDTO(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        boolean starter,
        boolean cpuEligible,
        boolean valid,
        int totalCards,
        DeckValidationResult validation,
        List<DeckCardItemDTO> cards,
        Instant createdAt,
        Instant updatedAt
) {
}
