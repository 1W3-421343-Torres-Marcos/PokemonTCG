package com.pokemontcg.engine.action;

public record AttachEnergyAction(
        String energyInstanceId,
        String targetPokemonInstanceId
) implements GameAction {
}
