package com.pokemontcg.engine;

import com.pokemontcg.engine.action.AttachEnergyAction;
import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.ConcedeAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.PlayBasicAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.SelectSetupAction;
import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameEngineTest {

    private final GameEngine engine = new GameEngine();

    @Test
    void rejectsInvalidDeckSizesOrMissingBasicPokemon() {
        List<CardDefinition> shortDeck = buildFireDeck(10).subList(0, 10);
        List<CardDefinition> energyOnlyDeck = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            energyOnlyDeck.add(EngineTestFixtures.fireEnergy());
        }

        RandomProvider noOpRandom = fixedOrderRandom();
        assertThatThrownBy(() -> engine.initializeMatch(shortDeck, buildWaterDeck(10), noOpRandom))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("60");

        assertThatThrownBy(() -> engine.initializeMatch(energyOnlyDeck, buildWaterDeck(10), noOpRandom))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("Básico");
    }

    @Test
    void handlesNetMulliganBonusDrawsOnSetupCompletion() {
        // Put 10 Fire Energy first so no Basic on top 7 unless shifted by scripted shuffles.
        // Deck structure: 0..6 = energies (mulligan), then Basics later.
        List<CardDefinition> playerDeck = buildDeckWithDelayedBasics(6); // 6 basics at end
        List<CardDefinition> cpuDeck = buildDeckWithDelayedBasics(6);

        // Scripted random:
        // Player shuffles twice putting energies on top (1 mulligan? wait:
        // attempt 1 -> keep energies first -> mulligan #1
        // attempt 2 -> rotate basic into top 7 -> success! (player mulligans = 1)
        // CPU shuffles 4 times:
        // attempt 1 -> energies on top -> mulligan #1
        // attempt 2 -> energies on top -> mulligan #2
        // attempt 3 -> energies on top -> mulligan #3
        // attempt 4 -> rotate basic into top 7 -> success! (cpu mulligans = 3)
        AtomicInteger shuffleCall = new AtomicInteger(0);
        RandomProvider scripted = new RandomProvider() {
            @Override
            public void shuffle(List<?> items) {
                int call = shuffleCall.incrementAndGet();
                if (call == 2 || call == 6) {
                    rotateLastToFront(items, 7);
                }
            }

            @Override
            public boolean flipCoin() {
                return true;
            }
        };

        EngineStepResult init = engine.initializeMatch(playerDeck, cpuDeck, PlayerId.PLAYER, scripted);
        assertThat(init.state().phase()).isEqualTo(TurnPhase.SETUP);
        assertThat(init.state().player(PlayerId.PLAYER).mulliganCount()).isEqualTo(1);
        assertThat(init.state().player(PlayerId.CPU).mulliganCount()).isEqualTo(3);

        // Complete setup for both
        String pBasic = firstBasicInHand(init.state().player(PlayerId.PLAYER)).instanceId();
        EngineStepResult pSetup = engine.applyAction(init.state(), PlayerId.PLAYER, new SelectSetupAction(pBasic, List.of()));
        assertThat(pSetup.state().phase()).isEqualTo(TurnPhase.SETUP);

        String cBasic = firstBasicInHand(pSetup.state().player(PlayerId.CPU)).instanceId();
        EngineStepResult cSetup = engine.applyAction(pSetup.state(), PlayerId.CPU, new SelectSetupAction(cBasic, List.of()));

        // Net mulligan difference: PLAYER gets (3 - 1) = 2 bonus cards + 1 Turn 1 draw!
        // Started with 7, played 1 active (= 6), + 2 net mulligan + 1 Turn 1 draw = 9 in hand.
        assertThat(cSetup.state().phase()).isEqualTo(TurnPhase.MAIN_PHASE);
        assertThat(cSetup.state().turnNumber()).isEqualTo(1);
        assertThat(cSetup.state().activePlayer()).isEqualTo(PlayerId.PLAYER);
        assertThat(cSetup.state().player(PlayerId.PLAYER).hand()).hasSize(9);
        // CPU gets max(0, 1 - 3) = 0 bonus cards; 7 - 1 active = 6 in hand.
        assertThat(cSetup.state().player(PlayerId.CPU).hand()).hasSize(6);
        assertThat(cSetup.state().player(PlayerId.PLAYER).prizes()).hasSize(6);
        assertThat(cSetup.state().player(PlayerId.CPU).prizes()).hasSize(6);
    }

    @Test
    void capsNetMulliganExtraDrawsToRemainingDeckSize() {
        // Setup draws up to 6 prizes first; keep 8 cards in player deck (6 prizes + 2 left for draw)
        PlayerState pStateEightDeck = new PlayerState(
                List.of(
                        new CardInstance("p-d1", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d2", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d3", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d4", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d5", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d6", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d7", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d8", EngineTestFixtures.fireEnergy())
                ),
                List.of(new CardInstance("p-h1", EngineTestFixtures.charmander())),
                null, List.of(), List.of(), List.of(), false, false, false, 0
        );
        PlayerState cState = new PlayerState(
                List.of(
                        new CardInstance("c-d1", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d2", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d3", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d4", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d5", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d6", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d7", EngineTestFixtures.waterEnergy())
                ),
                List.of(),
                PokemonInPlay.fromBasic(new CardInstance("c-a1", EngineTestFixtures.squirtle()), 0),
                List.of(), List.of(), List.of(), false, false, true, 10
        );
        GameState setup = new GameState(0, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.SETUP, null,
                Map.of(PlayerId.PLAYER, pStateEightDeck, PlayerId.CPU, cState), null, null, 1L);

        // PLAYER completes setup: 6 prizes leave 2 deck cards; net mulligan asks for 10 extra draws -> capped at 2!
        // Then Turn 1 DRAW_PHASE finds deck empty -> declares CPU winner by DECK_OUT without throwing!
        EngineStepResult res = engine.applyAction(setup, PlayerId.PLAYER, new SelectSetupAction("p-h1", List.of()));
        assertThat(res.state().player(PlayerId.PLAYER).hand()).hasSize(2);
        assertThat(res.state().player(PlayerId.PLAYER).deck()).isEmpty();
        assertThat(res.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(res.state().winReason()).isEqualTo(WinReason.DECK_OUT);
    }

    @Test
    void enforcesTurnNumberIncrementPerTurnAndBlocksTurnOneAttack() {
        GameState readyTurn1 = initializedAndSetupReadyGame(PlayerId.PLAYER);
        assertThat(readyTurn1.turnNumber()).isEqualTo(1);
        assertThat(readyTurn1.activePlayer()).isEqualTo(PlayerId.PLAYER);

        // Turn 1 starting player cannot attack
        CardInstance energy = firstEnergyInHand(readyTurn1.player(PlayerId.PLAYER));
        String activeId = readyTurn1.player(PlayerId.PLAYER).active().instanceId();
        GameState withEnergy = engine.applyAction(readyTurn1, PlayerId.PLAYER,
                new AttachEnergyAction(energy.instanceId(), activeId)).state();

        assertThatThrownBy(() -> engine.applyAction(withEnergy, PlayerId.PLAYER, new AttackAction(0)))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("Turno 1");

        // End Turn 1 -> Turn 2 CPU
        GameState turn2 = engine.applyAction(withEnergy, PlayerId.PLAYER, new EndTurnAction()).state();
        assertThat(turn2.turnNumber()).isEqualTo(2);
        assertThat(turn2.activePlayer()).isEqualTo(PlayerId.CPU);

        // Turn 2 CPU CAN attach energy and attack!
        CardInstance cpuEnergy = firstEnergyInHand(turn2.player(PlayerId.CPU));
        String cpuActiveId = turn2.player(PlayerId.CPU).active().instanceId();
        GameState cpuWithEnergy = engine.applyAction(turn2, PlayerId.CPU,
                new AttachEnergyAction(cpuEnergy.instanceId(), cpuActiveId)).state();
        GameState turn3 = engine.applyAction(cpuWithEnergy, PlayerId.CPU, new AttackAction(0)).state();

        // After attack without KO, turn transitions to Turn 3 PLAYER (Squirtle Bubble 10 x 2 Water Weakness = 20)
        assertThat(turn3.turnNumber()).isEqualTo(3);
        assertThat(turn3.activePlayer()).isEqualTo(PlayerId.PLAYER);
        assertThat(turn3.player(PlayerId.PLAYER).active().damage()).isEqualTo(20);
    }

    @Test
    void enforcesFiveBenchLimitAndSingleEnergyPerTurn() {
        GameState turn1 = initializedAndSetupReadyGame(PlayerId.PLAYER);

        // Hand currently has extra Basics (because we seeded 30 Basics + 30 Energies interleaved).
        GameState current = turn1;
        // Fill 5 bench slots
        for (int i = 0; i < 5; i++) {
            CardInstance basic = firstBasicInHand(current.player(PlayerId.PLAYER));
            if (basic != null) {
                current = engine.applyAction(current, PlayerId.PLAYER, new PlayBasicAction(basic.instanceId())).state();
            } else {
                // Ensure we have 5 on bench in test setup
                break;
            }
        }
        assertThat(current.player(PlayerId.PLAYER).bench()).hasSizeLessThanOrEqualTo(5);

        // Explicitly construct state with 5 on bench and 1 Basic in hand to test overflow
        GameState fullBenchState = stateWithBenchSize(current, PlayerId.PLAYER, 5);
        CardInstance extraBasic = firstBasicInHand(fullBenchState.player(PlayerId.PLAYER));
        assertThat(extraBasic).isNotNull();
        assertThatThrownBy(() -> engine.applyAction(fullBenchState, PlayerId.PLAYER, new PlayBasicAction(extraBasic.instanceId())))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("5");

        // Energy limit: 1 per turn
        CardInstance e1 = firstEnergyInHand(current.player(PlayerId.PLAYER));
        String activeId = current.player(PlayerId.PLAYER).active().instanceId();
        GameState afterE1 = engine.applyAction(current, PlayerId.PLAYER, new AttachEnergyAction(e1.instanceId(), activeId)).state();
        CardInstance e2 = firstEnergyInHand(afterE1.player(PlayerId.PLAYER));
        assertThatThrownBy(() -> engine.applyAction(afterE1, PlayerId.PLAYER, new AttachEnergyAction(e2.instanceId(), activeId)))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("una carta de Energía");
    }

    @Test
    void handlesKnockOutPrizeDrawAndMandatoryBenchPromotion() {
        CardDefinition heavyHitter = new CardDefinition(
                "test-1", "Arcanine EX", "Pokemon", "Basic", null, 100,
                List.of("Fire"),
                List.of(new AttackDefinition("Blast", List.of("Fire"), 60, "60", null)),
                List.of(), List.of(), 1, null, null, null, 0, null, null
        );
        CardInstance attackerCard = new CardInstance("p-1", heavyHitter);
        PokemonInPlay attackerActive = new PokemonInPlay(
                attackerCard.instanceId(),
                attackerCard,
                List.of(attackerCard),
                List.of(new CardInstance("p-e1", EngineTestFixtures.fireEnergy())),
                0, java.util.Set.of(), 1, 1
        );

        PokemonInPlay cpuActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 0);
        PokemonInPlay cpuBench = PokemonInPlay.fromBasic(new CardInstance("c-2", EngineTestFixtures.charmander()), 0);

        PlayerState player = new PlayerState(
                List.of(new CardInstance("p-d1", EngineTestFixtures.fireEnergy())),
                List.of(),
                attackerActive,
                List.of(),
                List.of(new CardInstance("p-pr1", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-pr2", EngineTestFixtures.fireEnergy())),
                List.of(),
                false, false, true, 0
        );
        PlayerState cpu = new PlayerState(
                List.of(new CardInstance("c-d1", EngineTestFixtures.waterEnergy())),
                List.of(),
                cpuActive,
                List.of(cpuBench),
                List.of(new CardInstance("c-pr1", EngineTestFixtures.waterEnergy())),
                List.of(),
                false, false, true, 0
        );

        GameState state = new GameState(
                3, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(PlayerId.PLAYER, player, PlayerId.CPU, cpu), null, null, 1L
        );

        EngineStepResult koResult = engine.applyAction(state, PlayerId.PLAYER, new AttackAction(0));
        assertThat(koResult.state().phase()).isEqualTo(TurnPhase.POKEMON_CHECKUP);
        assertThat(koResult.state().awaitingPromotionPlayer()).isEqualTo(PlayerId.CPU);
        assertThat(koResult.state().player(PlayerId.PLAYER).prizes()).hasSize(1);
        assertThat(koResult.state().player(PlayerId.PLAYER).hand()).hasSize(1); // Took 1 prize card
        assertThat(koResult.state().player(PlayerId.CPU).active()).isNull();
        assertThat(koResult.state().player(PlayerId.CPU).discardPile()).hasSize(1);
        assertThat(koResult.events()).extracting(e -> e.type())
                .contains(GameEventType.POKEMON_KNOCKED_OUT, GameEventType.PRIZE_TAKEN);

        // Other actions blocked until CPU promotes
        assertThatThrownBy(() -> engine.applyAction(koResult.state(), PlayerId.CPU, new EndTurnAction()))
                .isInstanceOf(IllegalGameActionException.class);

        EngineStepResult promoted = engine.applyAction(koResult.state(), PlayerId.CPU,
                new PromoteActiveAction(cpuBench.instanceId()));
        assertThat(promoted.state().awaitingPromotionPlayer()).isNull();
        assertThat(promoted.state().phase()).isEqualTo(TurnPhase.MAIN_PHASE);
        assertThat(promoted.state().activePlayer()).isEqualTo(PlayerId.CPU);
        assertThat(promoted.state().turnNumber()).isEqualTo(4);
        assertThat(promoted.state().player(PlayerId.CPU).active().instanceId()).isEqualTo(cpuBench.instanceId());
        assertThat(promoted.state().player(PlayerId.CPU).bench()).isEmpty();
    }

    @Test
    void declaresVictoryByLastPrizeNoBenchDeckOutAndConcede() {
        // 1) Last prize taken -> ALL_PRIZES_TAKEN
        CardDefinition striker = new CardDefinition(
                "test-1", "Striker", "Pokemon", "Basic", null, 100,
                List.of("Fire"),
                List.of(new AttackDefinition("KO Hit", List.of(), 50, "50", null)),
                List.of(), List.of(), 0, null, null, null, 0, null, null
        );
        PokemonInPlay pActive = PokemonInPlay.fromBasic(new CardInstance("p-1", striker), 1);
        PokemonInPlay cActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 0);
        PokemonInPlay cBench = PokemonInPlay.fromBasic(new CardInstance("c-2", EngineTestFixtures.squirtle()), 0);

        GameState lastPrizeState = new GameState(
                3, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(
                        PlayerId.PLAYER, new PlayerState(List.of(), List.of(), pActive, List.of(),
                                List.of(new CardInstance("p-pr1", EngineTestFixtures.fireEnergy())), List.of(),
                                false, false, true, 0),
                        PlayerId.CPU, new PlayerState(List.of(), List.of(), cActive, List.of(cBench),
                                List.of(new CardInstance("c-pr1", EngineTestFixtures.waterEnergy())), List.of(),
                                false, false, true, 0)
                ),
                null, null, 1L
        );
        EngineStepResult winByPrizes = engine.applyAction(lastPrizeState, PlayerId.PLAYER, new AttackAction(0));
        assertThat(winByPrizes.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(winByPrizes.state().winner()).isEqualTo(PlayerId.PLAYER);
        assertThat(winByPrizes.state().winReason()).isEqualTo(WinReason.ALL_PRIZES_TAKEN);

        // 2) Opponent bench empty -> NO_POKEMON_LEFT
        GameState noBenchState = new GameState(
                3, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(
                        PlayerId.PLAYER, new PlayerState(List.of(), List.of(), pActive, List.of(),
                                List.of(new CardInstance("p-pr1", EngineTestFixtures.fireEnergy()),
                                        new CardInstance("p-pr2", EngineTestFixtures.fireEnergy())),
                                List.of(), false, false, true, 0),
                        PlayerId.CPU, new PlayerState(List.of(), List.of(), cActive, List.of(),
                                List.of(new CardInstance("c-pr1", EngineTestFixtures.waterEnergy())), List.of(),
                                false, false, true, 0)
                ),
                null, null, 1L
        );
        EngineStepResult winByNoBench = engine.applyAction(noBenchState, PlayerId.PLAYER, new AttackAction(0));
        assertThat(winByNoBench.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(winByNoBench.state().winner()).isEqualTo(PlayerId.PLAYER);
        assertThat(winByNoBench.state().winReason()).isEqualTo(WinReason.NO_POKEMON_LEFT);

        // 3) Opponent deck empty on turn transition -> DECK_OUT
        EngineStepResult winByDeckOut = engine.applyAction(noBenchState, PlayerId.PLAYER, new EndTurnAction());
        assertThat(winByDeckOut.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(winByDeckOut.state().winner()).isEqualTo(PlayerId.PLAYER);
        assertThat(winByDeckOut.state().winReason()).isEqualTo(WinReason.DECK_OUT);

        // 4) Concede + further action blocked
        EngineStepResult conceded = engine.applyAction(noBenchState, PlayerId.PLAYER, new ConcedeAction());
        assertThat(conceded.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(conceded.state().winner()).isEqualTo(PlayerId.CPU);
        assertThat(conceded.state().winReason()).isEqualTo(WinReason.CONCEDED);

        assertThatThrownBy(() -> engine.applyAction(conceded.state(), PlayerId.CPU, new EndTurnAction()))
                .isInstanceOf(IllegalGameActionException.class);
    }

    private GameState initializedAndSetupReadyGame(PlayerId startingPlayer) {
        EngineStepResult init = engine.initializeMatch(
                buildFireDeck(30),
                buildWaterDeck(30),
                startingPlayer,
                fixedOrderRandom()
        );
        String pActive = firstBasicInHand(init.state().player(PlayerId.PLAYER)).instanceId();
        GameState afterP = engine.applyAction(init.state(), PlayerId.PLAYER, new SelectSetupAction(pActive, List.of())).state();
        String cActive = firstBasicInHand(afterP.player(PlayerId.CPU)).instanceId();
        return engine.applyAction(afterP, PlayerId.CPU, new SelectSetupAction(cActive, List.of())).state();
    }

    private GameState stateWithBenchSize(GameState base, PlayerId playerId, int benchCount) {
        PlayerState ps = base.player(playerId);
        List<PokemonInPlay> bench = new ArrayList<>();
        for (int i = 0; i < benchCount; i++) {
            bench.add(PokemonInPlay.fromBasic(new CardInstance("extra-b-" + i, EngineTestFixtures.charmander()), 1));
        }
        List<CardInstance> hand = new ArrayList<>(ps.hand());
        hand.add(new CardInstance("extra-h-1", EngineTestFixtures.charmander()));
        return base.withPlayer(playerId, new PlayerState(
                ps.deck(), hand, ps.active(), bench, ps.prizes(), ps.discardPile(),
                ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(), ps.mulliganCount()
        ));
    }

    private List<CardDefinition> buildFireDeck(int pokemonCount) {
        List<CardDefinition> deck = new ArrayList<>(60);
        for (int i = 0; i < 60; i++) {
            if (i % 2 == 0 && pokemonCount > 0) {
                deck.add(EngineTestFixtures.charmander());
                pokemonCount--;
            } else {
                deck.add(EngineTestFixtures.fireEnergy());
            }
        }
        return deck;
    }

    private List<CardDefinition> buildWaterDeck(int pokemonCount) {
        List<CardDefinition> deck = new ArrayList<>(60);
        for (int i = 0; i < 60; i++) {
            if (i % 2 == 0 && pokemonCount > 0) {
                deck.add(EngineTestFixtures.squirtle());
                pokemonCount--;
            } else {
                deck.add(EngineTestFixtures.waterEnergy());
            }
        }
        return deck;
    }

    private List<CardDefinition> buildDeckWithDelayedBasics(int basicCount) {
        List<CardDefinition> deck = new ArrayList<>(60);
        for (int i = 0; i < 60 - basicCount; i++) {
            deck.add(EngineTestFixtures.fireEnergy());
        }
        for (int i = 0; i < basicCount; i++) {
            deck.add(EngineTestFixtures.charmander());
        }
        return deck;
    }

    private CardInstance firstBasicInHand(PlayerState ps) {
        return ps.hand().stream().filter(c -> c.definition().basicPokemon()).findFirst().orElse(null);
    }

    private CardInstance firstEnergyInHand(PlayerState ps) {
        return ps.hand().stream().filter(c -> c.definition().energyCard()).findFirst().orElseThrow();
    }

    private RandomProvider fixedOrderRandom() {
        return new RandomProvider() {
            @Override
            public void shuffle(List<?> items) {
                // Keep deterministic order for unit tests
            }

            @Override
            public boolean flipCoin() {
                return true;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void rotateLastToFront(List items, int count) {
        for (int i = 0; i < count; i++) {
            Object last = items.remove(items.size() - 1);
            items.add(0, last);
        }
    }
}
