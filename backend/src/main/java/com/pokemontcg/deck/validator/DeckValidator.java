package com.pokemontcg.deck.validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeckValidator {

    public static final int REQUIRED_DECK_SIZE = 60;
    public static final int MAX_COPIES_PER_NAME = 4;

    public static List<DeckCardEntry> consolidateEntries(List<DeckCardEntry> entries) {
        if (entries == null) {
            return List.of();
        }
        Map<String, Integer> consolidated = new LinkedHashMap<>();
        for (DeckCardEntry entry : entries) {
            if (entry == null || entry.cardId() == null || entry.cardId().isBlank()) {
                throw new IllegalArgumentException("Card ID must not be blank");
            }
            if (entry.quantity() <= 0) {
                throw new IllegalArgumentException("Card quantity must be >= 1 for card " + entry.cardId());
            }
            String cardId = entry.cardId().trim();
            consolidated.merge(cardId, entry.quantity(), Integer::sum);
        }
        return consolidated.entrySet().stream()
                .map(e -> new DeckCardEntry(e.getKey(), e.getValue()))
                .toList();
    }

    public DeckValidationResult validate(
            List<DeckCardEntry> rawEntries,
            Map<String, DeckCardMetadata> catalogById
    ) {
        List<DeckCardEntry> entries = consolidateEntries(rawEntries);
        List<String> errors = new ArrayList<>();

        int totalCards = 0;
        boolean hasBasicPokemon = false;
        Map<String, Integer> copiesByName = new LinkedHashMap<>();
        Map<String, DeckCardMetadata> representativeByName = new LinkedHashMap<>();

        for (DeckCardEntry entry : entries) {
            totalCards += entry.quantity();
            DeckCardMetadata meta = catalogById != null ? catalogById.get(entry.cardId()) : null;
            if (meta == null) {
                errors.add("Unknown card ID: " + entry.cardId());
                continue;
            }
            if (isBasicPokemon(meta)) {
                hasBasicPokemon = true;
            }
            String normalizedName = meta.name().trim().toLowerCase(Locale.ROOT);
            copiesByName.merge(normalizedName, entry.quantity(), Integer::sum);
            representativeByName.putIfAbsent(normalizedName, meta);
        }

        if (totalCards != REQUIRED_DECK_SIZE) {
            errors.add("Deck must contain exactly " + REQUIRED_DECK_SIZE + " cards (currently " + totalCards + ")");
        }

        if (!hasBasicPokemon) {
            errors.add("Deck must include at least 1 Basic Pokémon");
        }

        for (Map.Entry<String, Integer> entry : copiesByName.entrySet()) {
            DeckCardMetadata meta = representativeByName.get(entry.getKey());
            if (!isBasicEnergy(meta) && entry.getValue() > MAX_COPIES_PER_NAME) {
                errors.add("Maximum " + MAX_COPIES_PER_NAME + " copies allowed for '" + meta.name()
                        + "' (found " + entry.getValue() + ")");
            }
        }

        return new DeckValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    public static boolean isBasicEnergy(DeckCardMetadata meta) {
        if (meta == null || !"Energy".equalsIgnoreCase(meta.category())) {
            return false;
        }
        return "Basic".equalsIgnoreCase(meta.energyType())
                || "Normal".equalsIgnoreCase(meta.energyType());
    }

    public static boolean isBasicPokemon(DeckCardMetadata meta) {
        return meta != null
                && "Pokemon".equalsIgnoreCase(meta.category())
                && "Basic".equalsIgnoreCase(meta.stage());
    }
}
