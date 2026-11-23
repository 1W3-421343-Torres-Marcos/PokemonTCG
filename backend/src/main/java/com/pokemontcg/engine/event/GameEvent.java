package com.pokemontcg.engine.event;

import com.pokemontcg.engine.model.PlayerId;

import java.util.Map;
import java.util.Objects;

public record GameEvent(
        long sequence,
        GameEventType type,
        PlayerId actor,
        String message,
        Map<String, Object> metadata
) {
    public GameEvent {
        type = Objects.requireNonNull(type, "event type must not be null");
        message = Objects.requireNonNullElse(message, "");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
