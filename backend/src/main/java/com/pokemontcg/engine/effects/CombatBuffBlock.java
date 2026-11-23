package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.ActiveModifier;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composable block for combat buffs (PlusPower) and protective active modifiers (Defender, Harden, etc.).
 */
public final class CombatBuffBlock {

    private CombatBuffBlock() {
    }

    public static void addModifierToPokemon(
            EffectContext ctx,
            PlayerId owner,
            String targetInstanceId,
            ActiveModifier modifier
    ) {
        PokemonInPlay target = ctx.findPokemon(owner, targetInstanceId)
                .orElseThrow(() -> new IllegalStateException("Target Pokemon not found for modifier: " + targetInstanceId));
        List<ActiveModifier> nextModifiers = new ArrayList<>(target.modifiers());
        nextModifiers.add(modifier);
        PokemonInPlay updated = target.withModifiers(nextModifiers);
        ctx.replacePokemon(owner, updated);

        ctx.emit(
                GameEventType.CHECKUP_TICK,
                owner,
                modifier.sourceCardName() + " aplicó protección sobre "
                        + updated.activeCard().definition().name() + ".",
                Map.of(
                        "source", modifier.sourceCardName(),
                        "modifierType", modifier.type().name(),
                        "pokemonName", updated.activeCard().definition().name(),
                        "expiresAfterTurn", modifier.expiresAfterTurn()
                )
        );
    }

    public static void addPlusPower(EffectContext ctx, PlayerId owner, int bonus) {
        if (bonus <= 0) {
            return;
        }
        ctx.updatePlayer(owner, ps -> new PlayerState(
                ps.deck(), ps.hand(), ps.active(), ps.bench(), ps.prizes(), ps.discardPile(),
                ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(),
                ps.mulliganCount(), ps.plusPowerBonus() + bonus
        ));

        ctx.emit(
                GameEventType.CHECKUP_TICK,
                owner,
                "PlusPower otorgó +" + bonus + " de daño a los ataques de "
                        + ctx.displayPlayer(owner) + " este turno.",
                Map.of("source", "PlusPower", "bonus", bonus)
        );
    }
}
