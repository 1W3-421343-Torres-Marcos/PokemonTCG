package com.pokemontcg.engine.dto;

import com.pokemontcg.engine.model.StatusCondition;

import java.util.List;

public record PokemonViewDTO(
        String instanceId,
        CardViewDTO activeCard,
        int maxHp,
        int remainingHp,
        int damage,
        List<CardViewDTO> attachedEnergies,
        List<StatusCondition> statusConditions,
        List<AttackViewDTO> attacks,
        int turnEnteredPlay,
        int turnLastEvolved
) {
    public PokemonViewDTO {
        attachedEnergies = attachedEnergies == null ? List.of() : List.copyOf(attachedEnergies);
        statusConditions = statusConditions == null ? List.of() : List.copyOf(statusConditions);
        attacks = attacks == null ? List.of() : List.copyOf(attacks);
    }
}
