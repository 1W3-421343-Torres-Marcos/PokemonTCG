package com.pokemontcg.engine.action;

import java.util.List;

public record RetreatAction(
        String benchInstanceId,
        List<String> discardEnergyInstanceIds
) implements GameAction {
    public RetreatAction {
        discardEnergyInstanceIds = discardEnergyInstanceIds == null ? List.of() : List.copyOf(discardEnergyInstanceIds);
    }
}
