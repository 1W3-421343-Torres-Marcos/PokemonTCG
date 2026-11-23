package com.pokemontcg.engine;

import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.RetreatAction;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PlayerViewProjector;
import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.rules.StatusRules;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusConditionCheckupTest {

    @Test
    void enforcesMutualExclusivityBetweenSleepParalysisConfusionAndCoexistsWithPoisonBurn() {
        PokemonInPlay mon = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1);

        mon = StatusRules.applyStatus(mon, StatusCondition.POISONED);
        mon = StatusRules.applyStatus(mon, StatusCondition.BURNED);
        mon = StatusRules.applyStatus(mon, StatusCondition.ASLEEP);
        assertThat(mon.statusConditions()).containsExactlyInAnyOrder(
                StatusCondition.POISONED, StatusCondition.BURNED, StatusCondition.ASLEEP
        );

        mon = StatusRules.applyStatus(mon, StatusCondition.PARALYZED);
        assertThat(mon.statusConditions()).containsExactlyInAnyOrder(
                StatusCondition.POISONED, StatusCondition.BURNED, StatusCondition.PARALYZED
        );

        mon = StatusRules.applyStatus(mon, StatusCondition.CONFUSED);
        assertThat(mon.statusConditions()).containsExactlyInAnyOrder(
                StatusCondition.POISONED, StatusCondition.BURNED, StatusCondition.CONFUSED
        );
    }

    @Test
    void blocksAttackAndRetreatWhileAsleepOrParalyzedAndCuresParalysisAtEndOfOwnTurn() {
        GameEngine deterministicHeads = new GameEngine(fixedCoins(false)); // sleep coin stays tails

        PokemonInPlay paralyzed = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1)
                .withAttachedEnergies(List.of(new CardInstance("p-e1", EngineTestFixtures.fireEnergy())))
                .withStatusConditions(Set.of(StatusCondition.PARALYZED));
        PokemonInPlay bench = PokemonInPlay.fromBasic(new CardInstance("p-b1", EngineTestFixtures.squirtle()), 1);

        GameState state = stateWithActives(3, PlayerId.PLAYER, paralyzed, List.of(bench),
                PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 0), List.of(), 2, 2);

        PlayerViewDTO view = PlayerViewProjector.project(state, PlayerId.PLAYER);
        assertThat(view.canAttackThisTurn()).isFalse();
        assertThat(view.canRetreat()).isFalse();

        assertThatThrownBy(() -> deterministicHeads.applyAction(state, PlayerId.PLAYER, new AttackAction(0)))
                .isInstanceOf(IllegalGameActionException.class);
        assertThatThrownBy(() -> deterministicHeads.applyAction(state, PlayerId.PLAYER, new RetreatAction("p-b1", List.of())))
                .isInstanceOf(IllegalGameActionException.class);

        // Ending turn clears PLAYER's own PARALYZED condition during POKEMON_CHECKUP
        EngineStepResult ended = deterministicHeads.applyAction(state, PlayerId.PLAYER, new EndTurnAction());
        assertThat(ended.state().player(PlayerId.PLAYER).active().statusConditions()).isEmpty();
        assertThat(ended.state().turnNumber()).isEqualTo(4);
    }

    @Test
    void handlesConfusionCoinFlipHeadsAndTailsSelfDamagePlusBurnSleepCheckup() {
        // Coin 1: false (Confusion tails -> 30 self dmg, no attack)
        // Coin 2: true (Burn checkup heads -> recovers from Burn)
        // Coin 3: true (Confusion heads on next turn -> attack succeeds)
        GameEngine engine = new GameEngine(fixedCoins(false, true, true));

        PokemonInPlay confusedAndBurned = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1) // 50 HP
                .withAttachedEnergies(List.of(new CardInstance("p-e1", EngineTestFixtures.fireEnergy())))
                .withStatusConditions(Set.of(StatusCondition.CONFUSED, StatusCondition.BURNED));

        PokemonInPlay cpuActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 0);

        GameState state = stateWithActives(3, PlayerId.PLAYER, confusedAndBurned, List.of(), cpuActive, List.of(), 2, 2);
        // 50 HP, takes 30 confusion self-damage + 20 Burn checkup = 50 -> KO!
        EngineStepResult tailsResult = engine.applyAction(state, PlayerId.PLAYER, new AttackAction(0));
        assertThat(tailsResult.events()).extracting(e -> e.type())
                .contains(GameEventType.COIN_FLIPPED, GameEventType.DAMAGE_DEALT, GameEventType.CHECKUP_TICK, GameEventType.POKEMON_KNOCKED_OUT);
        // CPU active remained unharmed because Confusion failed
        assertThat(tailsResult.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(tailsResult.state().winner()).isEqualTo(PlayerId.CPU);
        assertThat(tailsResult.state().winReason()).isEqualTo(WinReason.NO_POKEMON_LEFT);
    }

    @Test
    void doesNotDoubleRunCheckupWhenPromotingAfterCheckupKnockOut() {
        // Test exact adjustment #1:
        // PLAYER active is POISONED with 10 remaining HP (dies in Checkup -> PLAYER promotes bench).
        // CPU active is ALSO POISONED with 50/50 HP -> suffers ONLY ONE 10 dmg poison tick (NOT 20!).
        GameEngine engine = new GameEngine(fixedCoins(true));

        PokemonInPlay doomedPlayer = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1)
                .withDamage(40)
                .withStatusConditions(Set.of(StatusCondition.POISONED));
        PokemonInPlay benchPlayer = PokemonInPlay.fromBasic(new CardInstance("p-b1", EngineTestFixtures.magmar()), 1);

        PokemonInPlay poisonedCpu = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.magmar()), 1)
                .withStatusConditions(Set.of(StatusCondition.POISONED)); // 50 HP

        GameState state = stateWithActives(3, PlayerId.PLAYER, doomedPlayer, List.of(benchPlayer),
                poisonedCpu, List.of(), 2, 2);

        EngineStepResult endTurn = engine.applyAction(state, PlayerId.PLAYER, new EndTurnAction());
        assertThat(endTurn.state().phase()).isEqualTo(TurnPhase.POKEMON_CHECKUP);
        assertThat(endTurn.state().awaitingPromotionPlayer()).isEqualTo(PlayerId.PLAYER);
        assertThat(endTurn.state().player(PlayerId.CPU).active().damage()).isEqualTo(10); // First checkup tick

        // PLAYER promotes bench; Checkup MUST NOT run a second time on CPU active!
        EngineStepResult promoted = engine.applyAction(endTurn.state(), PlayerId.PLAYER, new PromoteActiveAction("p-b1"));
        assertThat(promoted.state().phase()).isEqualTo(TurnPhase.MAIN_PHASE);
        assertThat(promoted.state().activePlayer()).isEqualTo(PlayerId.CPU);
        assertThat(promoted.state().turnNumber()).isEqualTo(4);
        assertThat(promoted.state().player(PlayerId.CPU).active().damage()).isEqualTo(10); // Still 10, not 20!
    }

    @Test
    void handlesSimultaneousCheckupKnockOutsWithChainedPromotionsAndDeterministicTieBreakWin() {
        GameEngine engine = new GameEngine(fixedCoins(false));

        // 1) Both Actives die in Checkup simultaneously; both have bench & 2 prizes -> chained double promotion
        PokemonInPlay pDoomed = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1)
                .withDamage(40).withStatusConditions(Set.of(StatusCondition.POISONED));
        PokemonInPlay pBench = PokemonInPlay.fromBasic(new CardInstance("p-b1", EngineTestFixtures.magmar()), 1);

        PokemonInPlay cDoomed = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 1)
                .withDamage(30).withStatusConditions(Set.of(StatusCondition.POISONED));
        PokemonInPlay cBench = PokemonInPlay.fromBasic(new CardInstance("c-b1", EngineTestFixtures.squirtle()), 1);

        GameState state = stateWithActives(3, PlayerId.PLAYER, pDoomed, List.of(pBench),
                cDoomed, List.of(cBench), 2, 2);

        EngineStepResult afterEnd = engine.applyAction(state, PlayerId.PLAYER, new EndTurnAction());
        assertThat(afterEnd.state().phase()).isEqualTo(TurnPhase.POKEMON_CHECKUP);
        assertThat(afterEnd.state().awaitingPromotionPlayer()).isEqualTo(PlayerId.CPU);
        assertThat(afterEnd.state().player(PlayerId.PLAYER).prizes()).hasSize(1);
        assertThat(afterEnd.state().player(PlayerId.CPU).prizes()).hasSize(1);

        // CPU promotes first -> now awaits PLAYER promotion keeping POKEMON_CHECKUP
        EngineStepResult afterCpuPromo = engine.applyAction(afterEnd.state(), PlayerId.CPU, new PromoteActiveAction("c-b1"));
        assertThat(afterCpuPromo.state().phase()).isEqualTo(TurnPhase.POKEMON_CHECKUP);
        assertThat(afterCpuPromo.state().awaitingPromotionPlayer()).isEqualTo(PlayerId.PLAYER);

        // PLAYER promotes second -> jumps cleanly to Turn 4 CPU MAIN_PHASE without re-running Checkup
        EngineStepResult afterPlayerPromo = engine.applyAction(afterCpuPromo.state(), PlayerId.PLAYER, new PromoteActiveAction("p-b1"));
        assertThat(afterPlayerPromo.state().awaitingPromotionPlayer()).isNull();
        assertThat(afterPlayerPromo.state().phase()).isEqualTo(TurnPhase.MAIN_PHASE);
        assertThat(afterPlayerPromo.state().activePlayer()).isEqualTo(PlayerId.CPU);
        assertThat(afterPlayerPromo.state().turnNumber()).isEqualTo(4);

        // 2) Tie-breaker test: both have 1 Prize left -> simultaneous KO empties both prizes -> activePlayer (PLAYER) wins
        GameState tieState = stateWithActives(3, PlayerId.PLAYER, pDoomed, List.of(pBench),
                cDoomed, List.of(cBench), 1, 1);
        EngineStepResult tieWin = engine.applyAction(tieState, PlayerId.PLAYER, new EndTurnAction());
        assertThat(tieWin.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(tieWin.state().winner()).isEqualTo(PlayerId.PLAYER);
        assertThat(tieWin.state().winReason()).isEqualTo(WinReason.ALL_PRIZES_TAKEN);
    }

    private GameState stateWithActives(
            int turnNumber,
            PlayerId activePlayer,
            PokemonInPlay pActive,
            List<PokemonInPlay> pBench,
            PokemonInPlay cActive,
            List<PokemonInPlay> cBench,
            int pPrizes,
            int cPrizes
    ) {
        List<CardInstance> pPrizeList = java.util.stream.IntStream.range(0, pPrizes)
                .mapToObj(i -> new CardInstance("p-pr-" + i, EngineTestFixtures.fireEnergy())).toList();
        List<CardInstance> cPrizeList = java.util.stream.IntStream.range(0, cPrizes)
                .mapToObj(i -> new CardInstance("c-pr-" + i, EngineTestFixtures.waterEnergy())).toList();

        PlayerState player = new PlayerState(
                List.of(new CardInstance("p-d1", EngineTestFixtures.fireEnergy())),
                List.of(), pActive, pBench, pPrizeList, List.of(), false, false, true, 0, 0
        );
        PlayerState cpu = new PlayerState(
                List.of(new CardInstance("c-d1", EngineTestFixtures.waterEnergy())),
                List.of(), cActive, cBench, cPrizeList, List.of(), false, false, true, 0, 0
        );
        return new GameState(turnNumber, activePlayer, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(PlayerId.PLAYER, player, PlayerId.CPU, cpu), null, null, 1L);
    }

    private RandomProvider fixedCoins(Boolean... results) {
        Deque<Boolean> queue = new ArrayDeque<>(List.of(results));
        return new RandomProvider() {
            @Override
            public void shuffle(List<?> items) {
            }

            @Override
            public boolean flipCoin() {
                return queue.isEmpty() || queue.poll();
            }
        };
    }
}
