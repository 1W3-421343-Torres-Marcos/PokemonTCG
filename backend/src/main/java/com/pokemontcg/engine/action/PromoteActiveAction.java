package com.pokemontcg.engine.action;

public record PromoteActiveAction(
        String benchInstanceId
) implements GameAction {
}
