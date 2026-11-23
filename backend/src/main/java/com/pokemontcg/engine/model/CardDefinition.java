package com.pokemontcg.engine.model;

import java.util.List;
import java.util.Objects;

public record CardDefinition(
        String id,
        String name,
        String category,
        String stage,
        String evolveFrom,
        Integer hp,
        List<String> types,
        List<AttackDefinition> attacks,
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
    public CardDefinition {
        id = Objects.requireNonNull(id, "card id must not be null");
        name = Objects.requireNonNull(name, "card name must not be null");
        category = Objects.requireNonNull(category, "card category must not be null");
        types = types == null ? List.of() : List.copyOf(types);
        attacks = attacks == null ? List.of() : List.copyOf(attacks);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        resistances = resistances == null ? List.of() : List.copyOf(resistances);
        retreatCost = Math.max(0, retreatCost);
        providedEnergyCount = Math.max(0, providedEnergyCount);
    }

    public boolean pokemonCard() {
        return "Pokemon".equalsIgnoreCase(category);
    }

    public boolean basicPokemon() {
        return pokemonCard() && "Basic".equalsIgnoreCase(stage);
    }

    public boolean evolutionCard() {
        if (!pokemonCard() || stage == null) {
            return false;
        }
        String normalized = stage.replace(" ", "");
        return "Stage1".equalsIgnoreCase(normalized) || "Stage2".equalsIgnoreCase(normalized);
    }

    public String primaryType() {
        return types.isEmpty() ? "Colorless" : types.get(0);
    }

    public boolean energyCard() {
        return "Energy".equalsIgnoreCase(category);
    }
}
