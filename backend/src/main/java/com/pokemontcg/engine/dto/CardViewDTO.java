package com.pokemontcg.engine.dto;

import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.WeaknessRule;

import java.util.List;

public record CardViewDTO(
        String instanceId,
        String cardId,
        String name,
        String category,
        String stage,
        String evolveFrom,
        Integer hp,
        List<String> types,
        List<AttackViewDTO> attacks,
        List<WeaknessRule> weaknesses,
        List<ResistanceRule> resistances,
        int retreatCost,
        String trainerType,
        String energyType,
        String providedEnergyType,
        int providedEnergyCount,
        String imageLowUrl,
        String imageHighUrl
) {
    public CardViewDTO {
        types = types == null ? List.of() : List.copyOf(types);
        attacks = attacks == null ? List.of() : List.copyOf(attacks);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        resistances = resistances == null ? List.of() : List.copyOf(resistances);
    }
}
