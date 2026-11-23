package com.pokemontcg.engine.rules;

import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.PokemonInPlay;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnergyCostMatcher {

    private EnergyCostMatcher() {
    }

    public static boolean canPay(PokemonInPlay pokemon, List<String> attackCost) {
        if (pokemon == null) {
            return false;
        }
        return canPay(pokemon.attachedEnergies(), attackCost);
    }

    public static boolean canPay(List<CardInstance> attachedEnergies, List<String> attackCost) {
        if (attackCost == null || attackCost.isEmpty()) {
            return true;
        }
        if (attachedEnergies == null || attachedEnergies.isEmpty()) {
            return false;
        }

        Map<String, Integer> availableByType = new HashMap<>();
        int totalAvailable = 0;

        for (CardInstance card : attachedEnergies) {
            int units = resolveEnergyUnits(card.definition());
            if (units <= 0) {
                continue;
            }
            String type = resolveEnergyType(card.definition());
            availableByType.merge(type, units, Integer::sum);
            totalAvailable += units;
        }

        int colorlessRequired = 0;
        for (String rawCost : attackCost) {
            if (rawCost == null || rawCost.isBlank()) {
                continue;
            }
            String normalized = normalizeType(rawCost);
            if ("colorless".equals(normalized) || "free".equals(normalized)) {
                if ("colorless".equals(normalized)) {
                    colorlessRequired++;
                }
                continue;
            }
            int remainingOfType = availableByType.getOrDefault(normalized, 0);
            if (remainingOfType <= 0) {
                return false;
            }
            availableByType.put(normalized, remainingOfType - 1);
            totalAvailable--;
        }

        return totalAvailable >= colorlessRequired;
    }

    private static int resolveEnergyUnits(CardDefinition def) {
        if (def.providedEnergyCount() > 0) {
            return def.providedEnergyCount();
        }
        if (def.energyCard()) {
            return 1;
        }
        return 0;
    }

    private static String resolveEnergyType(CardDefinition def) {
        if (def.providedEnergyType() != null && !def.providedEnergyType().isBlank()) {
            return normalizeType(def.providedEnergyType());
        }
        if (!def.types().isEmpty()) {
            return normalizeType(def.types().get(0));
        }
        return "colorless";
    }

    private static String normalizeType(String type) {
        return type.trim().toLowerCase(Locale.ROOT);
    }
}
