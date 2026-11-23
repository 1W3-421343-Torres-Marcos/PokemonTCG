package com.pokemontcg.engine.model;

import java.util.Objects;

public record ActiveModifier(
        String sourceCardName,
        ModifierType type,
        int amount,
        int expiresAfterTurn
) {
    public ActiveModifier {
        sourceCardName = Objects.requireNonNullElse(sourceCardName, "");
        type = Objects.requireNonNull(type, "modifier type must not be null");
        amount = Math.max(0, amount);
    }
}
