package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PokemonInPlay;

import java.util.Map;

/**
 * Composable block for recoil / self-damage applied directly to an Active Pokémon.
 */
public final class DamageBlock {

    private DamageBlock() {
    }

    public static void dealSelfDamage(EffectContext ctx, PlayerId owner, int recoilDamage, String sourceName) {
        if (recoilDamage <= 0) {
            return;
        }
        PokemonInPlay active = ctx.state().player(owner).active();
        if (active == null) {
            return;
        }
        PokemonInPlay damaged = active.withDamage(active.damage() + recoilDamage);
        ctx.replacePokemon(owner, damaged);

        ctx.emit(
                GameEventType.DAMAGE_DEALT,
                owner,
                damaged.activeCard().definition().name() + " recibió " + recoilDamage + " de daño propio por "
                        + sourceName + " (" + damaged.remainingHp() + "/" + damaged.maxHp() + " PS).",
                Map.of(
                        "targetName", damaged.activeCard().definition().name(),
                        "damage", recoilDamage,
                        "remainingHp", damaged.remainingHp(),
                        "maxHp", damaged.maxHp(),
                        "selfDamage", true
                )
        );
    }
}
