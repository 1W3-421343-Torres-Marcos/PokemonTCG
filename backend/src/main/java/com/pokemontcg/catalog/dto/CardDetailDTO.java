package com.pokemontcg.catalog.dto;

import com.pokemontcg.catalog.domain.CardAttack;
import com.pokemontcg.catalog.domain.CardTypeModifier;

import java.util.List;

public record CardDetailDTO(
        String id,
        String setId,
        String setName,
        String localId,
        int sortOrder,
        String name,
        String category,
        Integer hp,
        String stage,
        String evolveFrom,
        Integer retreat,
        String trainerType,
        String energyType,
        String rarity,
        String illustrator,
        String imageLowUrl,
        String imageHighUrl,
        List<String> types,
        List<CardAttack> attacks,
        List<CardTypeModifier> weaknesses,
        List<CardTypeModifier> resistances,
        String effectText
) {
}
