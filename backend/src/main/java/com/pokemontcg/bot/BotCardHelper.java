package com.pokemontcg.bot;

import com.pokemontcg.engine.dto.AttackViewDTO;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.dto.PokemonViewDTO;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.WeaknessRule;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared pure-Java heuristics and card inspection helpers operating strictly on {@code PlayerViewDTO}.
 */
final class BotCardHelper {

    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    private BotCardHelper() {
    }

    static boolean isBasicPokemon(CardViewDTO card) {
        return card != null
                && "Pokemon".equalsIgnoreCase(card.category())
                && "Basic".equalsIgnoreCase(card.stage());
    }

    static boolean isEvolutionPokemon(CardViewDTO card) {
        if (card == null || !"Pokemon".equalsIgnoreCase(card.category()) || card.stage() == null) {
            return false;
        }
        String normalized = card.stage().replace(" ", "");
        return "Stage1".equalsIgnoreCase(normalized) || "Stage2".equalsIgnoreCase(normalized);
    }

    static boolean isEnergy(CardViewDTO card) {
        return card != null && "Energy".equalsIgnoreCase(card.category());
    }

    static boolean isImmobilized(PokemonViewDTO pokemon) {
        return pokemon != null && (pokemon.statusConditions().contains(StatusCondition.ASLEEP)
                || pokemon.statusConditions().contains(StatusCondition.PARALYZED));
    }

    static int maxAttackDamage(List<AttackViewDTO> attacks) {
        return attacks.stream().mapToInt(AttackViewDTO::baseDamage).max().orElse(0);
    }

    static String normalizeType(String value) {
        return value == null ? "colorless" : value.trim().toLowerCase(Locale.ROOT);
    }

    static int estimateEffectiveDamage(PokemonViewDTO attacker, PokemonViewDTO defender, int baseDamage) {
        if (attacker == null || defender == null || baseDamage <= 0) {
            return Math.max(0, baseDamage);
        }
        int damage = baseDamage;
        String attackerType = attacker.activeCard().types().isEmpty()
                ? "Colorless"
                : attacker.activeCard().types().get(0);

        for (WeaknessRule weakness : defender.activeCard().weaknesses()) {
            if (attackerType.equalsIgnoreCase(weakness.type())) {
                damage *= parseFactor(weakness.value(), 2);
                break;
            }
        }
        for (ResistanceRule resistance : defender.activeCard().resistances()) {
            if (attackerType.equalsIgnoreCase(resistance.type())) {
                damage = Math.max(0, damage - parseFactor(resistance.value(), 30));
                break;
            }
        }
        return damage;
    }

    static int bestEffectiveDamage(PokemonViewDTO attacker, PokemonViewDTO defender) {
        if (attacker == null) {
            return 0;
        }
        return attacker.attacks().stream()
                .filter(AttackViewDTO::canPay)
                .mapToInt(a -> estimateEffectiveDamage(attacker, defender, a.baseDamage()))
                .max()
                .orElse(0);
    }

    static boolean hasWeaknessAdvantage(PokemonViewDTO attacker, PokemonViewDTO defender) {
        if (attacker == null || defender == null || attacker.activeCard().types().isEmpty()) {
            return false;
        }
        String attackerType = attacker.activeCard().types().get(0);
        return defender.activeCard().weaknesses().stream()
                .anyMatch(w -> attackerType.equalsIgnoreCase(w.type()));
    }

    static CardViewDTO selectBestEnergyForTarget(List<CardViewDTO> energies, PokemonViewDTO target) {
        Optional<AttackViewDTO> priorityAttack = target.attacks().stream()
                .filter(a -> !a.canPay())
                .max(Comparator.comparingInt(AttackViewDTO::baseDamage));

        if (priorityAttack.isPresent()) {
            Map<String, Integer> attachedCounts = new HashMap<>();
            for (CardViewDTO attached : target.attachedEnergies()) {
                String type = normalizeType(attached.providedEnergyType());
                attachedCounts.merge(type, Math.max(1, attached.providedEnergyCount()), Integer::sum);
            }

            for (String costType : priorityAttack.get().cost()) {
                String normalized = normalizeType(costType);
                if ("colorless".equals(normalized) || "free".equals(normalized)) {
                    continue;
                }
                int available = attachedCounts.getOrDefault(normalized, 0);
                if (available > 0) {
                    attachedCounts.put(normalized, available - 1);
                    continue;
                }
                Optional<CardViewDTO> exactMatch = energies.stream()
                        .filter(e -> normalized.equals(normalizeType(e.providedEnergyType())))
                        .findFirst();
                if (exactMatch.isPresent()) {
                    return exactMatch.get();
                }
            }
        }

        return energies.get(0);
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
