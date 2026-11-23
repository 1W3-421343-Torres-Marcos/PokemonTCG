package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.rules.StatusRules;

import java.util.Map;

/**
 * Composable block for applying a status condition via {@link StatusRules}.
 */
public final class ApplyStatusBlock {

    private ApplyStatusBlock() {
    }

    public static void applyToDefenderActive(AttackContext attackCtx, StatusCondition condition) {
        EffectContext ctx = attackCtx.effects();
        if (attackCtx.defenderInitiallyImmune()) {
            return;
        }
        PlayerId defenderOwner = ctx.actor().opponent();
        PokemonInPlay defender = ctx.state().player(defenderOwner).active();
        if (defender == null || defender.knockedOut()) {
            return;
        }
        PokemonInPlay updated = StatusRules.applyStatus(defender, condition);
        ctx.replacePokemon(defenderOwner, updated);

        ctx.emit(
                GameEventType.STATUS_APPLIED,
                defenderOwner,
                updated.activeCard().definition().name() + " quedó " + labelFor(condition) + ".",
                Map.of(
                        "pokemonName", updated.activeCard().definition().name(),
                        "status", condition.name()
                )
        );
    }

    private static String labelFor(StatusCondition condition) {
        return switch (condition) {
            case POISONED -> "Envenenado";
            case BURNED -> "Quemado";
            case ASLEEP -> "Dormido";
            case PARALYZED -> "Paralizado";
            case CONFUSED -> "Confundido";
        };
    }
}
