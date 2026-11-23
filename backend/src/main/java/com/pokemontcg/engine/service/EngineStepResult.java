package com.pokemontcg.engine.service;

import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.model.GameState;

import java.util.List;
import java.util.Objects;

public record EngineStepResult(
        GameState state,
        List<GameEvent> events
) {
    public EngineStepResult {
        state = Objects.requireNonNull(state, "state must not be null");
        events = events == null ? List.of() : List.copyOf(events);
    }
}
