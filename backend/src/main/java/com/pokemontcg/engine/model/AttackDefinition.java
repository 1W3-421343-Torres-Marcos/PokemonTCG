package com.pokemontcg.engine.model;

import java.util.List;
import java.util.Objects;

public record AttackDefinition(
        String name,
        List<String> cost,
        int baseDamage,
        String rawDamage,
        String text
) {
    public AttackDefinition {
        name = Objects.requireNonNullElse(name, "");
        cost = cost == null ? List.of() : List.copyOf(cost);
        baseDamage = Math.max(0, baseDamage);
    }
}
