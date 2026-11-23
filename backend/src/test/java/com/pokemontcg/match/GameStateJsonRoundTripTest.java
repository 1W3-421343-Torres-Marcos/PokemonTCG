package com.pokemontcg.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WeaknessRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateJsonRoundTripTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAndDeserializesGameStateCleanlyWithoutSpuriousFields() throws Exception {
        CardDefinition charCard = new CardDefinition(
                "base1-46", "Charmander", "Pokemon", "Basic", null, 50,
                List.of("Fire"),
                List.of(new AttackDefinition("Ember", List.of("Fire", "Colorless"), 30, "30", "Discard 1 Fire")),
                List.of(new WeaknessRule("Water", "×2")),
                List.of(new ResistanceRule("Fighting", "-30")),
                1, null, null, null, 0,
                "/low.webp", "/high.webp"
        );
        CardDefinition fireEnergy = new CardDefinition(
                "base1-98", "Fire Energy", "Energy", "Basic", null, null,
                List.of(), List.of(), List.of(), List.of(),
                0, null, "Basic", "Fire", 1,
                "/e-low.webp", "/e-high.webp"
        );

        CardInstance pActiveCard = new CardInstance("p-1", charCard);
        CardInstance pBenchCard = new CardInstance("p-2", charCard);
        CardInstance pEnergy = new CardInstance("p-3", fireEnergy);
        CardInstance pHandCard = new CardInstance("p-4", fireEnergy);
        CardInstance cActiveCard = new CardInstance("c-1", charCard);

        PokemonInPlay pActive = new PokemonInPlay(
                "p-1", pActiveCard, List.of(pActiveCard), List.of(pEnergy),
                20, Set.of(StatusCondition.BURNED), 0, 0
        );
        PokemonInPlay pBench = PokemonInPlay.fromBasic(pBenchCard, 2);
        PokemonInPlay cActive = PokemonInPlay.fromBasic(cActiveCard, 0);

        PlayerState player = new PlayerState(
                List.of(new CardInstance("p-5", fireEnergy)),
                List.of(pHandCard),
                pActive,
                List.of(pBench),
                List.of(new CardInstance("p-6", fireEnergy)),
                List.of(),
                true,
                false,
                true,
                1
        );
        PlayerState cpu = new PlayerState(
                List.of(new CardInstance("c-2", fireEnergy)),
                List.of(),
                cActive,
                List.of(),
                List.of(new CardInstance("c-3", fireEnergy)),
                List.of(),
                false,
                false,
                true,
                0
        );

        GameState original = new GameState(
                3,
                PlayerId.PLAYER,
                PlayerId.PLAYER,
                TurnPhase.MAIN_PHASE,
                null,
                Map.of(PlayerId.PLAYER, player, PlayerId.CPU, cpu),
                null,
                null,
                14L
        );

        String json = objectMapper.writeValueAsString(original);
        JsonNode tree = objectMapper.readTree(json);

        // Verify no spurious get*/is* properties exist on PokemonInPlay or CardDefinition
        JsonNode activeJson = tree.get("players").get("PLAYER").get("active");
        assertThat(activeJson.has("knockedOut")).isFalse();
        assertThat(activeJson.has("isKnockedOut")).isFalse();
        assertThat(activeJson.has("maxHp")).isFalse();
        assertThat(activeJson.has("remainingHp")).isFalse();
        assertThat(activeJson.has("allCards")).isFalse();

        GameState restored = objectMapper.readValue(json, GameState.class);
        assertThat(restored).isEqualTo(original);
    }
}
