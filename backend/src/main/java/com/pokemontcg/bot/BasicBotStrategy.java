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
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PokemonViewDTO;
import com.pokemontcg.engine.model.TurnPhase;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BasicBotStrategy implements BotStrategy {

    private static final int MAX_BENCH_SIZE = 5;

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
                "Bot asked to act when not eligible: phase=" + botView.phase()
                        + ", activePlayer=" + botView.activePlayer()
                        + ", awaitingPromotion=" + botView.awaitingPromotionPlayer()
        );
    }

    private GameAction chooseSetupAction(PlayerViewDTO botView) {
        List<CardViewDTO> basics = botView.myHand().stream()
                .filter(BotCardHelper::isBasicPokemon)
                .sorted(Comparator
                        .comparingInt((CardViewDTO c) -> -(c.hp() == null ? 0 : c.hp()))
                        .thenComparingInt(c -> -BotCardHelper.maxAttackDamage(c.attacks()))
                        .thenComparing(CardViewDTO::instanceId))
                .toList();

        if (basics.isEmpty()) {
            throw new IllegalStateException("Bot has no Basic Pokemon in opening hand during SETUP");
        }

        String activeId = basics.get(0).instanceId();
        List<String> benchIds = basics.stream()
                .skip(1)
                .limit(MAX_BENCH_SIZE)
                .map(CardViewDTO::instanceId)
                .toList();

        return new SelectSetupAction(activeId, benchIds);
    }

    private GameAction choosePromotionAction(PlayerViewDTO botView) {
        PokemonViewDTO bestBench = botView.myBench().stream()
                .max(Comparator
                        .comparingInt((PokemonViewDTO p) -> (p.attachedEnergies().size() * 30) + p.remainingHp())
                        .thenComparingInt(PokemonViewDTO::maxHp)
                        .thenComparing(PokemonViewDTO::instanceId))
                .orElseThrow(() -> new IllegalStateException("Bot must promote but has empty bench"));

        return new PromoteActiveAction(bestBench.instanceId());
    }

    private GameAction chooseMainPhaseAction(PlayerViewDTO botView) {
        // 1. Fill bench with available Basics
        if (botView.myBench().size() < MAX_BENCH_SIZE) {
            Optional<CardViewDTO> basicInHand = botView.myHand().stream()
                    .filter(BotCardHelper::isBasicPokemon)
                    .max(Comparator.comparingInt(c -> c.hp() == null ? 0 : c.hp()));
            if (basicInHand.isPresent()) {
                return new PlayBasicAction(basicInHand.get().instanceId());
            }
        }

        // 2. Attach 1 Energy per turn
        if (!botView.energyAttachedThisTurn()) {
            List<CardViewDTO> energiesInHand = botView.myHand().stream()
                    .filter(BotCardHelper::isEnergy)
                    .toList();
            if (!energiesInHand.isEmpty()) {
                Optional<PokemonViewDTO> target = selectEnergyTarget(botView);
                if (target.isPresent()) {
                    CardViewDTO chosenEnergy = BotCardHelper.selectBestEnergyForTarget(energiesInHand, target.get());
                    return new AttachEnergyAction(chosenEnergy.instanceId(), target.get().instanceId());
                }
            }
        }

        // 3. Attack with the highest-damage payable attack
        if (botView.canAttackThisTurn() && botView.myActive() != null) {
            Optional<AttackViewDTO> bestAttack = botView.myActive().attacks().stream()
                    .filter(AttackViewDTO::canPay)
                    .max(Comparator
                            .comparingInt(AttackViewDTO::baseDamage)
                            .thenComparingInt(a -> -a.cost().size())
                            .thenComparingInt(AttackViewDTO::index));
            if (bestAttack.isPresent()) {
                return new AttackAction(bestAttack.get().index());
            }
        }

        // 4. End Turn
        return new EndTurnAction();
    }

    private Optional<PokemonViewDTO> selectEnergyTarget(PlayerViewDTO botView) {
        PokemonViewDTO active = botView.myActive();
        if (active != null && hasUnpayableDamageAttack(active)) {
            return Optional.of(active);
        }

        Optional<PokemonViewDTO> hungryBench = botView.myBench().stream()
                .filter(this::hasUnpayableDamageAttack)
                .max(Comparator.comparingInt(PokemonViewDTO::maxHp));
        if (hungryBench.isPresent()) {
            return hungryBench;
        }

        if (active != null && hasAnyUnpayableAttack(active)) {
            return Optional.of(active);
        }

        return Optional.ofNullable(active)
                .or(() -> botView.myBench().stream().findFirst());
    }

    private boolean hasUnpayableDamageAttack(PokemonViewDTO pokemon) {
        return pokemon.attacks().stream()
                .anyMatch(a -> !a.canPay() && a.baseDamage() > 0);
    }

    private boolean hasAnyUnpayableAttack(PokemonViewDTO pokemon) {
        return pokemon.attacks().stream().anyMatch(a -> !a.canPay());
    }
}
