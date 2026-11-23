package com.pokemontcg.engine;

import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.WeaknessRule;

import java.util.List;

final class EngineTestFixtures {

    private EngineTestFixtures() {
    }

    static CardDefinition charmander() {
        return new CardDefinition(
                "base1-46",
                "Charmander",
                "Pokemon",
                "Basic",
                null,
                50,
                List.of("Fire"),
                List.of(
                        new AttackDefinition("Scratch", List.of("Colorless"), 10, "10", null),
                        new AttackDefinition("Ember", List.of("Fire", "Colorless"), 30, "30", "Discard 1 Fire Energy.")
                ),
                List.of(new WeaknessRule("Water", "×2")),
                List.of(),
                1,
                null,
                null,
                null,
                0,
                "/img/base1-46-low.webp",
                "/img/base1-46-high.webp"
        );
    }

    static CardDefinition magmar() {
        return new CardDefinition(
                "base1-36",
                "Magmar",
                "Pokemon",
                "Basic",
                null,
                50,
                List.of("Fire"),
                List.of(
                        new AttackDefinition("Fire Punch", List.of("Fire", "Fire"), 30, "30", null),
                        new AttackDefinition("Flamethrower", List.of("Fire", "Fire", "Colorless"), 50, "50", null)
                ),
                List.of(new WeaknessRule("Water", "×2")),
                List.of(),
                2,
                null,
                null,
                null,
                0,
                "/img/base1-36-low.webp",
                "/img/base1-36-high.webp"
        );
    }

    static CardDefinition squirtle() {
        return new CardDefinition(
                "base1-63",
                "Squirtle",
                "Pokemon",
                "Basic",
                null,
                40,
                List.of("Water"),
                List.of(
                        new AttackDefinition("Bubble", List.of("Water"), 10, "10", null),
                        new AttackDefinition("Withdraw", List.of("Water", "Colorless"), 0, null, null)
                ),
                List.of(new WeaknessRule("Lightning", "×2")),
                List.of(new ResistanceRule("Fighting", "-30")),
                1,
                null,
                null,
                null,
                0,
                "/img/base1-63-low.webp",
                "/img/base1-63-high.webp"
        );
    }

    static CardDefinition fireEnergy() {
        return new CardDefinition(
                "base1-98",
                "Fire Energy",
                "Energy",
                "Basic",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                null,
                "Basic",
                "Fire",
                1,
                "/img/base1-98-low.webp",
                "/img/base1-98-high.webp"
        );
    }

    static CardDefinition waterEnergy() {
        return new CardDefinition(
                "base1-102",
                "Water Energy",
                "Energy",
                "Basic",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                null,
                "Basic",
                "Water",
                1,
                "/img/base1-102-low.webp",
                "/img/base1-102-high.webp"
        );
    }

    static CardDefinition doubleColorlessEnergy() {
        return new CardDefinition(
                "base1-96",
                "Double Colorless Energy",
                "Energy",
                "Basic",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                null,
                "Special",
                "Colorless",
                2,
                "/img/base1-96-low.webp",
                "/img/base1-96-high.webp"
        );
    }
}
