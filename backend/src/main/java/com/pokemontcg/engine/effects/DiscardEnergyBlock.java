package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composable block for discarding attached energies and retrieving basic energies from discard.
 */
public final class DiscardEnergyBlock {

    private DiscardEnergyBlock() {
    }

    public static int discardFromPokemon(
            EffectContext ctx,
            PlayerId owner,
            String targetInstanceId,
            int maxCount,
            String preferredType,
            String sourceName
    ) {
        PokemonInPlay target = ctx.findPokemon(owner, targetInstanceId)
                .orElseThrow(() -> new IllegalStateException("Target Pokemon not found for energy discard: " + targetInstanceId));
        if (target.attachedEnergies().isEmpty() || maxCount <= 0) {
            return 0;
        }

        List<CardInstance> attached = new ArrayList<>(target.attachedEnergies());
        List<CardInstance> discarded = new ArrayList<>();
        int targetCount = Math.min(maxCount, attached.size());

        for (int i = 0; i < targetCount; i++) {
            int chosenIdx = findBestDiscardIndex(attached, preferredType);
            discarded.add(attached.remove(chosenIdx));
        }

        ctx.replacePokemon(owner, target.withAttachedEnergies(attached));
        ctx.updatePlayer(owner, ps -> {
            List<CardInstance> nextDiscard = new ArrayList<>(ps.discardPile());
            nextDiscard.addAll(discarded);
            return new PlayerState(
                    ps.deck(), ps.hand(), ps.active(), ps.bench(), ps.prizes(), nextDiscard,
                    ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(),
                    ps.mulliganCount(), ps.plusPowerBonus()
            );
        });

        ctx.emit(
                GameEventType.CHECKUP_TICK,
                owner,
                sourceName + " descartó " + discarded.size() + " Energía(s) de "
                        + target.activeCard().definition().name() + ".",
                Map.of(
                        "source", sourceName,
                        "pokemonName", target.activeCard().definition().name(),
                        "energiesDiscarded", discarded.size()
                )
        );
        return discarded.size();
    }

    public static int discardAllFromAttacker(EffectContext ctx, String sourceName) {
        PokemonInPlay attacker = ctx.state().player(ctx.actor()).active();
        if (attacker == null || attacker.attachedEnergies().isEmpty()) {
            return 0;
        }
        return discardFromPokemon(ctx, ctx.actor(), attacker.instanceId(), Integer.MAX_VALUE, null, sourceName);
    }

    public static int retrieveBasicEnergiesFromDiscard(
            EffectContext ctx,
            PlayerId owner,
            int maxCount,
            String sourceName
    ) {
        if (maxCount <= 0) {
            return 0;
        }
        PlayerState ps = ctx.state().player(owner);
        List<CardInstance> nextDiscard = new ArrayList<>(ps.discardPile());
        List<CardInstance> recovered = new ArrayList<>();

        for (int i = 0; i < nextDiscard.size() && recovered.size() < maxCount; ) {
            CardInstance card = nextDiscard.get(i);
            if (card.definition().basicEnergy()) {
                recovered.add(nextDiscard.remove(i));
            } else {
                i++;
            }
        }
        if (recovered.isEmpty()) {
            return 0;
        }

        List<CardInstance> nextHand = new ArrayList<>(ps.hand());
        nextHand.addAll(recovered);
        ctx.updatePlayer(owner, current -> new PlayerState(
                current.deck(), nextHand, current.active(), current.bench(), current.prizes(), nextDiscard,
                current.energyAttachedThisTurn(), current.retreatedThisTurn(), current.setupReady(),
                current.mulliganCount(), current.plusPowerBonus()
        ));

        ctx.emit(
                GameEventType.CHECKUP_TICK,
                owner,
                sourceName + " recuperó " + recovered.size() + " Energía(s) Básica(s) del descarte.",
                Map.of("source", sourceName, "recoveredCount", recovered.size())
        );
        return recovered.size();
    }

    private static int findBestDiscardIndex(List<CardInstance> attached, String preferredType) {
        if (preferredType != null && !preferredType.isBlank()) {
            for (int i = 0; i < attached.size(); i++) {
                CardInstance energy = attached.get(i);
                String provided = energy.definition().providedEnergyType();
                if (preferredType.equalsIgnoreCase(provided)
                        || preferredType.equalsIgnoreCase(energy.definition().primaryType())) {
                    return i;
                }
            }
        }
        return 0;
    }
}
