package com.pokemontcg.engine;

import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.EvolveAction;
import com.pokemontcg.engine.action.PlayBasicAction;
import com.pokemontcg.engine.action.RetreatAction;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PlayerViewProjector;
import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.ActiveModifier;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.ModifierType;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WeaknessRule;
import com.pokemontcg.engine.rules.DamageCalculator;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameEngineEvolutionRetreatDamageTest {

    private final GameEngine engine = new GameEngine();

    @Test
    void supportsStage1AndStage2EvolutionChainEnforcingTurnRestrictionsAndPreservingDamageAndEnergies() {
        CardDefinition charmeleonDef = stagePokemon("base1-24", "Charmeleon", "Stage1", "Charmander", 80, "Fire", 1);
        CardDefinition charizardDef = stagePokemon("base1-4", "Charizard", "Stage2", "Charmeleon", 120, "Fire", 3);

        CardInstance pActiveCard = new CardInstance("p-1", EngineTestFixtures.charmander());
        PokemonInPlay pActive = new PokemonInPlay(
                "p-1",
                pActiveCard,
                List.of(pActiveCard),
                List.of(new CardInstance("p-e1", EngineTestFixtures.fireEnergy())),
                20,
                Set.of(StatusCondition.POISONED),
                0,
                0,
                List.of(new ActiveModifier("Defender", ModifierType.DAMAGE_REDUCTION_20, 20, 2))
        );

        CardInstance charmeleonHand = new CardInstance("p-c2", charmeleonDef);
        CardInstance charizardHand = new CardInstance("p-c3", charizardDef);
        CardInstance freshBasicHand = new CardInstance("p-b2", EngineTestFixtures.charmander());

        GameState turn1State = stateForMainTurn(1, PlayerId.PLAYER, pActive, List.of(),
                List.of(charmeleonHand, charizardHand, freshBasicHand), List.of(), 0);

        // Turn 1: cannot evolve
        PlayerViewDTO turn1View = PlayerViewProjector.project(turn1State, PlayerId.PLAYER);
        assertThat(turn1View.myHand().get(0).playable()).isFalse();
        assertThatThrownBy(() -> engine.applyAction(turn1State, PlayerId.PLAYER,
                new EvolveAction("p-c2", "p-1")))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("primer turno");

        // Advance cleanly to Turn 3 (PLAYER's second turn) with Charmeleon & Charizard ready
        GameState turn3State = stateForMainTurn(3, PlayerId.PLAYER, pActive, List.of(),
                List.of(charmeleonHand, charizardHand, freshBasicHand), List.of(), 0);

        PlayerViewDTO turn3View = PlayerViewProjector.project(turn3State, PlayerId.PLAYER);
        CardViewDTO charmeleonView = turn3View.myHand().stream()
                .filter(c -> c.instanceId().equals("p-c2")).findFirst().orElseThrow();
        CardViewDTO charizardView = turn3View.myHand().stream()
                .filter(c -> c.instanceId().equals("p-c3")).findFirst().orElseThrow();
        assertThat(charmeleonView.playable()).isTrue();
        assertThat(charmeleonView.validTargetIds()).containsExactly("p-1");
        assertThat(charizardView.playable()).isFalse();

        // Also bench a fresh Charmander on Turn 3 -> fresh Charmander CANNOT evolve on Turn 3
        EngineStepResult benched = engine.applyAction(turn3State, PlayerId.PLAYER, new PlayBasicAction("p-b2"));
        PlayerViewDTO afterBenchView = PlayerViewProjector.project(benched.state(), PlayerId.PLAYER);
        CardViewDTO charmeleonAfterBench = afterBenchView.myHand().stream()
                .filter(c -> c.instanceId().equals("p-c2")).findFirst().orElseThrow();
        assertThat(charmeleonAfterBench.validTargetIds()).containsExactly("p-1"); // NOT "p-b2"!

        // Evolve p-1 (Charmander -> Charmeleon)
        EngineStepResult evolved1 = engine.applyAction(benched.state(), PlayerId.PLAYER,
                new EvolveAction("p-c2", "p-1"));
        assertThat(evolved1.events()).extracting(e -> e.type()).contains(GameEventType.POKEMON_EVOLVED);
        PokemonInPlay nowCharmeleon = evolved1.state().player(PlayerId.PLAYER).active();
        assertThat(nowCharmeleon.activeCard().definition().name()).isEqualTo("Charmeleon");
        assertThat(nowCharmeleon.maxHp()).isEqualTo(80);
        assertThat(nowCharmeleon.damage()).isEqualTo(20); // damage preserved
        assertThat(nowCharmeleon.attachedEnergies()).hasSize(1); // energy preserved
        assertThat(nowCharmeleon.statusConditions()).isEmpty(); // status cleared
        assertThat(nowCharmeleon.modifiers()).isEmpty(); // modifiers cleared
        assertThat(nowCharmeleon.evolutionChain()).hasSize(2);

        // Cannot immediately evolve Charmeleon -> Charizard in the SAME Turn 3
        assertThatThrownBy(() -> engine.applyAction(evolved1.state(), PlayerId.PLAYER,
                new EvolveAction("p-c3", "p-1")))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("mismo turno");

        // On Turn 5 (next PLAYER turn), Charmeleon CAN evolve into Charizard
        GameState turn5State = new GameState(
                5, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                evolved1.state().players(), null, null, evolved1.state().nextEventSequence()
        );
        EngineStepResult evolved2 = engine.applyAction(turn5State, PlayerId.PLAYER,
                new EvolveAction("p-c3", "p-1"));
        PokemonInPlay nowCharizard = evolved2.state().player(PlayerId.PLAYER).active();
        assertThat(nowCharizard.activeCard().definition().name()).isEqualTo("Charizard");
        assertThat(nowCharizard.maxHp()).isEqualTo(120);
        assertThat(nowCharizard.evolutionChain()).hasSize(3);
    }

    @Test
    void executesRetreatWithAutomaticAndExplicitEnergyPaymentClearsStatusAndBlocksTwicePerTurn() {
        CardInstance e1 = new CardInstance("p-e1", EngineTestFixtures.fireEnergy());
        CardInstance dce = new CardInstance("p-e2", EngineTestFixtures.doubleColorlessEnergy());

        PokemonInPlay retreatingActive = new PokemonInPlay(
                "p-1",
                new CardInstance("p-1", EngineTestFixtures.magmar()), // retreatCost = 2
                List.of(new CardInstance("p-1", EngineTestFixtures.magmar())),
                List.of(e1, dce),
                30,
                Set.of(StatusCondition.CONFUSED),
                1,
                1,
                List.of(new ActiveModifier("Defender", ModifierType.DAMAGE_REDUCTION_20, 20, 4))
        );
        PokemonInPlay benchMon = PokemonInPlay.fromBasic(new CardInstance("p-2", EngineTestFixtures.charmander()), 1);

        // 1) Automatic retreat payment on cost 2: total 3 units (Fire=1 + DCE=2), picks smallest first then DCE or enough units
        GameState state = stateForMainTurn(3, PlayerId.PLAYER, retreatingActive, List.of(benchMon),
                List.of(), List.of(), 0);
        assertThat(PlayerViewProjector.project(state, PlayerId.PLAYER).canRetreat()).isTrue();

        // Explicit payment using DCE alone (2 units >= retreatCost 2!)
        EngineStepResult retreated = engine.applyAction(state, PlayerId.PLAYER,
                new RetreatAction("p-2", List.of("p-e2")));
        assertThat(retreated.events()).extracting(e -> e.type()).contains(GameEventType.POKEMON_RETREATED);

        PlayerState after = retreated.state().player(PlayerId.PLAYER);
        assertThat(after.active().instanceId()).isEqualTo("p-2");
        assertThat(after.bench()).hasSize(1);
        PokemonInPlay movedToBench = after.bench().get(0);
        assertThat(movedToBench.instanceId()).isEqualTo("p-1");
        assertThat(movedToBench.attachedEnergies()).containsExactly(e1);
        assertThat(movedToBench.statusConditions()).isEmpty();
        assertThat(movedToBench.modifiers()).isEmpty();
        assertThat(after.discardPile()).containsExactly(dce);
        assertThat(after.retreatedThisTurn()).isTrue();
        assertThat(PlayerViewProjector.project(retreated.state(), PlayerId.PLAYER).canRetreat()).isFalse();

        // Double retreat in same turn rejected
        assertThatThrownBy(() -> engine.applyAction(retreated.state(), PlayerId.PLAYER,
                new RetreatAction("p-1", List.of())))
                .isInstanceOf(IllegalGameActionException.class)
                .hasMessageContaining("una vez por turno");
    }

    @Test
    void calculatesWeaknessResistancePlusPowerDefenderHardenAndExpiresModifiersDeterministically() {
        CardDefinition grassWeakFire = new CardDefinition(
                "test-tangela", "Tangela", "Pokemon", "Basic", null, 80,
                List.of("Grass"), List.of(),
                List.of(new WeaknessRule("Fire", "×2")),
                List.of(),
                2, null, null, null, 0, null, null
        );
        CardDefinition fightingRes = new CardDefinition(
                "test-zapdos", "Zapdos", "Pokemon", "Basic", null, 90,
                List.of("Lightning"), List.of(),
                List.of(),
                List.of(new ResistanceRule("Fighting", "-30")),
                3, null, null, null, 0, null, null
        );

        PokemonInPlay fireAttacker = new PokemonInPlay(
                "p-1",
                new CardInstance("p-1", EngineTestFixtures.charmander()), // Fire
                List.of(new CardInstance("p-1", EngineTestFixtures.charmander())),
                List.of(
                        new CardInstance("p-e1", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-e2", EngineTestFixtures.doubleColorlessEnergy())
                ),
                0, Set.of(), 1, 1, List.of()
        );
        PokemonInPlay grassDefender = PokemonInPlay.fromBasic(new CardInstance("c-1", grassWeakFire), 1);

        // Pure Weakness x2: 30 * 2 = 60
        assertThat(DamageCalculator.calculateDamage(fireAttacker, grassDefender, 30, 0)).isEqualTo(60);
        // PlusPower +10 and Defender -20: 60 + 10 - 20 = 50
        PokemonInPlay withDefender = grassDefender.withModifiers(
                List.of(new ActiveModifier("Defender", ModifierType.DAMAGE_REDUCTION_20, 20, 4))
        );
        assertThat(DamageCalculator.calculateDamage(fireAttacker, withDefender, 30, 10)).isEqualTo(50);

        // Resistance -30 floored at 0
        CardDefinition fighter = stagePokemon("base1-7", "Hitmonchan", "Basic", null, 70, "Fighting", 2);
        PokemonInPlay fightAttacker = PokemonInPlay.fromBasic(new CardInstance("p-f", fighter), 1);
        PokemonInPlay zapdosDefender = PokemonInPlay.fromBasic(new CardInstance("c-z", fightingRes), 1);
        assertThat(DamageCalculator.calculateDamage(fightAttacker, zapdosDefender, 20, 0)).isEqualTo(0);
        // 40 - 30 = 10; with PlusPower +10 = 20
        assertThat(DamageCalculator.calculateDamage(fightAttacker, zapdosDefender, 40, 10)).isEqualTo(20);

        // Test GameEngine integration with expiresAfterTurn cleanup:
        // Defender active has Harden (PREVENT_30_OR_LESS) expiring after Turn 4 -> blocks 30 dmg on Turn 4,
        // and once Turn 4 ends -> Turn 5, Harden is purged cleanly!
        PokemonInPlay onix = PokemonInPlay.fromBasic(new CardInstance("c-onix", grassWeakFire), 1)
                .withModifiers(List.of(new ActiveModifier("Harden", ModifierType.PREVENT_30_OR_LESS_NEXT_TURN, 30, 4)));
        // Fire 10 * Weakness 2 = 20 <= 30 -> prevented on Turn 4!
        assertThat(DamageCalculator.calculateDamage(fireAttacker, onix, 10, 0)).isEqualTo(0);

        // Run GameEngine Turn 3 Attack with Fire Ember (30 base) -> deals 60 to Tangela, plus expires Turn 3 modifiers when Turn 4 ends
        GameState turn3 = stateForMainTurn(3, PlayerId.PLAYER, fireAttacker, List.of(),
                List.of(), List.of(onix), 0);
        // On Turn 3, onix has Harden expiring after Turn 4; Charmander Scratch 10 * 2 = 20 <= 30 -> 0 dmg!
        EngineStepResult blockedHit = engine.applyAction(turn3, PlayerId.PLAYER, new AttackAction(0));
        assertThat(blockedHit.state().player(PlayerId.CPU).active().damage()).isEqualTo(0);
        // Turn 4 starts -> CPU ends turn -> closes Turn 4 -> Harden (expiresAfterTurn == 4) is purged!
        EngineStepResult afterCpuTurn = engine.applyAction(blockedHit.state(), PlayerId.CPU, new EndTurnAction());
        assertThat(afterCpuTurn.state().turnNumber()).isEqualTo(5);
        assertThat(afterCpuTurn.state().player(PlayerId.CPU).active().modifiers()).isEmpty();
    }

    private CardDefinition stagePokemon(
            String id, String name, String stage, String evolveFrom, int hp, String type, int retreatCost
    ) {
        return new CardDefinition(
                id, name, "Pokemon", stage, evolveFrom, hp, List.of(type),
                List.of(new AttackDefinition("Attack", List.of("Colorless"), 30, "30", null)),
                List.of(), List.of(), retreatCost, null, null, null, 0, null, null
        );
    }

    private GameState stateForMainTurn(
            int turnNumber,
            PlayerId activePlayer,
            PokemonInPlay playerActive,
            List<PokemonInPlay> playerBench,
            List<CardInstance> playerHand,
            List<PokemonInPlay> cpuSide,
            int plusPowerBonus
    ) {
        PokemonInPlay cpuActive = cpuSide.isEmpty()
                ? PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 0)
                : cpuSide.get(0);

        PlayerState player = new PlayerState(
                List.of(new CardInstance("p-d1", EngineTestFixtures.fireEnergy()),
                        new CardInstance("p-d2", EngineTestFixtures.fireEnergy())),
                playerHand,
                playerActive,
                playerBench,
                List.of(new CardInstance("p-pr1", EngineTestFixtures.fireEnergy())),
                List.of(),
                false,
                false,
                true,
                0,
                plusPowerBonus
        );
        PlayerState cpu = new PlayerState(
                List.of(new CardInstance("c-d1", EngineTestFixtures.waterEnergy()),
                        new CardInstance("c-d2", EngineTestFixtures.waterEnergy())),
                List.of(),
                cpuActive,
                List.of(),
                List.of(new CardInstance("c-pr1", EngineTestFixtures.waterEnergy())),
                List.of(),
                false,
                false,
                true,
                0,
                0
        );

        return new GameState(
                turnNumber, activePlayer, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(PlayerId.PLAYER, player, PlayerId.CPU, cpu), null, null, 1L
        );
    }
}
