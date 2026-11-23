package com.pokemontcg.engine.action;

import java.util.List;

public record SelectSetupAction(
        String activeInstanceId,
        List<String> benchInstanceIds
) implements GameAction {
    public SelectSetupAction {
        benchInstanceIds = benchInstanceIds == null ? List.of() : List.copyOf(benchInstanceIds);
    }
}
