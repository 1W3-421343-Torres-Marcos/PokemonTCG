package com.pokemontcg.match;

import com.pokemontcg.catalog.domain.CardAttack;
import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.domain.CardTypeModifier;
import com.pokemontcg.deck.domain.DeckCardEntity;
import com.pokemontcg.deck.domain.DeckEntity;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.match.mapper.CatalogCardMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogCardMapperTest {

    private final CatalogCardMapper mapper = new CatalogCardMapper();

    @Test
    void infersEnergyTypesForAllBase1EnergiesIncludingDoubleColorless() {
        assertEnergy("base1-96", "Double Colorless Energy", "Special", "Colorless", 2);
        assertEnergy("base1-97", "Fighting Energy", "Basic", "Fighting", 1);
        assertEnergy("base1-98", "Fire Energy", "Basic", "Fire", 1);
        assertEnergy("base1-99", "Grass Energy", "Basic", "Grass", 1);
        assertEnergy("base1-100", "Lightning Energy", "Basic", "Lightning", 1);
        assertEnergy("base1-101", "Psychic Energy", "Basic", "Psychic", 1);
        assertEnergy("base1-102", "Water Energy", "Basic", "Water", 1);
    }

    @Test
    void parsesNumericBaseDamageVariantsAndExpandsFullDeck() {
        assertThat(mapper.parseBaseDamage("30+")).isEqualTo(30);
        assertThat(mapper.parseBaseDamage("20×")).isEqualTo(20);
        assertThat(mapper.parseBaseDamage("100")).isEqualTo(100);
        assertThat(mapper.parseBaseDamage(null)).isEqualTo(0);
        assertThat(mapper.parseBaseDamage("")).isEqualTo(0);

        CardEntity pikachu = new CardEntity();
        pikachu.setId("base1-58");
        pikachu.setSortOrder(58);
        pikachu.setName("Pikachu");
        pikachu.setCategory("Pokemon");
        pikachu.setStage("Basic");
        pikachu.setHp(40);
        pikachu.setRetreat(1);
        pikachu.setTypes(Set.of("Lightning"));
        pikachu.setAttacks(List.of(
                new CardAttack(List.of("Colorless"), "Gnaw", null, "10"),
                new CardAttack(List.of("Lightning", "Colorless"), "Thunder Jolt", "Flip a coin", "30")
        ));
        pikachu.setWeaknesses(List.of(new CardTypeModifier("Fighting", "×2")));

        CardEntity energy = buildEnergyEntity("base1-100", "Lightning Energy", "Basic");
        energy.setSortOrder(100);

        DeckEntity deck = new DeckEntity();
        deck.setId(UUID.randomUUID());
        deck.setName("Test Lightning");
        deck.getCards().add(new DeckCardEntity(deck, pikachu, 4));
        deck.getCards().add(new DeckCardEntity(deck, energy, 56));

        List<CardDefinition> mapped = mapper.toDeckDefinitions(deck);
        assertThat(mapped).hasSize(60);
        assertThat(mapped.get(0).name()).isEqualTo("Pikachu");
        assertThat(mapped.get(0).attacks().get(1).baseDamage()).isEqualTo(30);
        assertThat(mapped.get(59).providedEnergyType()).isEqualTo("Lightning");
        assertThat(mapped.get(59).providedEnergyCount()).isEqualTo(1);
    }

    private void assertEnergy(String id, String name, String energyType, String expectedProvided, int expectedCount) {
        CardEntity entity = buildEnergyEntity(id, name, energyType);
        CardDefinition def = mapper.toCardDefinition(entity);
        assertThat(def.energyCard()).isTrue();
        assertThat(def.providedEnergyType()).isEqualTo(expectedProvided);
        assertThat(def.providedEnergyCount()).isEqualTo(expectedCount);
    }

    private CardEntity buildEnergyEntity(String id, String name, String energyType) {
        CardEntity entity = new CardEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setCategory("Energy");
        entity.setStage("Basic");
        entity.setEnergyType(energyType);
        entity.setTypes(Set.of()); // Reflects real base1 empty types[] for energies
        return entity;
    }
}
