package com.pokemontcg.bot;

import com.pokemontcg.engine.action.AttachEnergyAction;
import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.EvolveAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.action.PlayBasicAction;
import com.pokemontcg.engine.action.PlayTrainerAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.RetreatAction;
import com.pokemontcg.engine.action.SelectSetupAction;
import com.pokemontcg.engine.dto.AttackViewDTO;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PokemonViewDTO;
import com.pokemontcg.engine.model.TurnPhase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tactical CPU opponent operating strictly on projected {@link PlayerViewDTO} data.
 */
public class TacticalBotStrategy implements BotStrategy {

    private static final int MAX_BENCH_SIZE = 5;
    private static final Pattern SELF_DAMAGE_PATTERN = Pattern.compile("does\\s+(\\d+)\\s+damage\\s+to\\s+itself", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCARD_ENERGY_PATTERN = Pattern.compile("discard\\s+(\\d+|all)\\s+energy", Pattern.CASE_INSENSITIVE);

    @Override
    public GameAction decideNextAction(PlayerViewDTO botView) {
        Objects.requireNonNull(botView, "botView must not be null");
        if (botView.phase() == TurnPhase.GAME_OVER) {
            throw new IllegalStateException("Bot cannot act after GAME_OVER");
        }
        if (botView.awaitingPromotionPlayer() == botView.viewer()) {
            return choosePromotionAction(botView);
        }
        if (botView.phase() == TurnPhase.SETUP && !botView.mySetupReady()) {
            return chooseSetupAction(botView);
        }
        if (botView.phase() == TurnPhase.MAIN_PHASE && botView.activePlayer() == botView.viewer()) {
            return chooseMainPhaseAction(botView);
        }
        throw new IllegalStateException(
                "Tactical bot asked to act when not eligible: phase=" + botView.phase()
                        + ", activePlayer=" + botView.activePlayer()
                        + ", awaitingPromotion=" + botView.awaitingPromotionPlayer()
        );
    }

    private GameAction chooseSetupAction(PlayerViewDTO botView) {
        List<CardViewDTO> basics = botView.myHand().stream()
                .filter(BotCardHelper::isBasicPokemon)
                .sorted(Comparator
                        .comparingInt(this::scoreSetupLead)
                        .reversed()
                        .thenComparing(CardViewDTO::instanceId))
                .toList();
        if (basics.isEmpty()) {
            throw new IllegalStateException("Tactical bot has no Basic Pokemon in SETUP hand");
        }
        String activeId = basics.get(0).instanceId();
        List<String> benchIds = basics.stream()
                .skip(1)
                .limit(MAX_BENCH_SIZE)
                .map(CardViewDTO::instanceId)
                .toList();
        return new SelectSetupAction(activeId, benchIds);
    }

    private int scoreSetupLead(CardViewDTO card) {
        int hp = card.hp() == null ? 0 : card.hp();
        int maxDmg = BotCardHelper.maxAttackDamage(card.attacks());
        return hp + maxDmg - (card.retreatCost() * 5);
    }

    private GameAction choosePromotionAction(PlayerViewDTO botView) {
        PokemonViewDTO oppActive = botView.opponent().active();
        PokemonViewDTO bestBench = botView.myBench().stream()
                .max(Comparator
                        .comparingInt((PokemonViewDTO p) -> scorePokemonAsActive(p, oppActive))
                        .thenComparing(PokemonViewDTO::instanceId))
                .orElseThrow(() -> new IllegalStateException("Bot must promote but has empty bench"));
        return new PromoteActiveAction(bestBench.instanceId());
    }

    private GameAction chooseMainPhaseAction(PlayerViewDTO botView) {
        Optional<GameAction> drawOrRecover = chooseDrawOrRetrievalTrainer(botView);
        if (drawOrRecover.isPresent()) {
            return drawOrRecover.get();
        }

        Optional<GameAction> evolution = chooseEvolutionAction(botView);
        if (evolution.isPresent()) {
            return evolution.get();
        }

        if (botView.myBench().size() < MAX_BENCH_SIZE) {
            Optional<CardViewDTO> basicInHand = botView.myHand().stream()
                    .filter(BotCardHelper::isBasicPokemon)
                    .max(Comparator.comparingInt(this::scoreSetupLead));
            if (basicInHand.isPresent()) {
                return new PlayBasicAction(basicInHand.get().instanceId());
            }
        }

        Optional<GameAction> supportTrainer = chooseSupportAndDisruptionTrainer(botView);
        if (supportTrainer.isPresent()) {
            return supportTrainer.get();
        }

        if (!botView.energyAttachedThisTurn()) {
            Optional<GameAction> attach = chooseBestEnergyAttach(botView);
            if (attach.isPresent()) {
                return attach.get();
            }
        }

        Optional<GameAction> pivotAction = chooseTacticalRetreatOrSwitch(botView);
        if (pivotAction.isPresent()) {
            return pivotAction.get();
        }

        Optional<CardViewDTO> plusPower = findPlayableTrainer(botView, "base1-84");
        if (plusPower.isPresent() && shouldPlayPlusPower(botView)) {
            return new PlayTrainerAction(plusPower.get().instanceId(), null);
        }

        if (botView.canAttackThisTurn() && botView.myActive() != null && botView.opponent().active() != null) {
            Optional<ScoredAttack> bestAttack = chooseBestAttack(botView);
            if (bestAttack.isPresent() && bestAttack.get().score() > -100) {
                return new AttackAction(bestAttack.get().attack().index());
            }
        }

        return new EndTurnAction();
    }

    private Optional<GameAction> chooseDrawOrRetrievalTrainer(PlayerViewDTO botView) {
        Optional<CardViewDTO> bill = findPlayableTrainer(botView, "base1-91");
        if (bill.isPresent() && botView.myHand().size() < 11) {
            return Optional.of(new PlayTrainerAction(bill.get().instanceId(), null));
        }

        Optional<CardViewDTO> retrieval = findPlayableTrainer(botView, "base1-81");
        if (retrieval.isPresent() && (!botView.energyAttachedThisTurn() || countEnergiesInHand(botView) <= 1)) {
            Optional<CardViewDTO> fodder = pickLowestValueDiscardFodder(botView, retrieval.get().validTargetIds());
            if (fodder.isPresent()) {
                return Optional.of(new PlayTrainerAction(retrieval.get().instanceId(), fodder.get().instanceId()));
            }
        }

        Optional<CardViewDTO> oak = findPlayableTrainer(botView, "base1-88");
        if (oak.isPresent() && shouldPlayProfessorOak(botView)) {
            return Optional.of(new PlayTrainerAction(oak.get().instanceId(), null));
        }
        return Optional.empty();
    }

    private boolean shouldPlayProfessorOak(PlayerViewDTO botView) {
        if (botView.myDeckCount() < 2) {
            return false;
        }
        if (botView.myHand().size() <= 3) {
            return true;
        }
        boolean hasEnergyNeeded = !botView.energyAttachedThisTurn() && countEnergiesInHand(botView) > 0;
        boolean hasPlayableEvo = botView.myHand().stream()
                .anyMatch(c -> BotCardHelper.isEvolutionPokemon(c) && c.playable());
        boolean activeCanAttack = botView.myActive() != null
                && botView.myActive().attacks().stream().anyMatch(AttackViewDTO::canPay);
        return !hasEnergyNeeded && !hasPlayableEvo && !activeCanAttack;
    }

    private Optional<GameAction> chooseEvolutionAction(PlayerViewDTO botView) {
        PokemonViewDTO oppActive = botView.opponent().active();
        List<CardViewDTO> playableEvos = botView.myHand().stream()
                .filter(c -> BotCardHelper.isEvolutionPokemon(c) && c.playable() && !c.validTargetIds().isEmpty())
                .sorted(Comparator.comparingInt((CardViewDTO c) -> -(c.hp() == null ? 0 : c.hp())))
                .toList();
        if (playableEvos.isEmpty()) {
            return Optional.empty();
        }
        CardViewDTO evo = playableEvos.get(0);
        String targetId = evo.validTargetIds().stream()
                .max(Comparator.comparingInt(id -> scoreEvolutionTarget(botView, id, oppActive)))
                .orElse(evo.validTargetIds().get(0));
        return Optional.of(new EvolveAction(evo.instanceId(), targetId));
    }

    private int scoreEvolutionTarget(PlayerViewDTO botView, String id, PokemonViewDTO oppActive) {
        if (botView.myActive() != null && botView.myActive().instanceId().equals(id)) {
            return 1000 + botView.myActive().attachedEnergies().size() * 20;
        }
        return botView.myBench().stream()
                .filter(p -> p.instanceId().equals(id))
                .findFirst()
                .map(p -> p.attachedEnergies().size() * 30 + (BotCardHelper.hasWeaknessAdvantage(p, oppActive) ? 40 : 0) + p.remainingHp())
                .orElse(0);
    }

    private Optional<GameAction> chooseSupportAndDisruptionTrainer(PlayerViewDTO botView) {
        PokemonViewDTO oppActive = botView.opponent().active();
        Optional<CardViewDTO> energyRemoval = findPlayableTrainer(botView, "base1-92");
        if (energyRemoval.isPresent()) {
            String erTarget = pickEnergyRemovalTarget(botView, energyRemoval.get().validTargetIds());
            if (erTarget != null) {
                return Optional.of(new PlayTrainerAction(energyRemoval.get().instanceId(), erTarget));
            }
        }

        Optional<CardViewDTO> potion = findPlayableTrainer(botView, "base1-94");
        if (potion.isPresent()) {
            Optional<PokemonViewDTO> healTarget = pickPotionTarget(botView, potion.get().validTargetIds(), 20);
            if (healTarget.isPresent()) {
                return Optional.of(new PlayTrainerAction(potion.get().instanceId(), healTarget.get().instanceId()));
            }
        }

        Optional<CardViewDTO> superPotion = findPlayableTrainer(botView, "base1-90");
        if (superPotion.isPresent()) {
            Optional<PokemonViewDTO> superTarget = pickSuperPotionTarget(botView, superPotion.get().validTargetIds());
            if (superTarget.isPresent()) {
                return Optional.of(new PlayTrainerAction(superPotion.get().instanceId(), superTarget.get().instanceId()));
            }
        }

        Optional<CardViewDTO> gust = findPlayableTrainer(botView, "base1-93");
        if (gust.isPresent() && botView.canAttackThisTurn() && botView.myActive() != null) {
            Optional<PokemonViewDTO> gustTarget = pickGustTarget(botView, gust.get().validTargetIds());
            if (gustTarget.isPresent()) {
                return Optional.of(new PlayTrainerAction(gust.get().instanceId(), gustTarget.get().instanceId()));
            }
        }

        Optional<CardViewDTO> defender = findPlayableTrainer(botView, "base1-80");
        if (defender.isPresent()
                && botView.myActive() != null
                && oppActive != null
                && !oppActive.attachedEnergies().isEmpty()
                && botView.myActive().remainingHp() <= 80
                && defender.get().validTargetIds().contains(botView.myActive().instanceId())) {
            return Optional.of(new PlayTrainerAction(defender.get().instanceId(), botView.myActive().instanceId()));
        }
        return Optional.empty();
    }

    private Optional<GameAction> chooseBestEnergyAttach(PlayerViewDTO botView) {
        List<CardViewDTO> energies = botView.myHand().stream()
                .filter(BotCardHelper::isEnergy)
                .toList();
        if (energies.isEmpty()) {
            return Optional.empty();
        }
        List<PokemonViewDTO> own = new ArrayList<>();
        if (botView.myActive() != null) {
            own.add(botView.myActive());
        }
        own.addAll(botView.myBench());
        if (own.isEmpty()) {
            return Optional.empty();
        }

        PokemonViewDTO oppActive = botView.opponent().active();
        PokemonViewDTO chosenTarget = own.stream()
                .max(Comparator
                        .comparingInt((PokemonViewDTO p) -> scoreEnergyTarget(botView, p, oppActive, energies))
                        .thenComparing(PokemonViewDTO::instanceId))
                .orElse(own.get(0));

        CardViewDTO bestEnergy = BotCardHelper.selectBestEnergyForTarget(energies, chosenTarget);
        return Optional.of(new AttachEnergyAction(bestEnergy.instanceId(), chosenTarget.instanceId()));
    }

    private int scoreEnergyTarget(
            PlayerViewDTO botView,
            PokemonViewDTO target,
            PokemonViewDTO oppActive,
            List<CardViewDTO> energies
    ) {
        boolean isActive = botView.myActive() != null && botView.myActive().instanceId().equals(target.instanceId());
        boolean hasUnpayableDamage = target.attacks().stream().anyMatch(a -> !a.canPay() && a.baseDamage() > 0);
        boolean hasAnyUnpayable = target.attacks().stream().anyMatch(a -> !a.canPay());
        boolean weakAdvantage = BotCardHelper.hasWeaknessAdvantage(target, oppActive);

        int score = 0;
        if (hasUnpayableDamage) {
            score += 90;
        } else if (hasAnyUnpayable) {
            score += 30;
        } else {
            score -= 20;
        }
        if (isActive && !BotCardHelper.isImmobilized(target)) {
            score += (target.remainingHp() >= 40 || hasUnpayableDamage) ? 50 : 10;
        }
        if (weakAdvantage) {
            score += 65;
        }
        if (isTypeMatchInHand(target, energies)) {
            score += 15;
        }
        return score + (target.maxHp() / 5);
    }

    private Optional<GameAction> chooseTacticalRetreatOrSwitch(PlayerViewDTO botView) {
        PokemonViewDTO active = botView.myActive();
        PokemonViewDTO oppActive = botView.opponent().active();
        if (active == null || oppActive == null || botView.myBench().isEmpty()) {
            return Optional.empty();
        }

        Optional<CardViewDTO> switchTrainer = findPlayableTrainer(botView, "base1-95");
        boolean immobilized = BotCardHelper.isImmobilized(active);
        int activeScore = scorePokemonAsActive(active, oppActive);

        Optional<PokemonViewDTO> bestBenchOpt = botView.myBench().stream()
                .max(Comparator
                        .comparingInt((PokemonViewDTO p) -> scorePokemonAsActive(p, oppActive))
                        .thenComparing(PokemonViewDTO::instanceId));
        if (bestBenchOpt.isEmpty()) {
            return Optional.empty();
        }
        PokemonViewDTO bestBench = bestBenchOpt.get();
        int benchScore = scorePokemonAsActive(bestBench, oppActive);
        boolean benchHasAttack = bestBench.attacks().stream().anyMatch(AttackViewDTO::canPay);

        if (immobilized && switchTrainer.isPresent() && benchHasAttack
                && switchTrainer.get().validTargetIds().contains(bestBench.instanceId())) {
            return Optional.of(new PlayTrainerAction(switchTrainer.get().instanceId(), bestBench.instanceId()));
        }

        boolean activeCanKnockOut = BotCardHelper.bestEffectiveDamage(active, oppActive) >= oppActive.remainingHp();
        if (activeCanKnockOut) {
            return Optional.empty();
        }

        boolean urgentPivot = benchHasAttack && benchScore >= activeScore + 45;
        if (!urgentPivot) {
            return Optional.empty();
        }

        if (switchTrainer.isPresent() && active.activeCard().retreatCost() >= 2
                && switchTrainer.get().validTargetIds().contains(bestBench.instanceId())) {
            return Optional.of(new PlayTrainerAction(switchTrainer.get().instanceId(), bestBench.instanceId()));
        }

        if (botView.canRetreat()) {
            List<String> discardIds = chooseRetreatEnergyIds(active, active.activeCard().retreatCost());
            return Optional.of(new RetreatAction(bestBench.instanceId(), discardIds));
        }
        return Optional.empty();
    }

    private int scorePokemonAsActive(PokemonViewDTO pokemon, PokemonViewDTO oppActive) {
        int score = pokemon.remainingHp() + (pokemon.attachedEnergies().size() * 20);
        if (BotCardHelper.isImmobilized(pokemon)) {
            score -= 80;
        }
        int effective = BotCardHelper.bestEffectiveDamage(pokemon, oppActive);
        score += effective * 2;
        if (oppActive != null && effective >= oppActive.remainingHp() && effective > 0) {
            score += 120;
        }
        if (BotCardHelper.hasWeaknessAdvantage(pokemon, oppActive)) {
            score += 40;
        }
        return score;
    }

    private boolean shouldPlayPlusPower(PlayerViewDTO botView) {
        if (!botView.canAttackThisTurn() || botView.myActive() == null || botView.opponent().active() == null) {
            return false;
        }
        PokemonViewDTO oppActive = botView.opponent().active();
        int effective = BotCardHelper.bestEffectiveDamage(botView.myActive(), oppActive);
        return effective > 0 && effective < oppActive.remainingHp();
    }

    private Optional<ScoredAttack> chooseBestAttack(PlayerViewDTO botView) {
        PokemonViewDTO attacker = botView.myActive();
        PokemonViewDTO defender = botView.opponent().active();
        if (attacker == null || defender == null) {
            return Optional.empty();
        }

        return attacker.attacks().stream()
                .filter(AttackViewDTO::canPay)
                .map(attack -> scoreAttack(botView, attacker, defender, attack))
                .max(Comparator
                        .comparingInt(ScoredAttack::score)
                        .thenComparingInt(sa -> sa.attack().index()));
    }

    private ScoredAttack scoreAttack(
            PlayerViewDTO botView,
            PokemonViewDTO attacker,
            PokemonViewDTO defender,
            AttackViewDTO attack
    ) {
        int effectiveDamage = BotCardHelper.estimateEffectiveDamage(attacker, defender, attack.baseDamage());
        boolean lethal = effectiveDamage >= defender.remainingHp() && effectiveDamage > 0;
        int score = effectiveDamage;

        if (lethal) {
            score += 200;
        }

        String text = attack.text() == null ? "" : attack.text().toLowerCase(Locale.ROOT);
        int discardPenalty = estimateDiscardEnergyCount(text, attacker.attachedEnergies().size()) * (lethal ? 10 : 25);
        score -= discardPenalty;

        int recoil = parseSelfDamage(text);
        if (recoil >= attacker.remainingHp()) {
            boolean gameWinning = lethal && botView.opponent().prizesRemaining() <= 1;
            if (!gameWinning) {
                score -= 300;
            }
        } else if (recoil > 0 && !lethal) {
            score -= recoil / 2;
        }

        if (effectiveDamage == 0 || !lethal) {
            if (text.contains("paralyzed") || text.contains("confused")) {
                score += 25;
            } else if (text.contains("poisoned")) {
                score += 20;
            } else if (text.contains("prevent all damage") || text.contains("prevent all effects")) {
                score += attacker.remainingHp() <= 40 ? 45 : 15;
            } else if (text.contains("remove all damage") && attacker.damage() >= 30) {
                score += 55;
            }
        }

        if ("Switch".equalsIgnoreCase(attack.name())) {
            score -= 5;
        }
        return new ScoredAttack(attack, score);
    }

    private Optional<CardViewDTO> findPlayableTrainer(PlayerViewDTO botView, String cardId) {
        return botView.myHand().stream()
                .filter(c -> cardId.equalsIgnoreCase(c.cardId()) && c.playable())
                .findFirst();
    }

    private int countEnergiesInHand(PlayerViewDTO botView) {
        return (int) botView.myHand().stream().filter(BotCardHelper::isEnergy).count();
    }

    private Optional<CardViewDTO> pickLowestValueDiscardFodder(PlayerViewDTO botView, List<String> validIds) {
        return botView.myHand().stream()
                .filter(c -> validIds.contains(c.instanceId()))
                .filter(c -> !BotCardHelper.isEnergy(c))
                .min(Comparator.comparingInt(c -> c.playable() ? 2 : (BotCardHelper.isEvolutionPokemon(c) ? 3 : 1)));
    }

    private String pickEnergyRemovalTarget(PlayerViewDTO botView, List<String> validIds) {
        PokemonViewDTO oppActive = botView.opponent().active();
        if (oppActive != null && validIds.contains(oppActive.instanceId())) {
            return oppActive.instanceId();
        }
        return botView.opponent().bench().stream()
                .filter(b -> validIds.contains(b.instanceId()))
                .max(Comparator.comparingInt(b -> b.attachedEnergies().size()))
                .map(PokemonViewDTO::instanceId)
                .orElse(null);
    }

    private Optional<PokemonViewDTO> pickPotionTarget(PlayerViewDTO botView, List<String> validIds, int minDamage) {
        if (botView.myActive() != null
                && validIds.contains(botView.myActive().instanceId())
                && botView.myActive().damage() >= minDamage) {
            return Optional.of(botView.myActive());
        }
        return botView.myBench().stream()
                .filter(p -> validIds.contains(p.instanceId()) && p.damage() >= minDamage)
                .max(Comparator.comparingInt(PokemonViewDTO::damage));
    }

    private Optional<PokemonViewDTO> pickSuperPotionTarget(PlayerViewDTO botView, List<String> validIds) {
        if (botView.myActive() != null
                && validIds.contains(botView.myActive().instanceId())
                && botView.myActive().damage() >= 40
                && botView.myActive().attachedEnergies().size() > 1) {
            return Optional.of(botView.myActive());
        }
        return Optional.empty();
    }

    private Optional<PokemonViewDTO> pickGustTarget(PlayerViewDTO botView, List<String> validIds) {
        PokemonViewDTO myActive = botView.myActive();
        PokemonViewDTO oppActive = botView.opponent().active();
        if (myActive == null || oppActive == null) {
            return Optional.empty();
        }
        boolean alreadyKoOppActive = BotCardHelper.bestEffectiveDamage(myActive, oppActive) >= oppActive.remainingHp();
        if (alreadyKoOppActive) {
            return Optional.empty();
        }
        return botView.opponent().bench().stream()
                .filter(b -> validIds.contains(b.instanceId()))
                .filter(b -> BotCardHelper.bestEffectiveDamage(myActive, b) >= b.remainingHp()
                        || (BotCardHelper.hasWeaknessAdvantage(myActive, b) && b.attachedEnergies().size() <= 1))
                .max(Comparator.comparingInt(b -> BotCardHelper.bestEffectiveDamage(myActive, b) - b.remainingHp()));
    }

    private boolean isTypeMatchInHand(PokemonViewDTO target, List<CardViewDTO> energies) {
        String primaryType = target.activeCard().types().isEmpty() ? "" : target.activeCard().types().get(0);
        return energies.stream().anyMatch(e -> primaryType.equalsIgnoreCase(e.providedEnergyType()));
    }

    private List<String> chooseRetreatEnergyIds(PokemonViewDTO active, int retreatCost) {
        if (retreatCost <= 0) {
            return List.of();
        }
        List<CardViewDTO> sorted = active.attachedEnergies().stream()
                .sorted(Comparator.comparingInt(e -> Math.max(1, e.providedEnergyCount())))
                .toList();
        List<String> chosen = new ArrayList<>();
        int paid = 0;
        for (CardViewDTO e : sorted) {
            if (paid >= retreatCost) {
                break;
            }
            chosen.add(e.instanceId());
            paid += Math.max(1, e.providedEnergyCount());
        }
        return chosen;
    }

    private int parseSelfDamage(String lowerText) {
        Matcher matcher = SELF_DAMAGE_PATTERN.matcher(lowerText);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private int estimateDiscardEnergyCount(String lowerText, int attachedSize) {
        Matcher matcher = DISCARD_ENERGY_PATTERN.matcher(lowerText);
        if (!matcher.find()) {
            return 0;
        }
        String token = matcher.group(1);
        if ("all".equalsIgnoreCase(token)) {
            return attachedSize;
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private record ScoredAttack(AttackViewDTO attack, int score) {
    }
}
