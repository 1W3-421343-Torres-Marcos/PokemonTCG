package com.pokemontcg.engine.model;

import java.util.Objects;

public record CardInstance(
        String instanceId,
        CardDefinition definition
) {
    public CardInstance {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        definition = Objects.requireNonNull(definition, "definition must not be null");
    }
}
