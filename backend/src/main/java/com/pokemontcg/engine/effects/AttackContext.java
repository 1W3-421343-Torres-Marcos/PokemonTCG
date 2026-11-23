package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.ModifierType;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.rules.DamageCalculator;

import java.util.Map;
import java.util.Objects;

/**
 * Execution context for resolving an individual Pokémon attack and its side effects.
 */
public final class AttackContext {

    private final EffectContext effects;
    private final AttackDefinition attack;
    private final boolean defenderInitiallyImmune;

    public AttackContext(EffectContext effects, AttackDefinition attack, boolean defenderInitiallyImmune) {
        this.effects = Objects.requireNonNull(effects, "effects must not be null");
        this.attack = Objects.requireNonNull(attack, "attack must not be null");
        this.defenderInitiallyImmune = defenderInitiallyImmune;
    }

    public EffectContext effects() {
        return effects;
    }

    public AttackDefinition attack() {
        return attack;
    }

    public boolean defenderInitiallyImmune() {
        return defenderInitiallyImmune;
    }

    public PokemonInPlay attacker() {
        return effects.state().player(effects.actor()).active();
    }

    public PokemonInPlay defender() {
        return effects.state().player(effects.actor().opponent()).active();
    }

    public int dealCalculatedDamage(int baseDamage) {
        PlayerId actor = effects.actor();
        PlayerId defenderOwner = actor.opponent();
        PlayerState attackerState = effects.state().player(actor);
        PokemonInPlay currentAttacker = attackerState.active();
        PokemonInPlay currentDefender = effects.state().player(defenderOwner).active();
        if (currentAttacker == null || currentDefender == null) {
            return 0;
        }

        int damageDealt = DamageCalculator.calculateDamage(
                currentAttacker,
                currentDefender,
                baseDamage,
                attackerState.plusPowerBonus()
        );

        PokemonInPlay damagedDefender = currentDefender.withDamage(currentDefender.damage() + damageDealt);
        effects.replacePokemon(defenderOwner, damagedDefender);

        effects.emit(
                GameEventType.DAMAGE_DEALT,
                actor,
                damagedDefender.activeCard().definition().name() + " recibió " + damageDealt + " de daño ("
                        + damagedDefender.remainingHp() + "/" + damagedDefender.maxHp() + " PS).",
                Map.of(
                        "targetName", damagedDefender.activeCard().definition().name(),
                        "damage", damageDealt,
                        "remainingHp", damagedDefender.remainingHp(),
                        "maxHp", damagedDefender.maxHp()
                )
        );
        return damageDealt;
    }

    public static boolean hasImmuneModifier(PokemonInPlay defender) {
        return defender != null && defender.modifiers().stream()
                .anyMatch(m -> m.type() == ModifierType.IMMUNE_NEXT_TURN);
    }
}
