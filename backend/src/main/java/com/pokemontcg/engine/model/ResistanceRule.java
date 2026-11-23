package com.pokemontcg.engine.model;

import java.util.Objects;

public record ResistanceRule(
        String type,
        String value
) {
    public ResistanceRule {
        type = Objects.requireNonNullElse(type, "");
        value = Objects.requireNonNullElse(value, "");
    }
}
