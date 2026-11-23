package com.pokemontcg.engine;

import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.rules.EnergyCostMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyCostMatcherTest {

    @Test
    void matchesSpecificAndColorlessCostsCorrectly() {
        CardInstance fire = new CardInstance("e-1", EngineTestFixtures.fireEnergy());
        CardInstance water = new CardInstance("e-2", EngineTestFixtures.waterEnergy());

        assertThat(EnergyCostMatcher.canPay(List.of(fire, water), List.of("Fire", "Colorless"))).isTrue();
        assertThat(EnergyCostMatcher.canPay(List.of(water), List.of("Fire", "Colorless"))).isFalse();
        assertThat(EnergyCostMatcher.canPay(List.of(fire), List.of("Fire", "Colorless"))).isFalse();
        assertThat(EnergyCostMatcher.canPay(List.of(), List.of())).isTrue();
    }

    @Test
    void supportsDoubleColorlessEnergyAsTwoColorlessUnitsOnly() {
        CardInstance dce = new CardInstance("e-dce", EngineTestFixtures.doubleColorlessEnergy());
        CardInstance fire = new CardInstance("e-fire", EngineTestFixtures.fireEnergy());

        // [Fire, Fire, Colorless] with 1 Fire + 1 DCE (= 2 Colorless): missing second Fire
        assertThat(EnergyCostMatcher.canPay(List.of(fire, dce), List.of("Fire", "Fire", "Colorless"))).isFalse();

        // [Fire, Colorless, Colorless] with 1 Fire + 1 DCE: satisfies both Colorless units
        assertThat(EnergyCostMatcher.canPay(List.of(fire, dce), List.of("Fire", "Colorless", "Colorless"))).isTrue();

        // Pure Double Colorless satisfies [Colorless, Colorless]
        assertThat(EnergyCostMatcher.canPay(List.of(dce), List.of("Colorless", "Colorless"))).isTrue();
    }
}
