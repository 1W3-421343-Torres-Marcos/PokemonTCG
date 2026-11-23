package com.pokemontcg.engine.action;

public record PlayTrainerAction(
        String trainerCardInstanceId,
        String targetPokemonInstanceId
) implements GameAction {
}
