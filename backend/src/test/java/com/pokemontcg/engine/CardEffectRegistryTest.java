package com.pokemontcg.engine;

import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.PlayTrainerAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PlayerViewProjector;
import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardEffectRegistryTest {

    @Test
    void trainerBillAndProfessorOakDrawAndDiscardCorrectly() {
        GameEngine engine = new GameEngine(fixedCoins());
        CardInstance bill = new CardInstance("tr-bill", trainerDef("base1-91", "Bill"));
        CardInstance oak = new CardInstance("tr-oak", trainerDef("base1-88", "Professor Oak"));
        CardInstance spare = new CardInstance("sp-1", EngineTestFixtures.charmander());

        List<CardInstance> deck = IntStream.range(1, 12)
                .mapToObj(i -> new CardInstance("d-" + i, EngineTestFixtures.fireEnergy()))
                .toList();

        PokemonInPlay pActive = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1);
        PokemonInPlay cActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 2);

        GameState state = buildState(3, PlayerId.PLAYER, deck, List.of(bill, oak, spare),
                pActive, List.of(), List.of(), cActive, List.of());

        EngineStepResult afterBill = engine.applyAction(state, PlayerId.PLAYER, new PlayTrainerAction("tr-bill", null));
        assertThat(afterBill.state().player(PlayerId.PLAYER).hand()).hasSize(4); // oak, spare, d-1, d-2
        assertThat(afterBill.state().player(PlayerId.PLAYER).deck()).hasSize(9);
        assertThat(afterBill.events()).anyMatch(e -> e.type() == GameEventType.TRAINER_PLAYED);

        EngineStepResult afterOak = engine.applyAction(afterBill.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-oak", null));
        assertThat(afterOak.state().player(PlayerId.PLAYER).hand()).hasSize(7);
        assertThat(afterOak.state().player(PlayerId.PLAYER).deck()).hasSize(2);
        // spare, d-1, d-2 plus oak and bill should now be in discard
        assertThat(afterOak.state().player(PlayerId.PLAYER).discardPile()).hasSize(5);
    }

    @Test
    void potionSuperPotionAndEnergyRetrievalWorkWithProjectedTargets() {
        GameEngine engine = new GameEngine(fixedCoins());
        CardInstance potion = new CardInstance("tr-pot", trainerDef("base1-94", "Potion"));
        CardInstance superPotion = new CardInstance("tr-spot", trainerDef("base1-90", "Super Potion"));
        CardInstance energyRetrieval = new CardInstance("tr-ret", trainerDef("base1-81", "Energy Retrieval"));
        CardInstance discardFodder = new CardInstance("tr-fodder", EngineTestFixtures.squirtle());

        CardInstance fire1 = new CardInstance("e-f1", EngineTestFixtures.fireEnergy());
        CardInstance waterDisc1 = new CardInstance("d-w1", EngineTestFixtures.waterEnergy());
        CardInstance fireDisc2 = new CardInstance("d-f2", EngineTestFixtures.fireEnergy());
        CardInstance doubleColorless = new CardInstance("d-dce", specialEnergyDef("base1-96", "Double Colorless Energy"));

        PokemonInPlay damagedActive = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.magmar()), 1)
                .withDamage(50)
                .withAttachedEnergies(List.of(fire1));
        PokemonInPlay healthyBench = PokemonInPlay.fromBasic(new CardInstance("p-b1", EngineTestFixtures.charmander()), 1);
        PokemonInPlay cActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 2);

        GameState state = buildState(3, PlayerId.PLAYER, List.of(),
                List.of(potion, superPotion, energyRetrieval, discardFodder),
                damagedActive, List.of(healthyBench), List.of(doubleColorless, waterDisc1, fireDisc2), cActive, List.of());

        PlayerViewDTO projected = PlayerViewProjector.project(state, PlayerId.PLAYER);
        CardViewDTO spotView = findHandCard(projected, "tr-spot");
        CardViewDTO retView = findHandCard(projected, "tr-ret");
        assertThat(spotView.playable()).isTrue();
        assertThat(spotView.validTargetIds()).containsExactly("p-1");
        assertThat(retView.playable()).isTrue();
        assertThat(retView.validTargetIds()).containsExactlyInAnyOrder("tr-pot", "tr-spot", "tr-fodder");

        // Super Potion heals 40 (50 -> 10) and discards attached Fire Energy
        EngineStepResult afterSuper = engine.applyAction(state, PlayerId.PLAYER, new PlayTrainerAction("tr-spot", "p-1"));
        assertThat(afterSuper.state().player(PlayerId.PLAYER).active().damage()).isEqualTo(10);
        assertThat(afterSuper.state().player(PlayerId.PLAYER).active().attachedEnergies()).isEmpty();

        // Potion rejects healthy bench, heals active remaining 10 -> 0
        assertThatThrownBy(() -> engine.applyAction(afterSuper.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-pot", "p-b1")))
                .isInstanceOf(IllegalGameActionException.class);
        EngineStepResult afterPot = engine.applyAction(afterSuper.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-pot", "p-1"));
        assertThat(afterPot.state().player(PlayerId.PLAYER).active().damage()).isZero();

        // Energy Retrieval discards tr-fodder and recovers 2 basic energies (skipping DCE)
        EngineStepResult afterRetrieval = engine.applyAction(afterPot.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-ret", "tr-fodder"));
        assertThat(afterRetrieval.state().player(PlayerId.PLAYER).hand())
                .extracting(CardInstance::instanceId)
                .containsExactlyInAnyOrder("d-w1", "d-f2");
        assertThat(afterRetrieval.state().player(PlayerId.PLAYER).discardPile())
                .extracting(CardInstance::instanceId)
                .contains("d-dce", "tr-fodder", "e-f1");
    }

    @Test
    void defenderPlusPowerEnergyRemovalSwitchAndGustOfWindWorkAndExpireProperly() {
        GameEngine engine = new GameEngine(fixedCoins());
        CardInstance defenderCard = new CardInstance("tr-def", trainerDef("base1-80", "Defender"));
        CardInstance plusPower = new CardInstance("tr-pp", trainerDef("base1-84", "PlusPower"));
        CardInstance energyRemoval = new CardInstance("tr-er", trainerDef("base1-92", "Energy Removal"));
        CardInstance gustOfWind = new CardInstance("tr-gust", trainerDef("base1-93", "Gust of Wind"));
        CardInstance switchCard = new CardInstance("tr-sw", trainerDef("base1-95", "Switch"));

        PokemonInPlay pActive = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.magmar()), 1)
                .withAttachedEnergies(List.of(new CardInstance("p-e1", EngineTestFixtures.fireEnergy())))
                .withStatusConditions(Set.of(StatusCondition.PARALYZED));
        PokemonInPlay pBench = PokemonInPlay.fromBasic(new CardInstance("p-b1", EngineTestFixtures.charmander()), 1)
                .withAttachedEnergies(List.of(new CardInstance("p-e2", EngineTestFixtures.fireEnergy())));

        PokemonInPlay cActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 2)
                .withAttachedEnergies(List.of(new CardInstance("c-e1", EngineTestFixtures.waterEnergy())));
        PokemonInPlay cBench = PokemonInPlay.fromBasic(new CardInstance("c-b1", pokemonDef("base1-65", "Staryu", 60, "Water",
                List.of(new AttackDefinition("Slap", List.of("Colorless"), 20, "20", null)))), 2);

        GameState state = buildState(3, PlayerId.PLAYER,
                List.of(new CardInstance("pd1", EngineTestFixtures.fireEnergy()), new CardInstance("pd2", EngineTestFixtures.fireEnergy())),
                List.of(switchCard, defenderCard, plusPower, energyRemoval, gustOfWind),
                pActive, List.of(pBench), List.of(), cActive, List.of(cBench));
        state = state.withPlayer(PlayerId.CPU, new PlayerState(
                List.of(new CardInstance("cd1", EngineTestFixtures.waterEnergy()), new CardInstance("cd2", EngineTestFixtures.waterEnergy())),
                List.of(), cActive, List.of(cBench), state.player(PlayerId.CPU).prizes(), List.of(),
                false, false, true, 0, 0
        ));

        // 1. Switch relieves Paralyzed Magmar for Charmander cleanly without using retreat
        EngineStepResult s1 = engine.applyAction(state, PlayerId.PLAYER, new PlayTrainerAction("tr-sw", "p-b1"));
        assertThat(s1.state().player(PlayerId.PLAYER).active().instanceId()).isEqualTo("p-b1");
        assertThat(s1.state().player(PlayerId.PLAYER).retreatedThisTurn()).isFalse();
        assertThat(s1.state().player(PlayerId.PLAYER).bench().get(0).statusConditions()).isEmpty();

        // 2. Energy Removal discards Water energy from cActive
        EngineStepResult s2 = engine.applyAction(s1.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-er", "c-1"));
        assertThat(s2.state().player(PlayerId.CPU).active().attachedEnergies()).isEmpty();
        assertThat(s2.state().player(PlayerId.CPU).discardPile()).hasSize(1);

        // 3. Gust of Wind forces cBench (Staryu) active
        EngineStepResult s3 = engine.applyAction(s2.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-gust", "c-b1"));
        assertThat(s3.state().player(PlayerId.CPU).active().instanceId()).isEqualTo("c-b1");

        // 4. Defender on Charmander (-20) + PlusPower (+10) -> Scratch deals 10+10=20 damage to Staryu
        EngineStepResult s4 = engine.applyAction(s3.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-def", "p-b1"));
        EngineStepResult s5 = engine.applyAction(s4.state(), PlayerId.PLAYER, new PlayTrainerAction("tr-pp", null));
        EngineStepResult s6 = engine.applyAction(s5.state(), PlayerId.PLAYER, new AttackAction(0));
        assertThat(s6.state().player(PlayerId.CPU).active().damage()).isEqualTo(20);
        assertThat(s6.state().turnNumber()).isEqualTo(4);
        assertThat(s6.state().player(PlayerId.PLAYER).active().modifiers()).hasSize(1);

        // 5. On CPU Turn 4, Slap (20 base) is reduced to 0 by Defender; at end of Turn 4 Defender expires!
        // Attach 1 energy first or Slap costs Colorless (needs 1 energy), attach? Wait Staryu has 0 energy -> EndTurn expires Defender
        EngineStepResult s7 = engine.applyAction(s6.state(), PlayerId.CPU, new EndTurnAction());
        assertThat(s7.state().turnNumber()).isEqualTo(5);
        assertThat(s7.state().player(PlayerId.PLAYER).active().modifiers()).isEmpty();
    }

    @Test
    void starterPokemonAttacksApplyHydroPumpPsychicBarrierLeechSeedHardenThunderpunchAndDoubleEdgeRecoil() {
        // 1. Blastoise Hydro Pump: 3W = 40, 4W = 50, 6W = 60 cap
        GameEngine engine = new GameEngine(fixedCoins(true, false));
        PokemonInPlay blastoise4W = PokemonInPlay.fromBasic(new CardInstance("bl", pokemonDef("base1-2", "Blastoise", 100, "Water",
                        List.of(new AttackDefinition("Hydro Pump", List.of("Water", "Water", "Water"), 40, "40+", null)))), 1)
                .withAttachedEnergies(List.of(
                        new CardInstance("w1", EngineTestFixtures.waterEnergy()),
                        new CardInstance("w2", EngineTestFixtures.waterEnergy()),
                        new CardInstance("w3", EngineTestFixtures.waterEnergy()),
                        new CardInstance("w4", EngineTestFixtures.waterEnergy()),
                        new CardInstance("w5", EngineTestFixtures.waterEnergy()),
                        new CardInstance("w6", EngineTestFixtures.waterEnergy())
                ));
        PokemonInPlay target120 = PokemonInPlay.fromBasic(new CardInstance("t1", pokemonDef("base1-x", "Snorlax", 120, "Colorless", List.of())), 2);
        GameState hydroState = buildState(3, PlayerId.PLAYER, List.of(new CardInstance("d", EngineTestFixtures.waterEnergy())),
                List.of(), blastoise4W, List.of(), List.of(), target120, List.of());
        EngineStepResult hydroRes = engine.applyAction(hydroState, PlayerId.PLAYER, new AttackAction(0));
        assertThat(hydroRes.state().player(PlayerId.CPU).active().damage()).isEqualTo(60); // 40 + 20 cap

        // 2. Electabuzz Thunderpunch: coin 1 (true) => 40 dmg; coin 2 (false) => 30 dmg + 10 self
        PokemonInPlay buzz = PokemonInPlay.fromBasic(new CardInstance("eb", pokemonDef("base1-20", "Electabuzz", 70, "Lightning",
                        List.of(
                                new AttackDefinition("Thundershock", List.of("Lightning"), 10, "10", null),
                                new AttackDefinition("Thunderpunch", List.of("Lightning", "Colorless"), 30, "30+", null)
                        ))), 1)
                .withAttachedEnergies(List.of(
                        new CardInstance("l1", lightningEnergy()),
                        new CardInstance("l2", lightningEnergy())
                ));
        GameState buzzState = buildState(3, PlayerId.PLAYER, List.of(new CardInstance("d", EngineTestFixtures.fireEnergy())),
                List.of(), buzz, List.of(), List.of(), target120, List.of());
        EngineStepResult buzzHeads = engine.applyAction(buzzState, PlayerId.PLAYER, new AttackAction(1));
        assertThat(buzzHeads.state().player(PlayerId.CPU).active().damage()).isEqualTo(40);
        assertThat(buzzHeads.state().player(PlayerId.PLAYER).active().damage()).isZero();

        // Advance CPU turn 4 -> back to turn 5 for second coin (tails)
        EngineStepResult cpuSkip = engine.applyAction(buzzHeads.state(), PlayerId.CPU, new EndTurnAction());
        EngineStepResult buzzTails = engine.applyAction(cpuSkip.state(), PlayerId.PLAYER, new AttackAction(1));
        assertThat(buzzTails.state().player(PlayerId.CPU).active().damage()).isEqualTo(70); // +30
        assertThat(buzzTails.state().player(PlayerId.PLAYER).active().damage()).isEqualTo(10); // 10 self recoil

        // 3. Mewtwo Barrier prevents damage & status next turn, while Psychic scales with attached energies
        GameEngine deterministicEngine = new GameEngine(fixedCoins(true, true));
        PokemonInPlay mewtwo = PokemonInPlay.fromBasic(new CardInstance("m2", pokemonDef("base1-10", "Mewtwo", 60, "Psychic",
                        List.of(
                                new AttackDefinition("Psychic", List.of("Psychic", "Colorless"), 10, "10+", null),
                                new AttackDefinition("Barrier", List.of("Psychic", "Psychic"), 0, null, null)
                        ))), 1)
                .withAttachedEnergies(List.of(
                        new CardInstance("p1", psychicEnergy()),
                        new CardInstance("p2", psychicEnergy()),
                        new CardInstance("p3", psychicEnergy())
                ));
        PokemonInPlay ivysaur = PokemonInPlay.fromBasic(new CardInstance("ivy", pokemonDef("base1-30", "Ivysaur", 60, "Grass",
                        List.of(
                                new AttackDefinition("Vine Whip", List.of("Grass"), 30, "30", null),
                                new AttackDefinition("Poisonpowder", List.of("Grass", "Grass", "Grass"), 20, "20", null)
                        ))), 2)
                .withAttachedEnergies(List.of(
                        new CardInstance("g1", grassEnergy()),
                        new CardInstance("g2", grassEnergy()),
                        new CardInstance("g3", grassEnergy())
                ));
        GameState barrierState = buildState(3, PlayerId.PLAYER, List.of(new CardInstance("d", psychicEnergy())),
                List.of(), mewtwo, List.of(), List.of(), ivysaur, List.of());

        EngineStepResult barrierUsed = deterministicEngine.applyAction(barrierState, PlayerId.PLAYER, new AttackAction(1));
        assertThat(barrierUsed.state().player(PlayerId.PLAYER).active().attachedEnergies()).hasSize(2);
        // CPU Ivysaur tries Poisonpowder -> damage & POISONED both blocked by IMMUNE_NEXT_TURN!
        EngineStepResult ivyBlocked = deterministicEngine.applyAction(barrierUsed.state(), PlayerId.CPU, new AttackAction(1));
        assertThat(ivyBlocked.state().player(PlayerId.PLAYER).active().damage()).isZero();
        assertThat(ivyBlocked.state().player(PlayerId.PLAYER).active().statusConditions()).isEmpty();
        assertThat(ivyBlocked.state().player(PlayerId.PLAYER).active().modifiers()).isEmpty(); // expired after turn 4!

        // Turn 5: Mewtwo uses Psychic against Ivysaur (3 energies attached -> 10 + 30 = 40)
        EngineStepResult psychicHit = deterministicEngine.applyAction(ivyBlocked.state(), PlayerId.PLAYER, new AttackAction(0));
        assertThat(psychicHit.state().player(PlayerId.CPU).active().damage()).isEqualTo(40);
    }

    @Test
    void chanseyDoubleEdgeCausesSimultaneousRecoilKnockOutAndResolvesChainedPromotionsAndWinTieBreak() {
        GameEngine engine = new GameEngine(fixedCoins());
        PokemonInPlay chansey = PokemonInPlay.fromBasic(new CardInstance("ch", pokemonDef("base1-3", "Chansey", 120, "Colorless",
                        List.of(
                                new AttackDefinition("Scrunch", List.of("Colorless", "Colorless"), 0, null, null),
                                new AttackDefinition("Double-edge", List.of("Colorless", "Colorless", "Colorless", "Colorless"), 80, "80", null)
                        ))), 1)
                .withDamage(50) // 70 HP left, 80 recoil will KO!
                .withAttachedEnergies(List.of(
                        new CardInstance("c1", EngineTestFixtures.fireEnergy()),
                        new CardInstance("c2", EngineTestFixtures.fireEnergy()),
                        new CardInstance("c3", EngineTestFixtures.fireEnergy()),
                        new CardInstance("c4", EngineTestFixtures.fireEnergy())
                ));
        PokemonInPlay playerBench = PokemonInPlay.fromBasic(new CardInstance("pb", EngineTestFixtures.charmander()), 1);
        PokemonInPlay cpuActive = PokemonInPlay.fromBasic(new CardInstance("ca", EngineTestFixtures.magmar()), 2); // 50 HP, 80 dmg KOs!
        PokemonInPlay cpuBench = PokemonInPlay.fromBasic(new CardInstance("cb", EngineTestFixtures.squirtle()), 2);

        GameState state = buildState(3, PlayerId.PLAYER, List.of(new CardInstance("pd", EngineTestFixtures.fireEnergy())),
                List.of(), chansey, List.of(playerBench), List.of(), cpuActive, List.of(cpuBench));

        // Double-edge knocks out both -> CPU promotes first, then PLAYER promotes, then POKEMON_CHECKUP advances turn!
        EngineStepResult afterAttack = engine.applyAction(state, PlayerId.PLAYER, new AttackAction(1));
        assertThat(afterAttack.state().phase()).isEqualTo(TurnPhase.ATTACK_PHASE);
        assertThat(afterAttack.state().awaitingPromotionPlayer()).isEqualTo(PlayerId.CPU);
        assertThat(afterAttack.state().player(PlayerId.PLAYER).prizes()).hasSize(5);
        assertThat(afterAttack.state().player(PlayerId.CPU).prizes()).hasSize(5);

        EngineStepResult afterCpuPromo = engine.applyAction(afterAttack.state(), PlayerId.CPU, new PromoteActiveAction("cb"));
        assertThat(afterCpuPromo.state().awaitingPromotionPlayer()).isEqualTo(PlayerId.PLAYER);

        EngineStepResult afterPlayerPromo = engine.applyAction(afterCpuPromo.state(), PlayerId.PLAYER, new PromoteActiveAction("pb"));
        assertThat(afterPlayerPromo.state().awaitingPromotionPlayer()).isNull();
        assertThat(afterPlayerPromo.state().phase()).isEqualTo(TurnPhase.MAIN_PHASE);
        assertThat(afterPlayerPromo.state().turnNumber()).isEqualTo(4);

        // If both have only 1 prize left, active attacker (PLAYER) wins the tie-break cleanly
        GameState onePrizeEach = new GameState(
                state.turnNumber(), state.activePlayer(), state.startingPlayer(), TurnPhase.MAIN_PHASE, null,
                Map.of(
                        PlayerId.PLAYER, withPrizes(state.player(PlayerId.PLAYER), 1),
                        PlayerId.CPU, withPrizes(state.player(PlayerId.CPU), 1)
                ),
                null, null, 1L
        );
        EngineStepResult simultaneousWin = engine.applyAction(onePrizeEach, PlayerId.PLAYER, new AttackAction(1));
        assertThat(simultaneousWin.state().phase()).isEqualTo(TurnPhase.GAME_OVER);
        assertThat(simultaneousWin.state().winner()).isEqualTo(PlayerId.PLAYER);
        assertThat(simultaneousWin.state().winReason()).isEqualTo(WinReason.ALL_PRIZES_TAKEN);
    }

    @Test
    void recoverHardenAndLeechSeedWorkDeterministically() {
        GameEngine engine = new GameEngine(fixedCoins(true, true));
        // Starmie Recover heals all damage after discarding 1 energy
        PokemonInPlay starmie = PokemonInPlay.fromBasic(new CardInstance("st", pokemonDef("base1-64", "Starmie", 60, "Water",
                        List.of(new AttackDefinition("Recover", List.of("Water", "Water"), 0, null, null)))), 1)
                .withDamage(40)
                .withAttachedEnergies(List.of(
                        new CardInstance("w1", EngineTestFixtures.waterEnergy()),
                        new CardInstance("w2", EngineTestFixtures.waterEnergy())
                ));
        // Onix Harden blocks attacks of <= 30 next turn
        PokemonInPlay onix = PokemonInPlay.fromBasic(new CardInstance("on", pokemonDef("base1-56", "Onix", 90, "Fighting",
                        List.of(
                                new AttackDefinition("Rock Throw", List.of("Fighting"), 10, "10", null),
                                new AttackDefinition("Harden", List.of("Fighting", "Fighting"), 0, null, null)
                        ))), 2)
                .withAttachedEnergies(List.of(
                        new CardInstance("f1", fightingEnergy()),
                        new CardInstance("f2", fightingEnergy())
                ));
        // Bulbasaur Leech Seed heals 10 only when damage > 0
        PokemonInPlay bulba = PokemonInPlay.fromBasic(new CardInstance("bu", pokemonDef("base1-44", "Bulbasaur", 40, "Grass",
                        List.of(new AttackDefinition("Leech Seed", List.of("Grass", "Grass"), 20, "20", null)))), 1)
                .withDamage(10)
                .withAttachedEnergies(List.of(
                        new CardInstance("g1", grassEnergy()),
                        new CardInstance("g2", grassEnergy())
                ));

        GameState state1 = buildState(3, PlayerId.PLAYER, List.of(new CardInstance("d", EngineTestFixtures.waterEnergy())),
                List.of(), starmie, List.of(), List.of(), onix, List.of());
        EngineStepResult recRes = engine.applyAction(state1, PlayerId.PLAYER, new AttackAction(0));
        assertThat(recRes.state().player(PlayerId.PLAYER).active().damage()).isZero();
        assertThat(recRes.state().player(PlayerId.PLAYER).active().attachedEnergies()).hasSize(1);

        // CPU Onix uses Harden on Turn 4
        EngineStepResult hardenRes = engine.applyAction(recRes.state(), PlayerId.CPU, new AttackAction(1));
        // Turn 5: Swap active to damaged Bulbasaur attacking Harden Onix -> Weakness x2 (Grass x2 = 40!) penetrates >30 and heals Bulba!
        GameState bulbaVsHardenOnix = buildState(5, PlayerId.PLAYER, List.of(), List.of(),
                bulba, List.of(), List.of(), hardenRes.state().player(PlayerId.CPU).active(), List.of());
        EngineStepResult leechHit = engine.applyAction(bulbaVsHardenOnix, PlayerId.PLAYER, new AttackAction(0));
        assertThat(leechHit.state().player(PlayerId.CPU).active().damage()).isEqualTo(40);
        assertThat(leechHit.state().player(PlayerId.PLAYER).active().damage()).isZero();
    }

    private static CardViewDTO findHandCard(PlayerViewDTO view, String instanceId) {
        return view.myHand().stream()
                .filter(c -> c.instanceId().equals(instanceId))
                .findFirst()
                .orElseThrow();
    }

    private static PlayerState withPrizes(PlayerState ps, int prizeCount) {
        return new PlayerState(
                ps.deck(), ps.hand(), ps.active(), ps.bench(), ps.prizes().subList(0, prizeCount),
                ps.discardPile(), ps.energyAttachedThisTurn(), ps.retreatedThisTurn(),
                ps.setupReady(), ps.mulliganCount(), ps.plusPowerBonus()
        );
    }

    private GameState buildState(
            int turnNumber,
            PlayerId activePlayer,
            List<CardInstance> playerDeck,
            List<CardInstance> playerHand,
            PokemonInPlay pActive,
            List<PokemonInPlay> pBench,
            List<CardInstance> pDiscard,
            PokemonInPlay cActive,
            List<PokemonInPlay> cBench
    ) {
        List<CardInstance> pPrizes = IntStream.range(0, 6)
                .mapToObj(i -> new CardInstance("pp-" + i, EngineTestFixtures.fireEnergy())).toList();
        List<CardInstance> cPrizes = IntStream.range(0, 6)
                .mapToObj(i -> new CardInstance("cp-" + i, EngineTestFixtures.waterEnergy())).toList();
        List<CardInstance> cDeck = List.of(
                new CardInstance("cd-a", EngineTestFixtures.waterEnergy()),
                new CardInstance("cd-b", EngineTestFixtures.waterEnergy())
        );

        PlayerState player = new PlayerState(
                playerDeck, playerHand, pActive, pBench, pPrizes, pDiscard,
                false, false, true, 0, 0
        );
        PlayerState cpu = new PlayerState(
                cDeck, List.of(), cActive, cBench, cPrizes, List.of(),
                false, false, true, 0, 0
        );
        return new GameState(turnNumber, activePlayer, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(PlayerId.PLAYER, player, PlayerId.CPU, cpu), null, null, 1L);
    }

    private static CardDefinition trainerDef(String id, String name) {
        return new CardDefinition(id, name, "Trainer", null, null, null, List.of(), List.of(),
                List.of(), List.of(), 0, "Item", null, null, 0, "/l.webp", "/h.webp");
    }

    private static CardDefinition specialEnergyDef(String id, String name) {
        return new CardDefinition(id, name, "Energy", null, null, null, List.of(), List.of(),
                List.of(), List.of(), 0, null, "Special", "Colorless", 2, "/l.webp", "/h.webp");
    }

    private static CardDefinition pokemonDef(
            String id,
            String name,
            int hp,
            String type,
            List<AttackDefinition> attacks
    ) {
        return new CardDefinition(id, name, "Pokemon", "Basic", null, hp, List.of(type), attacks,
                "Onix".equals(name) ? List.of(new com.pokemontcg.engine.model.WeaknessRule("Grass", "×2")) : List.of(),
                List.of(), 1, null, null, null, 0, "/l.webp", "/h.webp");
    }

    private static CardDefinition lightningEnergy() {
        return new CardDefinition("base1-100", "Lightning Energy", "Energy", null, null, null,
                List.of(), List.of(), List.of(), List.of(), 0, null, "Normal", "Lightning", 1, "/l.webp", "/h.webp");
    }

    private static CardDefinition psychicEnergy() {
        return new CardDefinition("base1-101", "Psychic Energy", "Energy", null, null, null,
                List.of(), List.of(), List.of(), List.of(), 0, null, "Normal", "Psychic", 1, "/l.webp", "/h.webp");
    }

    private static CardDefinition grassEnergy() {
        return new CardDefinition("base1-99", "Grass Energy", "Energy", null, null, null,
                List.of(), List.of(), List.of(), List.of(), 0, null, "Normal", "Grass", 1, "/l.webp", "/h.webp");
    }

    private static CardDefinition fightingEnergy() {
        return new CardDefinition("base1-97", "Fighting Energy", "Energy", null, null, null,
                List.of(), List.of(), List.of(), List.of(), 0, null, "Normal", "Fighting", 1, "/l.webp", "/h.webp");
    }

    private static RandomProvider fixedCoins(Boolean... results) {
        Deque<Boolean> queue = new ArrayDeque<>(new ArrayList<>(List.of(results)));
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
