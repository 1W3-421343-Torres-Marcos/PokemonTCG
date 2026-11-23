package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PokemonInPlay;

import java.util.Map;

/**
 * Composable block for removing damage counters from a Pokémon in play.
 */
public final class HealBlock {

    private HealBlock() {
    }

    public static int healPokemon(EffectContext ctx, PlayerId owner, String targetInstanceId, int amount, String sourceName) {
        PokemonInPlay target = ctx.findPokemon(owner, targetInstanceId)
                .orElseThrow(() -> new IllegalStateException("Target Pokemon not found for heal: " + targetInstanceId));
        if (target.damage() <= 0 || amount <= 0) {
            return 0;
        }
        int healed = Math.min(amount, target.damage());
        PokemonInPlay updated = target.withDamage(target.damage() - healed);
        ctx.replacePokemon(owner, updated);

        ctx.emit(
                GameEventType.CHECKUP_TICK,
                owner,
                sourceName + " curó " + healed + " PS a " + updated.activeCard().definition().name()
                        + " (" + updated.remainingHp() + "/" + updated.maxHp() + " PS).",
                Map.of(
                        "source", sourceName,
                        "pokemonName", updated.activeCard().definition().name(),
                        "healed", healed
                )
        );
        return healed;
    }

    public static int healAllActive(EffectContext ctx, PlayerId owner, String sourceName) {
        PokemonInPlay active = ctx.state().player(owner).active();
        if (active == null || active.damage() <= 0) {
            return 0;
        }
        return healPokemon(ctx, owner, active.instanceId(), Integer.MAX_VALUE, sourceName);
    }
}
