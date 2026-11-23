package com.pokemontcg.catalog.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TcgdexCardDetailResponse(
        String id,
        String localId,
        String name,
        String category,
        String image,
        Integer hp,
        List<String> types,
        String evolveFrom,
        String stage,
        Integer retreat,
        String rarity,
        String illustrator,
        String trainerType,
        String energyType,
        String effect,
        String description,
        List<TcgdexAttack> attacks,
        List<TcgdexTypeModifier> weaknesses,
        List<TcgdexTypeModifier> resistances
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TcgdexAttack(List<String> cost, String name, String effect, String damage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TcgdexTypeModifier(String type, String value) {
    }
}
