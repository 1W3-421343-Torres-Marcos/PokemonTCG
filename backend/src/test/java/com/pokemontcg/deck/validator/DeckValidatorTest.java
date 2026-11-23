package com.pokemontcg.deck.validator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckValidatorTest {

    private final DeckValidator validator = new DeckValidator();

    private static Map<String, DeckCardMetadata> sampleCatalog() {
        return Map.of(
                "base1-46", new DeckCardMetadata("base1-46", "Charmander", "Pokemon", "Basic", null),
                "base1-24", new DeckCardMetadata("base1-24", "Charmeleon", "Pokemon", "Stage1", null),
                "base1-4", new DeckCardMetadata("base1-4", "Charizard", "Pokemon", "Stage2", null),
                "base1-58", new DeckCardMetadata("base1-58", "Pikachu", "Pokemon", "Basic", null),
                "base2-58", new DeckCardMetadata("base2-58", "Pikachu", "Pokemon", "Basic", null),
                "base1-91", new DeckCardMetadata("base1-91", "Bill", "Trainer", null, null),
                "base1-88", new DeckCardMetadata("base1-88", "Professor Oak", "Trainer", null, null),
                "base1-96", new DeckCardMetadata("base1-96", "Double Colorless Energy", "Energy", null, "Special"),
                "base1-98", new DeckCardMetadata("base1-98", "Fire Energy", "Energy", "Basic", "Basic"),
                "raw-water", new DeckCardMetadata("raw-water", "Water Energy", "Energy", "Basic", "Normal")
        );
    }

    @Test
    void acceptsValid60CardDeckWithUnlimitedBasicEnergyUnderBasicOrNormalLabel() {
        List<DeckCardEntry> entries = List.of(
                new DeckCardEntry("base1-46", 4),
                new DeckCardEntry("base1-24", 3),
                new DeckCardEntry("base1-4", 2),
                new DeckCardEntry("base1-91", 4),
                new DeckCardEntry("base1-88", 4),
                new DeckCardEntry("base1-96", 4),
                new DeckCardEntry("base1-98", 24),
                new DeckCardEntry("raw-water", 15)
        );

        DeckValidationResult result = validator.validate(entries, sampleCatalog());

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsIncorrectTotalCardCount() {
        List<DeckCardEntry> under = List.of(
                new DeckCardEntry("base1-46", 4),
                new DeckCardEntry("base1-98", 55)
        );
        DeckValidationResult underResult = validator.validate(under, sampleCatalog());
        assertThat(underResult.valid()).isFalse();
        assertThat(underResult.errors()).anyMatch(e -> e.contains("60 cards"));

        List<DeckCardEntry> over = List.of(
                new DeckCardEntry("base1-46", 4),
                new DeckCardEntry("base1-98", 57)
        );
        DeckValidationResult overResult = validator.validate(over, sampleCatalog());
        assertThat(overResult.valid()).isFalse();
    }

    @Test
    void enforcesMax4CopiesByCardNameIncludingDuplicateCardIdsAndSpecialEnergy() {
        List<DeckCardEntry> duplicateCardIds = List.of(
                new DeckCardEntry("base1-46", 3),
                new DeckCardEntry("base1-46", 2), // consolidated = 5
                new DeckCardEntry("base1-98", 55)
        );
        DeckValidationResult dupResult = validator.validate(duplicateCardIds, sampleCatalog());
        assertThat(dupResult.valid()).isFalse();
        assertThat(dupResult.errors()).anyMatch(e -> e.contains("Charmander") && e.contains("5"));

        List<DeckCardEntry> crossSetSameName = List.of(
                new DeckCardEntry("base1-58", 3),
                new DeckCardEntry("base2-58", 2), // both named Pikachu
                new DeckCardEntry("base1-98", 55)
        );
        DeckValidationResult crossSetResult = validator.validate(crossSetSameName, sampleCatalog());
        assertThat(crossSetResult.valid()).isFalse();
        assertThat(crossSetResult.errors()).anyMatch(e -> e.contains("Pikachu"));

        List<DeckCardEntry> tooManySpecialEnergy = List.of(
                new DeckCardEntry("base1-46", 4),
                new DeckCardEntry("base1-96", 5),
                new DeckCardEntry("base1-98", 51)
        );
        DeckValidationResult dceResult = validator.validate(tooManySpecialEnergy, sampleCatalog());
        assertThat(dceResult.valid()).isFalse();
        assertThat(dceResult.errors()).anyMatch(e -> e.contains("Double Colorless Energy"));
    }

    @Test
    void requiresAtLeastOneBasicPokemonEvenWhenBasicEnergyHasStageBasic() {
        List<DeckCardEntry> noBasicPokemon = List.of(
                new DeckCardEntry("base1-24", 4), // Stage1
                new DeckCardEntry("base1-91", 4),
                new DeckCardEntry("base1-98", 52) // Energy with stage='Basic'
        );

        DeckValidationResult result = validator.validate(noBasicPokemon, sampleCatalog());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("Basic Pokémon"));
    }

    @Test
    void rejectsUnknownCardIdAndInvalidQuantities() {
        List<DeckCardEntry> unknown = List.of(
                new DeckCardEntry("base1-46", 4),
                new DeckCardEntry("unknown-999", 1),
                new DeckCardEntry("base1-98", 55)
        );

        DeckValidationResult result = validator.validate(unknown, sampleCatalog());
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown-999"));

        assertThatThrownBy(() -> DeckValidator.consolidateEntries(List.of(new DeckCardEntry("base1-46", 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
