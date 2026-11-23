package com.pokemontcg.engine.rules;

import com.pokemontcg.engine.model.ActiveModifier;
import com.pokemontcg.engine.model.ModifierType;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.WeaknessRule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DamageCalculator {

    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    private DamageCalculator() {
    }

    public static int calculateDamage(
            PokemonInPlay attacker,
            PokemonInPlay defender,
            int basePlusBonusDamage,
            int attackerPlusPowerBonus
    ) {
        if (defender == null || attacker == null) {
            return 0;
        }
        boolean immune = defender.modifiers().stream()
                .anyMatch(m -> m.type() == ModifierType.IMMUNE_NEXT_TURN);
        if (immune || basePlusBonusDamage <= 0) {
            return 0;
        }

        int damage = basePlusBonusDamage;
        String attackerType = attacker.activeCard().definition().primaryType();

        for (WeaknessRule weakness : defender.activeCard().definition().weaknesses()) {
            if (attackerType.equalsIgnoreCase(weakness.type())) {
                int multiplier = parseFactor(weakness.value(), 2);
                damage *= multiplier;
                break;
            }
        }

        for (ResistanceRule resistance : defender.activeCard().definition().resistances()) {
            if (attackerType.equalsIgnoreCase(resistance.type())) {
                int reduction = parseFactor(resistance.value(), 30);
                damage = Math.max(0, damage - reduction);
                break;
            }
        }

        boolean preventUpTo30 = defender.modifiers().stream()
                .anyMatch(m -> m.type() == ModifierType.PREVENT_30_OR_LESS_NEXT_TURN);
        if (preventUpTo30 && damage <= 30) {
            return 0;
        }

        if (damage > 0 && attackerPlusPowerBonus > 0) {
            damage += attackerPlusPowerBonus;
        }

        int defenderReduction = defender.modifiers().stream()
                .filter(m -> m.type() == ModifierType.DAMAGE_REDUCTION_20)
                .mapToInt(ActiveModifier::amount)
                .sum();
        damage = Math.max(0, damage - defenderReduction);

        return damage;
    }

    private static int parseFactor(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        Matcher matcher = DIGITS.matcher(rawValue);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
