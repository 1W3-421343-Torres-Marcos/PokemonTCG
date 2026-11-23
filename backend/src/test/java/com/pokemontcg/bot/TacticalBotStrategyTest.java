package com.pokemontcg.bot;

import com.pokemontcg.engine.action.AttachEnergyAction;
import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.EvolveAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.action.PlayTrainerAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.RetreatAction;
import com.pokemontcg.engine.action.SelectSetupAction;
import com.pokemontcg.engine.dto.AttackViewDTO;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.dto.OpponentViewDTO;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PokemonViewDTO;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WeaknessRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TacticalBotStrategyTest {

    private final TacticalBotStrategy strategy = new TacticalBotStrategy();

    @Test
    void choosesBestSetupLeadAndTacticalPromotionByTypeAdvantage() {
        CardViewDTO weakBasic = basicCard("h-1", "Caterpie", 40, "Grass", 10, 1);
        CardViewDTO strongBasic = basicCard("h-2", "Electabuzz", 70, "Lightning", 30, 1);
        PlayerViewDTO setupView = view(TurnPhase.SETUP, PlayerId.CPU, null, false, false, false, false,
                List.of(weakBasic, strongBasic), null, List.of(),
                pokemon("p-a", basicCard("p-1", "Squirtle", 40, "Water", 10, 1), 40, List.of(), List.of()));

        GameAction setupAction = strategy.decideNextAction(setupView);
        assertThat(setupAction).isEqualTo(new SelectSetupAction("h-2", List.of("h-1")));

        // Promotion prioritizes Electric bench into Water opponent (x2 Weakness)
        PokemonViewDTO neutralBench = pokemon("b-1", basicCard("c-1", "Machop", 50, "Fighting", 20, 1),
                50, List.of(energy("e1", "Fighting")), List.of());
        PokemonViewDTO lightningBench = pokemon("b-2", strongBasic,
                70, List.of(energy("e2", "Lightning")), List.of(),
                new AttackViewDTO(0, "Thundershock", List.of("Lightning"), 10, "10", "", true));
        PokemonViewDTO waterOpp = pokemon("o-1",
                cardWithWeakResist("o-w", "Wartortle", 70, "Water",
                        List.of(new WeaknessRule("Lightning", "×2")), List.of(), 1, List.of()),
                70, List.of(), List.of());

        PlayerViewDTO promoView = view(TurnPhase.ATTACK_PHASE, PlayerId.PLAYER, PlayerId.CPU,
                false, false, false, true, List.of(), null, List.of(neutralBench, lightningBench), waterOpp);

        GameAction promoAction = strategy.decideNextAction(promoView);
        assertThat(promoAction).isEqualTo(new PromoteActiveAction("b-2"));
    }

    @Test
    void prioritizesPlayableDrawTrainersEvolutionsAndEnergyRemoval() {
        CardViewDTO bill = trainer("tr-bill", "base1-91", "Bill", true, List.of());
        CardViewDTO evo = evolutionCard("ev-1", "Charmeleon", "Charmander", 80, true, List.of("my-active"));
        CardViewDTO er = trainer("tr-er", "base1-92", "Energy Removal", true, List.of("opp-active"));
        PokemonViewDTO myActive = pokemon("my-active", basicCard("p-c", "Charmander", 50, "Fire", 10, 1), 50, List.of(), List.of());
        PokemonViewDTO oppActive = pokemon("opp-active", basicCard("o-s", "Squirtle", 40, "Water", 10, 1),
                40, List.of(energy("oe-1", "Water")), List.of());

        // 1. Bill first
        PlayerViewDTO withBill = view(TurnPhase.MAIN_PHASE, PlayerId.CPU, null, false, false, true, true,
                List.of(evo, bill, er), myActive, List.of(), oppActive);
        assertThat(strategy.decideNextAction(withBill)).isEqualTo(new PlayTrainerAction("tr-bill", null));

        // 2. Evolution before Energy Removal / Attack
        PlayerViewDTO withEvo = view(TurnPhase.MAIN_PHASE, PlayerId.CPU, null, false, false, true, true,
                List.of(evo, er), myActive, List.of(), oppActive);
        assertThat(strategy.decideNextAction(withEvo)).isEqualTo(new EvolveAction("ev-1", "my-active"));

        // 3. Energy Removal targets opponent active with energy
        PlayerViewDTO withEr = view(TurnPhase.MAIN_PHASE, PlayerId.CPU, null, false, false, true, true,
                List.of(er), myActive, List.of(), oppActive);
        assertThat(strategy.decideNextAction(withEr)).isEqualTo(new PlayTrainerAction("tr-er", "opp-active"));
    }

    @Test
    void usesSwitchWhenParalyzedOrRetreatsWhenOutmatchedAndAvoidsSuicideRecoil() {
        CardViewDTO switchCard = trainer("tr-sw", "base1-95", "Switch", true, List.of("bench-1"));
        PokemonViewDTO paralyzedActive = pokemon("my-active", basicCard("p-c", "Onix", 90, "Fighting", 10, 3),
                90, List.of(), List.of(StatusCondition.PARALYZED));
        PokemonViewDTO healthyBench = pokemon("bench-1", basicCard("p-m", "Hitmonchan", 70, "Fighting", 40, 1),
                70, List.of(energy("f1", "Fighting")), List.of(),
                new AttackViewDTO(0, "Jab", List.of("Fighting"), 20, "20", "", true));
        PokemonViewDTO oppActive = pokemon("opp-1", basicCard("o-1", "Magmar", 50, "Fire", 30, 2),
                50, List.of(energy("of1", "Fire")), List.of());

        PlayerViewDTO paralyzedView = view(TurnPhase.MAIN_PHASE, PlayerId.CPU, null, true, false, false, true,
                List.of(switchCard), paralyzedActive, List.of(healthyBench), oppActive);
        assertThat(strategy.decideNextAction(paralyzedView)).isEqualTo(new PlayTrainerAction("tr-sw", "bench-1"));

        // Retreat when low HP and weak vs healthy ready bench
        PokemonViewDTO weakLowHp = pokemon("my-active", basicCard("p-low", "Charmander", 50, "Fire", 10, 1),
                10, List.of(energy("re1", "Fire")), List.of(),
                new AttackViewDTO(0, "Scratch", List.of("Colorless"), 10, "10", "", true));
        PlayerViewDTO retreatView = new PlayerViewDTO(
                PlayerId.CPU, 5, PlayerId.CPU, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                true, false, true, true, true, null, null,
                List.of(), weakLowHp, List.of(healthyBench), List.of(), 20, 6,
                new OpponentViewDTO(PlayerId.PLAYER, 4, 20, 6, oppActive, List.of(), List.of()), true
        );
        assertThat(strategy.decideNextAction(retreatView)).isEqualTo(new RetreatAction("bench-1", List.of("re1")));

        // Attack avoids self-KO recoil (80 self dmg on 40 HP remaining) in favor of safe attack
        PokemonViewDTO chanseyLow = pokemon("ch-1", basicCard("base1-3", "Chansey", 120, "Colorless", 80, 1),
                40, List.of(energy("e1", "Fire"), energy("e2", "Fire")), List.of(),
                new AttackViewDTO(0, "Scrunch", List.of("Colorless"), 0, null, "Flip a coin.", true),
                new AttackViewDTO(1, "Double-edge", List.of("Colorless", "Colorless"), 80, "80",
                        "Chansey does 80 damage to itself.", true));
        PokemonViewDTO toughOpp = pokemon("sn-1", basicCard("base1-x", "Snorlax", 90, "Colorless", 30, 2),
                90, List.of(), List.of());
        PlayerViewDTO recoilChoice = view(TurnPhase.MAIN_PHASE, PlayerId.CPU, null, true, false, true, true,
                List.of(), chanseyLow, List.of(), toughOpp);
        assertThat(strategy.decideNextAction(recoilChoice)).isEqualTo(new AttackAction(0));
    }

    @Test
    void attachesEnergyToWeaknessAdvantageBenchAndChoosesOptimalAttackWithWeaknessAndResistance() {
        PokemonViewDTO resistedActive = pokemon("my-active", basicCard("p-1", "Hitmonchan", 70, "Fighting", 20, 1),
                70, List.of(energy("f1", "Fighting")), List.of(),
                new AttackViewDTO(0, "Jab", List.of("Fighting"), 20, "20", "", true),
                new AttackViewDTO(1, "Special Punch", List.of("Fighting", "Colorless"), 40, "40", "", false));
        PokemonViewDTO electricBench = pokemon("bench-el", basicCard("p-2", "Electabuzz", 70, "Lightning", 30, 1),
                70, List.of(), List.of(),
                new AttackViewDTO(0, "Thundershock", List.of("Lightning"), 10, "10", "", false));
        PokemonViewDTO squirtleOpp = pokemon("opp-w",
                cardWithWeakResist("o-s", "Squirtle", 60, "Water",
                        List.of(new WeaknessRule("Lightning", "×2")),
                        List.of(new ResistanceRule("Fighting", "-30")), 1, List.of()),
                60, List.of(), List.of());

        CardViewDTO fireHand = energy("he-1", "Fire");
        CardViewDTO lightHand = energy("he-2", "Lightning");

        // Weakness + unpayable attack on Electabuzz wins energy allocation over Fighting Hitmonchan
        PlayerViewDTO attachView = view(TurnPhase.MAIN_PHASE, PlayerId.CPU, null, false, false, true, true,
                List.of(fireHand, lightHand), resistedActive, List.of(electricBench), squirtleOpp);
        GameAction action = strategy.decideNextAction(attachView);
        assertThat(action).isEqualTo(new AttachEnergyAction("he-2", "bench-el"));
    }

    private PlayerViewDTO view(
            TurnPhase phase, PlayerId active, PlayerId promo,
            boolean energyAttached, boolean retreated, boolean canAttack, boolean setupReady,
            List<CardViewDTO> hand, PokemonViewDTO myActive, List<PokemonViewDTO> bench, PokemonViewDTO oppActive
    ) {
        return new PlayerViewDTO(
                PlayerId.CPU, 4, active, PlayerId.PLAYER, phase, promo,
                energyAttached, retreated, canAttack, setupReady, true, null, null,
                hand, myActive, bench, List.of(), 25, 4,
                new OpponentViewDTO(PlayerId.PLAYER, 5, 25, 4, oppActive, List.of(), List.of()), false
        );
    }

    private CardViewDTO basicCard(String id, String name, int hp, String type, int atkDmg, int retreatCost) {
        return cardWithWeakResist(id, name, hp, type, List.of(), List.of(), retreatCost,
                List.of(new AttackViewDTO(0, "Strike", List.of(type), atkDmg, String.valueOf(atkDmg), "", false)));
    }

    private CardViewDTO cardWithWeakResist(
            String id, String name, int hp, String type,
            List<WeaknessRule> weaknesses, List<ResistanceRule> resistances, int retreatCost, List<AttackViewDTO> attacks
    ) {
        return new CardViewDTO(id, id, name, "Pokemon", "Basic", null, hp, List.of(type), attacks,
                weaknesses, resistances, retreatCost, null, null, null, 0, "/l.webp", "/h.webp", true, List.of());
    }

    private CardViewDTO evolutionCard(
            String id, String name, String evolveFrom, int hp, boolean playable, List<String> targets
    ) {
        return new CardViewDTO(id, id, name, "Pokemon", "Stage1", evolveFrom, hp, List.of("Fire"), List.of(),
                List.of(), List.of(), 1, null, null, null, 0, "/l.webp", "/h.webp", playable, targets);
    }

    private CardViewDTO trainer(String instanceId, String cardId, String name, boolean playable, List<String> targets) {
        return new CardViewDTO(instanceId, cardId, name, "Trainer", null, null, null, List.of(), List.of(),
                List.of(), List.of(), 0, "Item", null, null, 0, "/l.webp", "/h.webp", playable, targets);
    }

    private CardViewDTO energy(String instanceId, String providedType) {
        return new CardViewDTO(instanceId, "e-" + providedType, providedType + " Energy", "Energy", null, null,
                null, List.of(), List.of(), List.of(), List.of(), 0, null, "Normal", providedType, 1,
                "/l.webp", "/h.webp", true, List.of());
    }

    private PokemonViewDTO pokemon(
            String instanceId, CardViewDTO card, int remainingHp, List<CardViewDTO> energies,
            List<StatusCondition> statuses, AttackViewDTO... attacks
    ) {
        int maxHp = card.hp() == null ? remainingHp : card.hp();
        return new PokemonViewDTO(
                instanceId, card, maxHp, remainingHp, Math.max(0, maxHp - remainingHp),
                energies, statuses, attacks.length == 0 ? card.attacks() : List.of(attacks), 1, 1
        );
    }
}
