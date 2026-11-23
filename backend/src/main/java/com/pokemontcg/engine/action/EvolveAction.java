package com.pokemontcg.engine.action;

public record EvolveAction(
        String evolutionCardInstanceId,
        String targetPokemonInstanceId
) implements GameAction {
}
