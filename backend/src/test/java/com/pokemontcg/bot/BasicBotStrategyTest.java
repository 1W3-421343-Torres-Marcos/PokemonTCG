package com.pokemontcg.bot;

import com.pokemontcg.engine.action.AttachEnergyAction;
import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.action.PlayBasicAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.SelectSetupAction;
import com.pokemontcg.engine.dto.AttackViewDTO;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.dto.OpponentViewDTO;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PokemonViewDTO;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicBotStrategyTest {

    private final BasicBotStrategy strategy = new BasicBotStrategy();

    @Test
    void setupSelectsHighestHpBasicAsActiveAndBenchesRemainingBasics() {
        CardViewDTO lowHp = basicCard("c-1", "Squirtle", 40, 10);
        CardViewDTO highHp = basicCard("c-2", "Magmar", 70, 30);
        CardViewDTO energy = energyCard("c-3", "Fire Energy", "Fire");

        PlayerViewDTO view = baseView(
                TurnPhase.SETUP, false, null, false, false,
                List.of(lowHp, highHp, energy), null, List.of()
        );

        GameAction action = strategy.decideNextAction(view);
        assertThat(action).isInstanceOf(SelectSetupAction.class);
        SelectSetupAction setup = (SelectSetupAction) action;
        assertThat(setup.activeInstanceId()).isEqualTo("c-2");
        assertThat(setup.benchInstanceIds()).containsExactly("c-1");
    }

    @Test
    void mainPhaseFollowsPriorityBenchThenMatchingEnergyThenHighestPayableAttackThenEndTurn() {
        CardViewDTO benchBasic = basicCard("c-basic", "Charmander", 50, 30);
        CardViewDTO waterEnergy = energyCard("c-ew", "Water Energy", "Water");
        CardViewDTO fireEnergy = energyCard("c-ef", "Fire Energy", "Fire");

        PokemonViewDTO activeNeedsFire = new PokemonViewDTO(
                "c-active",
                basicCard("c-active", "Charmander", 50, 30),
                50, 50, 0,
                List.of(),
                List.of(),
                List.of(
                        new AttackViewDTO(0, "Scratch", List.of("Colorless"), 10, "10", null, false),
                        new AttackViewDTO(1, "Ember", List.of("Fire", "Colorless"), 30, "30", null, false)
                ),
                0, 0
        );

        // 1. Plays Basic to Bench first
        PlayerViewDTO withBasicView = baseView(
                TurnPhase.MAIN_PHASE, true, null, false, true,
                List.of(waterEnergy, fireEnergy, benchBasic), activeNeedsFire, List.of()
        );
        assertThat(strategy.decideNextAction(withBasicView))
                .isEqualTo(new PlayBasicAction("c-basic"));

        // 2. Without Basics in hand, attaches Fire Energy (matching Ember cost) rather than Water Energy
        PlayerViewDTO attachView = baseView(
                TurnPhase.MAIN_PHASE, true, null, false, true,
                List.of(waterEnergy, fireEnergy), activeNeedsFire, List.of()
        );
        assertThat(strategy.decideNextAction(attachView))
                .isEqualTo(new AttachEnergyAction("c-ef", "c-active"));

        // 3. When energy already attached and both attacks payable, picks highest baseDamage attack (30 > 10)
        PokemonViewDTO fullyPoweredActive = new PokemonViewDTO(
                "c-active",
                basicCard("c-active", "Charmander", 50, 30),
                50, 50, 0,
                List.of(fireEnergy, waterEnergy),
                List.of(),
                List.of(
                        new AttackViewDTO(0, "Scratch", List.of("Colorless"), 10, "10", null, true),
                        new AttackViewDTO(1, "Ember", List.of("Fire", "Colorless"), 30, "30", null, true)
                ),
                0, 0
        );
        PlayerViewDTO attackView = baseView(
                TurnPhase.MAIN_PHASE, true, null, true, true,
                List.of(), fullyPoweredActive, List.of()
        );
        assertThat(strategy.decideNextAction(attackView))
                .isEqualTo(new AttackAction(1));

        // 4. On Turn 1 (canAttackThisTurn == false) with energy already attached -> EndTurnAction
        PlayerViewDTO turn1EndView = baseView(
                TurnPhase.MAIN_PHASE, true, null, true, false,
                List.of(), fullyPoweredActive, List.of()
        );
        assertThat(strategy.decideNextAction(turn1EndView))
                .isEqualTo(new EndTurnAction());
    }

    @Test
    void promotesBestBenchPokemonAfterKnockOut() {
        PokemonViewDTO weakBench = new PokemonViewDTO(
                "c-b1", basicCard("c-b1", "Rattata", 30, 10),
                30, 30, 0, List.of(), List.of(), List.of(), 0, 0
        );
        PokemonViewDTO poweredBench = new PokemonViewDTO(
                "c-b2", basicCard("c-b2", "Magmar", 50, 50),
                50, 40, 10, List.of(energyCard("c-e1", "Fire Energy", "Fire")), List.of(), List.of(), 0, 0
        );

        PlayerViewDTO promoView = baseView(
                TurnPhase.POKEMON_CHECKUP, true, PlayerId.CPU, true, false,
                List.of(), null, List.of(weakBench, poweredBench)
        );
        assertThat(strategy.decideNextAction(promoView))
                .isEqualTo(new PromoteActiveAction("c-b2"));
    }

    @Test
    void botTurnRunnerEnforcesMaxActionsSafetyLimitAndForcesEndTurn() {
        GameEngine engine = new GameEngine();
        BotTurnRunner cappedRunner = new BotTurnRunner(engine, 2);

        List<CardDefinition> deck = new ArrayList<>();
        CardDefinition basic = new CardDefinition(
                "base1-46", "Charmander", "Pokemon", "Basic", null, 50, List.of("Fire"),
                List.of(new AttackDefinition("Scratch", List.of("Colorless"), 10, "10", null)),
                List.of(), List.of(), 1, null, null, null, 0, null, null
        );
        for (int i = 0; i < 60; i++) {
            deck.add(basic);
        }

        RandomProvider noShuffle = new RandomProvider() {
            @Override
            public void shuffle(List<?> items) {
            }

            @Override
            public boolean flipCoin() {
                return true;
            }
        };

        EngineStepResult init = engine.initializeMatch(deck, deck, PlayerId.CPU, noShuffle);
        // Human completes setup choosing 1 active and 0 bench
        String pActive = init.state().player(PlayerId.PLAYER).hand().get(0).instanceId();
        GameState afterHumanSetup = engine.applyAction(
                init.state(), PlayerId.PLAYER, new SelectSetupAction(pActive, List.of())).state();

        // CPU runs setup (turn 0 -> 1 action), then Turn 1 MAIN_PHASE resets counter and runs at most 2 actions
        // (even if BasicBotStrategy only benches, cappedRunner stops after 2 actions and forces EndTurnAction)
        EngineStepResult botResult = cappedRunner.runUntilHumanTurn(afterHumanSetup, PlayerId.CPU, strategy);
        assertThat(botResult.state().activePlayer()).isEqualTo(PlayerId.PLAYER);
        assertThat(botResult.state().turnNumber()).isEqualTo(2);
    }

    private PlayerViewDTO baseView(
            TurnPhase phase,
            boolean mySetupReady,
            PlayerId awaitingPromotion,
            boolean energyAttachedThisTurn,
            boolean canAttackThisTurn,
            List<CardViewDTO> hand,
            PokemonViewDTO active,
            List<PokemonViewDTO> bench
    ) {
        return new PlayerViewDTO(
                PlayerId.CPU,
                canAttackThisTurn ? 2 : 1,
                PlayerId.CPU,
                PlayerId.PLAYER,
                phase,
                awaitingPromotion,
                energyAttachedThisTurn,
                false,
                canAttackThisTurn,
                mySetupReady,
                true,
                null,
                null,
                hand,
                active,
                bench,
                List.of(),
                40,
                6,
                new OpponentViewDTO(PlayerId.PLAYER, 5, 40, 6, null, List.of(), List.of())
        );
    }

    private CardViewDTO basicCard(String instanceId, String name, int hp, int dmg) {
        return new CardViewDTO(
                instanceId, "base1-x", name, "Pokemon", "Basic", null, hp,
                List.of("Fire"),
                List.of(new AttackViewDTO(0, "Hit", List.of("Colorless"), dmg, String.valueOf(dmg), null, false)),
                List.of(), List.of(), 1, null, null, null, 0, null, null
        );
    }

    private CardViewDTO energyCard(String instanceId, String name, String type) {
        return new CardViewDTO(
                instanceId, "base1-e", name, "Energy", "Basic", null, null,
                List.of(), List.of(), List.of(), List.of(), 0, null, "Basic", type, 1, null, null
        );
    }
}
