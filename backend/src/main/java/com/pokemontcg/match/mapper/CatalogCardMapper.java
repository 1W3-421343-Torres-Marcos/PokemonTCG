package com.pokemontcg.match.mapper;

import com.pokemontcg.catalog.domain.CardAttack;
import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.domain.CardTypeModifier;
import com.pokemontcg.deck.domain.DeckCardEntity;
import com.pokemontcg.deck.domain.DeckEntity;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.WeaknessRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CatalogCardMapper {

    private static final Pattern LEADING_DIGITS = Pattern.compile("^(\\d+)");

    public List<CardDefinition> toDeckDefinitions(DeckEntity deck) {
        List<DeckCardEntity> sortedEntries = deck.getCards().stream()
                .sorted(Comparator
                        .comparing((DeckCardEntity entry) -> entry.getCard().getSortOrder())
                        .thenComparing(entry -> entry.getCard().getId()))
                .toList();

        List<CardDefinition> definitions = new ArrayList<>(60);
        for (DeckCardEntity entry : sortedEntries) {
            CardDefinition definition = toCardDefinition(entry.getCard());
            for (int i = 0; i < entry.getQuantity(); i++) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    public CardDefinition toCardDefinition(CardEntity entity) {
        List<AttackDefinition> attacks = entity.getAttacks().stream()
                .map(this::toAttackDefinition)
                .toList();

        List<WeaknessRule> weaknesses = entity.getWeaknesses().stream()
                .map(this::toWeaknessRule)
                .toList();

        List<ResistanceRule> resistances = entity.getResistances().stream()
                .map(this::toResistanceRule)
                .toList();

        List<String> types = List.copyOf(entity.getTypes());
        EnergyProvision provision = resolveEnergyProvision(entity, types);

        return new CardDefinition(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getStage(),
                entity.getEvolveFrom(),
                entity.getHp(),
                types,
                attacks,
                weaknesses,
                resistances,
                entity.getRetreat() == null ? 0 : Math.max(0, entity.getRetreat()),
                entity.getTrainerType(),
                entity.getEnergyType(),
                provision.type(),
                provision.count(),
                entity.getImageLowUrl(),
                entity.getImageHighUrl()
        );
    }

    public int parseBaseDamage(String rawDamage) {
        if (rawDamage == null || rawDamage.isBlank()) {
            return 0;
        }
        Matcher matcher = LEADING_DIGITS.matcher(rawDamage.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private AttackDefinition toAttackDefinition(CardAttack attack) {
        return new AttackDefinition(
                attack.name(),
                attack.cost() == null ? List.of() : List.copyOf(attack.cost()),
                parseBaseDamage(attack.damage()),
                attack.damage(),
                attack.effect()
        );
    }

    private WeaknessRule toWeaknessRule(CardTypeModifier modifier) {
        return new WeaknessRule(modifier.type(), modifier.value());
    }

    private ResistanceRule toResistanceRule(CardTypeModifier modifier) {
        return new ResistanceRule(modifier.type(), modifier.value());
    }

    private EnergyProvision resolveEnergyProvision(CardEntity entity, List<String> types) {
        if (!"Energy".equalsIgnoreCase(entity.getCategory())) {
            return new EnergyProvision(null, 0);
        }

        String cardId = entity.getId() == null ? "" : entity.getId().trim().toLowerCase(Locale.ROOT);
        String name = entity.getName() == null ? "" : entity.getName().trim().toLowerCase(Locale.ROOT);

        if ("base1-96".equals(cardId) || name.contains("double colorless")) {
            return new EnergyProvision("Colorless", 2);
        }
        if ("base1-97".equals(cardId) || name.contains("fighting")) {
            return new EnergyProvision("Fighting", 1);
        }
        if ("base1-98".equals(cardId) || name.contains("fire")) {
            return new EnergyProvision("Fire", 1);
        }
        if ("base1-99".equals(cardId) || name.contains("grass")) {
            return new EnergyProvision("Grass", 1);
        }
        if ("base1-100".equals(cardId) || name.contains("lightning")) {
            return new EnergyProvision("Lightning", 1);
        }
        if ("base1-101".equals(cardId) || name.contains("psychic")) {
            return new EnergyProvision("Psychic", 1);
        }
        if ("base1-102".equals(cardId) || name.contains("water")) {
            return new EnergyProvision("Water", 1);
        }
        if (name.contains("darkness")) {
            return new EnergyProvision("Darkness", 1);
        }
        if (name.contains("metal")) {
            return new EnergyProvision("Metal", 1);
        }
        if (!types.isEmpty()) {
            return new EnergyProvision(types.get(0), 1);
        }
        return new EnergyProvision("Colorless", 1);
    }

    private record EnergyProvision(String type, int count) {
    }
}
