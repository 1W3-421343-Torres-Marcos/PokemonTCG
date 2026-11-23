package com.pokemontcg.bot;

import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BotVsBotSimulationTest {

    @Test
    void simulatesMultipleCompleteBotVsBotMatchesToGameOverWithoutInfiniteLoops() {
        GameEngine engine = new GameEngine();
        BotTurnRunner runner = new BotTurnRunner(engine);
        BasicBotStrategy strategy = new BasicBotStrategy();

        List<CardDefinition> fireDeck = buildFireStarterStyleDeck();
        List<CardDefinition> waterDeck = buildWaterStarterStyleDeck();

        for (long seed = 1; seed <= 5; seed++) {
            Random seededRandom = new Random(seed);
            RandomProvider random = new RandomProvider() {
                @Override
                public void shuffle(List<?> items) {
                    Collections.shuffle(items, seededRandom);
                }

                @Override
                public boolean flipCoin() {
                    return seededRandom.nextBoolean();
                }
            };

            EngineStepResult init = engine.initializeMatch(fireDeck, waterDeck, random);
            GameState state = init.state();
            List<GameEvent> allEvents = new ArrayList<>(init.events());

            int iterations = 0;
            while (state.phase() != TurnPhase.GAME_OVER && iterations < 200) {
                boolean acted = false;
                if (runner.requiresBotAction(state, PlayerId.PLAYER)) {
                    EngineStepResult step = runner.runUntilHumanTurn(state, PlayerId.PLAYER, strategy);
                    state = step.state();
                    allEvents.addAll(step.events());
                    acted = true;
                }
                if (state.phase() != TurnPhase.GAME_OVER && runner.requiresBotAction(state, PlayerId.CPU)) {
                    EngineStepResult step = runner.runUntilHumanTurn(state, PlayerId.CPU, strategy);
                    state = step.state();
                    allEvents.addAll(step.events());
                    acted = true;
                }
                assertThat(acted)
                        .as("Simulation stalled on seed %d at phase %s", seed, state.phase())
                        .isTrue();
                iterations++;
            }

            assertThat(state.phase()).isEqualTo(TurnPhase.GAME_OVER);
            assertThat(state.winner()).isNotNull();
            assertThat(state.winReason()).isNotNull();
            assertThat(allEvents).isNotEmpty();

            // Event sequence numbers must be strictly increasing
            long previousSeq = 0;
            for (GameEvent event : allEvents) {
                assertThat(event.sequence()).isGreaterThan(previousSeq);
                previousSeq = event.sequence();
            }
        }
    }

    private List<CardDefinition> buildFireStarterStyleDeck() {
        CardDefinition charmander = pokemon("base1-46", "Charmander", 50, "Fire",
                new AttackDefinition("Scratch", List.of("Colorless"), 10, "10", null),
                new AttackDefinition("Ember", List.of("Fire", "Colorless"), 30, "30", null));
        CardDefinition magmar = pokemon("base1-36", "Magmar", 50, "Fire",
                new AttackDefinition("Fire Punch", List.of("Fire", "Fire"), 30, "30", null),
                new AttackDefinition("Flamethrower", List.of("Fire", "Fire", "Colorless"), 50, "50", null));
        CardDefinition ponyta = pokemon("base1-60", "Ponyta", 40, "Fire",
                new AttackDefinition("Smash Kick", List.of("Colorless", "Colorless"), 20, "20", null),
                new AttackDefinition("Flame Tail", List.of("Fire", "Fire"), 30, "30", null));
        CardDefinition growlithe = pokemon("base1-28", "Growlithe", 60, "Fire",
                new AttackDefinition("Flare", List.of("Fire", "Colorless"), 20, "20", null));
        CardDefinition fireEnergy = energy("base1-98", "Fire Energy", "Basic", "Fire", 1);
        CardDefinition dce = energy("base1-96", "Double Colorless Energy", "Special", "Colorless", 2);

        List<CardDefinition> deck = new ArrayList<>(60);
        addCopies(deck, charmander, 4);
        addCopies(deck, magmar, 4);
        addCopies(deck, ponyta, 4);
        addCopies(deck, growlithe, 4);
        addCopies(deck, dce, 4);
        while (deck.size() < 60) {
            deck.add(fireEnergy);
        }
        return deck;
    }

    private List<CardDefinition> buildWaterStarterStyleDeck() {
        CardDefinition squirtle = pokemon("base1-63", "Squirtle", 40, "Water",
                new AttackDefinition("Bubble", List.of("Water"), 10, "10", null));
        CardDefinition laprasLike = pokemon("base1-24", "Seel", 60, "Water",
                new AttackDefinition("Headbutt", List.of("Water"), 10, "10", null));
        CardDefinition staryu = pokemon("base1-65", "Staryu", 40, "Water",
                new AttackDefinition("Slap", List.of("Water"), 20, "20", null));
        CardDefinition poliwag = pokemon("base1-59", "Poliwag", 40, "Water",
                new AttackDefinition("Water Gun", List.of("Water", "Colorless"), 20, "20", null));
        CardDefinition waterEnergy = energy("base1-102", "Water Energy", "Basic", "Water", 1);
        CardDefinition dce = energy("base1-96", "Double Colorless Energy", "Special", "Colorless", 2);

        List<CardDefinition> deck = new ArrayList<>(60);
        addCopies(deck, squirtle, 4);
        addCopies(deck, laprasLike, 4);
        addCopies(deck, staryu, 4);
        addCopies(deck, poliwag, 4);
        addCopies(deck, dce, 4);
        while (deck.size() < 60) {
            deck.add(waterEnergy);
        }
        return deck;
    }

    private CardDefinition pokemon(String id, String name, int hp, String type, AttackDefinition... attacks) {
        return new CardDefinition(
                id, name, "Pokemon", "Basic", null, hp,
                List.of(type), List.of(attacks), List.of(), List.of(),
                1, null, null, null, 0, null, null
        );
    }

    private CardDefinition energy(String id, String name, String energyType, String providedType, int count) {
        return new CardDefinition(
                id, name, "Energy", "Basic", null, null,
                List.of(), List.of(), List.of(), List.of(),
                0, null, energyType, providedType, count, null, null
        );
    }

    private void addCopies(List<CardDefinition> deck, CardDefinition card, int count) {
        for (int i = 0; i < count; i++) {
            deck.add(card);
        }
    }
}
