package com.pokemontcg.catalog.dto;

import java.util.List;

public record CardSummaryDTO(
        String id,
        String setId,
        String localId,
        int sortOrder,
        String name,
        String category,
        Integer hp,
        String stage,
        String trainerType,
        String energyType,
        String rarity,
        String imageLowUrl,
        List<String> types
) {
}
