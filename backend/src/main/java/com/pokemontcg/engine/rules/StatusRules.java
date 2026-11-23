package com.pokemontcg.engine.rules;

import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;

import java.util.EnumSet;
import java.util.Objects;

public final class StatusRules {

    private StatusRules() {
    }

    public static PokemonInPlay applyStatus(PokemonInPlay target, StatusCondition condition) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(condition, "condition must not be null");

        EnumSet<StatusCondition> next = target.statusConditions().isEmpty()
                ? EnumSet.noneOf(StatusCondition.class)
                : EnumSet.copyOf(target.statusConditions());

        if (isRotationCondition(condition)) {
            next.remove(StatusCondition.ASLEEP);
            next.remove(StatusCondition.PARALYZED);
            next.remove(StatusCondition.CONFUSED);
        }
        next.add(condition);
        return target.withStatusConditions(next);
    }

    public static PokemonInPlay removeStatus(PokemonInPlay target, StatusCondition condition) {
        Objects.requireNonNull(target, "target must not be null");
        if (!target.statusConditions().contains(condition)) {
            return target;
        }
        EnumSet<StatusCondition> next = EnumSet.copyOf(target.statusConditions());
        next.remove(condition);
        return target.withStatusConditions(next);
    }

    public static boolean blocksAttackOrRetreat(PokemonInPlay pokemon) {
        if (pokemon == null) {
            return false;
        }
        return pokemon.statusConditions().contains(StatusCondition.ASLEEP)
                || pokemon.statusConditions().contains(StatusCondition.PARALYZED);
    }

    private static boolean isRotationCondition(StatusCondition condition) {
        return condition == StatusCondition.ASLEEP
                || condition == StatusCondition.PARALYZED
                || condition == StatusCondition.CONFUSED;
    }
}
