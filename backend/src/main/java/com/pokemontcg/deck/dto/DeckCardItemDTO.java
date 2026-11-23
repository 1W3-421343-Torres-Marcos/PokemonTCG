package com.pokemontcg.deck.dto;

import java.util.List;

public record DeckCardItemDTO(
        String cardId,
        String localId,
        int sortOrder,
        String name,
        String category,
        String stage,
        String trainerType,
        String energyType,
        String imageLowUrl,
        List<String> types,
        int quantity
) {
}
