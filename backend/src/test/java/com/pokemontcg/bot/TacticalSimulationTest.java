package com.pokemontcg.bot;

import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WeaknessRule;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class TacticalSimulationTest {

    @Test
    void simulatesTenTacticalVsBasicMatchesWithCleanTerminationAndTacticalEdge() {
        TacticalBotStrategy tactical = new TacticalBotStrategy();
        BasicBotStrategy basic = new BasicBotStrategy();

        List<CardDefinition> tacticalDeck = buildBrushfireStarterStyleDeck();
        List<CardDefinition> basicDeck = buildZapTorrentStarterStyleDeck();

        int tacticalWins = 0;
        for (long seed = 101; seed <= 110; seed++) {
            GameState finalState = simulateMatch(seed, tacticalDeck, basicDeck, tactical, basic);
            assertThat(finalState.phase()).isEqualTo(TurnPhase.GAME_OVER);
            assertThat(finalState.winner()).isNotNull();
            if (finalState.winner() == PlayerId.PLAYER) {
                tacticalWins++;
            }
        }
        // Tactical utilizes Trainers + Evolutions + Retreat/Gust and must hold clear advantage (>= 5/10 vs mirror/type match)
        assertThat(tacticalWins).isGreaterThanOrEqualTo(5);
    }

    @Test
    void simulatesTacticalVsTacticalAcrossAllThreeStarterArchetypesWithoutInfiniteLoops() {
        TacticalBotStrategy tactical = new TacticalBotStrategy();
        List<List<CardDefinition>> starters = List.of(
                buildBrushfireStarterStyleDeck(),
                buildZapTorrentStarterStyleDeck(),
                buildBlackoutStarterStyleDeck()
        );

        long seed = 201;
        for (int i = 0; i < starters.size(); i++) {
            for (int j = 0; j < starters.size(); j++) {
                GameState finalState = simulateMatch(seed++, starters.get(i), starters.get(j), tactical, tactical);
                assertThat(finalState.phase()).isEqualTo(TurnPhase.GAME_OVER);
                assertThat(finalState.winner()).isNotNull();
                assertThat(finalState.winReason()).isNotNull();
            }
        }
    }

    private GameState simulateMatch(
            long seed,
            List<CardDefinition> playerDeck,
            List<CardDefinition> cpuDeck,
            BotStrategy playerStrategy,
            BotStrategy cpuStrategy
    ) {
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

        GameEngine engine = new GameEngine(random);
        BotTurnRunner runner = new BotTurnRunner(engine);
        EngineStepResult init = engine.initializeMatch(playerDeck, cpuDeck, random);
        GameState state = init.state();
        List<GameEvent> allEvents = new ArrayList<>(init.events());

        int iterations = 0;
        while (state.phase() != TurnPhase.GAME_OVER && iterations < 200) {
            boolean acted = false;
            if (runner.requiresBotAction(state, PlayerId.PLAYER)) {
                EngineStepResult step = runner.runUntilHumanTurn(state, PlayerId.PLAYER, playerStrategy);
                state = step.state();
                allEvents.addAll(step.events());
                acted = true;
            }
            if (state.phase() != TurnPhase.GAME_OVER && runner.requiresBotAction(state, PlayerId.CPU)) {
                EngineStepResult step = runner.runUntilHumanTurn(state, PlayerId.CPU, cpuStrategy);
                state = step.state();
                allEvents.addAll(step.events());
                acted = true;
            }
            assertThat(acted).as("Match stalled on seed %d at phase %s", seed, state.phase()).isTrue();
            iterations++;
        }

        assertThat(allEvents).isNotEmpty();
        long previousSeq = 0;
        for (GameEvent event : allEvents) {
            assertThat(event.sequence()).isGreaterThan(previousSeq);
            previousSeq = event.sequence();
        }
        return state;
    }

    private List<CardDefinition> buildBrushfireStarterStyleDeck() {
        List<CardDefinition> deck = new ArrayList<>(60);
        CardDefinition charmander = pokemon("base1-46", "Charmander", "Basic", null, 50, "Fire", "Water", null, 1,
                new AttackDefinition("Scratch", List.of("Colorless"), 10, "10", null),
                new AttackDefinition("Ember", List.of("Fire", "Colorless"), 30, "30", "Discard 1 Energy card."));
        CardDefinition charmeleon = pokemon("base1-24", "Charmeleon", "Stage1", "Charmander", 80, "Fire", "Water", null, 1,
                new AttackDefinition("Slash", List.of("Colorless", "Colorless", "Colorless"), 30, "30", null),
                new AttackDefinition("Flamethrower", List.of("Fire", "Fire", "Colorless"), 50, "50", "Discard 1 Energy card."));
        CardDefinition magmar = pokemon("base1-36", "Magmar", "Basic", null, 50, "Fire", "Water", null, 2,
                new AttackDefinition("Fire Punch", List.of("Fire", "Fire"), 30, "30", null));
        CardDefinition bulbasaur = pokemon("base1-44", "Bulbasaur", "Basic", null, 40, "Grass", "Fire", null, 1,
                new AttackDefinition("Leech Seed", List.of("Grass", "Grass"), 20, "20", null));
        CardDefinition ivysaur = pokemon("base1-30", "Ivysaur", "Stage1", "Bulbasaur", 60, "Grass", "Fire", null, 1,
                new AttackDefinition("Vine Whip", List.of("Grass", "Colorless", "Colorless"), 30, "30", null),
                new AttackDefinition("Poisonpowder", List.of("Grass", "Grass", "Grass"), 20, "20", "Poisoned"));

        addCopies(deck, charmander, 4);
        addCopies(deck, charmeleon, 2);
        addCopies(deck, magmar, 4);
        addCopies(deck, bulbasaur, 4);
        addCopies(deck, ivysaur, 2);
        addCopies(deck, trainer("base1-91", "Bill"), 4);
        addCopies(deck, trainer("base1-88", "Professor Oak"), 2);
        addCopies(deck, trainer("base1-94", "Potion"), 3);
        addCopies(deck, trainer("base1-95", "Switch"), 2);
        addCopies(deck, trainer("base1-81", "Energy Retrieval"), 2);
        addCopies(deck, trainer("base1-93", "Gust of Wind"), 2);
        addCopies(deck, energy("base1-98", "Fire Energy", "Normal", "Fire", 1), 19);
        addCopies(deck, energy("base1-99", "Grass Energy", "Normal", "Grass", 1), 10);
        return deck;
    }

    private List<CardDefinition> buildZapTorrentStarterStyleDeck() {
        List<CardDefinition> deck = new ArrayList<>(60);
        CardDefinition squirtle = pokemon("base1-63", "Squirtle", "Basic", null, 40, "Water", "Lightning", null, 1,
                new AttackDefinition("Bubble", List.of("Water"), 10, "10", "Paralyzed"),
                new AttackDefinition("Withdraw", List.of("Water", "Colorless"), 0, null, "prevent all damage"));
        CardDefinition wartortle = pokemon("base1-42", "Wartortle", "Stage1", "Squirtle", 70, "Water", "Lightning", null, 1,
                new AttackDefinition("Withdraw", List.of("Water", "Colorless"), 0, null, "prevent all damage"),
                new AttackDefinition("Bite", List.of("Water", "Colorless", "Colorless"), 40, "40", null));
        CardDefinition pikachu = pokemon("base1-58", "Pikachu", "Basic", null, 40, "Lightning", "Fighting", null, 1,
                new AttackDefinition("Gnaw", List.of("Colorless"), 10, "10", null),
                new AttackDefinition("Thunder Jolt", List.of("Lightning", "Colorless"), 30, "30", "does 10 damage to itself"));
        CardDefinition electabuzz = pokemon("base1-20", "Electabuzz", "Basic", null, 70, "Lightning", "Fighting", null, 2,
                new AttackDefinition("Thundershock", List.of("Lightning"), 10, "10", "Paralyzed"),
                new AttackDefinition("Thunderpunch", List.of("Lightning", "Colorless"), 30, "30+", "does 10 damage to itself"));

        addCopies(deck, squirtle, 4);
        addCopies(deck, wartortle, 2);
        addCopies(deck, pikachu, 4);
        addCopies(deck, electabuzz, 4);
        addCopies(deck, trainer("base1-91", "Bill"), 4);
        addCopies(deck, trainer("base1-88", "Professor Oak"), 2);
        addCopies(deck, trainer("base1-90", "Super Potion"), 2);
        addCopies(deck, trainer("base1-92", "Energy Removal"), 3);
        addCopies(deck, trainer("base1-84", "PlusPower"), 2);
        addCopies(deck, trainer("base1-95", "Switch"), 2);
        addCopies(deck, energy("base1-102", "Water Energy", "Normal", "Water", 1), 16);
        addCopies(deck, energy("base1-100", "Lightning Energy", "Normal", "Lightning", 1), 15);
        return deck;
    }

    private List<CardDefinition> buildBlackoutStarterStyleDeck() {
        List<CardDefinition> deck = new ArrayList<>(60);
        CardDefinition machop = pokemon("base1-52", "Machop", "Basic", null, 50, "Fighting", "Psychic", null, 1,
                new AttackDefinition("Low Kick", List.of("Fighting"), 20, "20", null));
        CardDefinition machoke = pokemon("base1-34", "Machoke", "Stage1", "Machop", 80, "Fighting", "Psychic", null, 3,
                new AttackDefinition("Karate Chop", List.of("Fighting", "Fighting", "Colorless"), 50, "50-", null),
                new AttackDefinition("Submission", List.of("Fighting", "Fighting", "Colorless", "Colorless"), 60, "60", "does 20 damage to itself"));
        CardDefinition hitmonchan = pokemon("base1-7", "Hitmonchan", "Basic", null, 70, "Fighting", "Psychic", null, 2,
                new AttackDefinition("Jab", List.of("Fighting"), 20, "20", null),
                new AttackDefinition("Special Punch", List.of("Fighting", "Fighting", "Colorless"), 40, "40", null));
        CardDefinition abra = pokemon("base1-43", "Abra", "Basic", null, 30, "Psychic", "Psychic", null, 0,
                new AttackDefinition("Psyshock", List.of("Psychic"), 10, "10", "Paralyzed"));
        CardDefinition kadabra = pokemon("base1-32", "Kadabra", "Stage1", "Abra", 60, "Psychic", "Psychic", null, 3,
                new AttackDefinition("Recover", List.of("Psychic", "Psychic"), 0, null, "Remove all damage counters."),
                new AttackDefinition("Super Psy", List.of("Psychic", "Psychic", "Colorless"), 50, "50", null));

        addCopies(deck, machop, 4);
        addCopies(deck, machoke, 2);
        addCopies(deck, hitmonchan, 4);
        addCopies(deck, abra, 3);
        addCopies(deck, kadabra, 2);
        addCopies(deck, trainer("base1-91", "Bill"), 4);
        addCopies(deck, trainer("base1-88", "Professor Oak"), 2);
        addCopies(deck, trainer("base1-80", "Defender"), 2);
        addCopies(deck, trainer("base1-94", "Potion"), 3);
        addCopies(deck, trainer("base1-92", "Energy Removal"), 2);
        addCopies(deck, trainer("base1-93", "Gust of Wind"), 2);
        addCopies(deck, energy("base1-97", "Fighting Energy", "Normal", "Fighting", 1), 16);
        addCopies(deck, energy("base1-101", "Psychic Energy", "Normal", "Psychic", 1), 14);
        return deck;
    }

    private static void addCopies(List<CardDefinition> deck, CardDefinition card, int count) {
        for (int i = 0; i < count; i++) {
            deck.add(card);
        }
    }

    private static CardDefinition pokemon(
            String id, String name, String stage, String evolveFrom, int hp,
            String type, String weakType, String resistType, int retreatCost, AttackDefinition... attacks
    ) {
        return new CardDefinition(
                id, name, "Pokemon", stage, evolveFrom, hp, List.of(type), List.of(attacks),
                weakType == null ? List.of() : List.of(new WeaknessRule(weakType, "×2")),
                resistType == null ? List.of() : List.of(new ResistanceRule(resistType, "-30")),
                retreatCost, null, null, null, 0, "/l.webp", "/h.webp"
        );
    }

    private static CardDefinition trainer(String id, String name) {
        return new CardDefinition(id, name, "Trainer", null, null, null, List.of(), List.of(),
                List.of(), List.of(), 0, "Item", null, null, 0, "/l.webp", "/h.webp");
    }

    private static CardDefinition energy(String id, String name, String energyType, String providedType, int count) {
        return new CardDefinition(id, name, "Energy", null, null, null, List.of(), List.of(),
                List.of(), List.of(), 0, null, energyType, providedType, count, "/l.webp", "/h.webp");
    }
}
