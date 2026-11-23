package com.pokemontcg.engine.model;

import java.util.Objects;

public record WeaknessRule(
        String type,
        String value
) {
    public WeaknessRule {
        type = Objects.requireNonNullElse(type, "");
        value = Objects.requireNonNullElse(value, "");
    }
}
