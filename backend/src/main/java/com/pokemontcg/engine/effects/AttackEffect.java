package com.pokemontcg.engine.effects;

/**
 * Strategy contract for special Pokémon attacks registered in {@link CardEffectRegistry}.
 */
@FunctionalInterface
public interface AttackEffect {

    AttackEffect DEFAULT_DAMAGE = ctx -> {
        if (ctx.attack().baseDamage() > 0) {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
        }
    };

    void apply(AttackContext ctx);
}
