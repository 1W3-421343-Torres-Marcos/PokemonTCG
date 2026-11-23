package com.pokemontcg.engine.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record GameState(
        int turnNumber,
        PlayerId activePlayer,
        PlayerId startingPlayer,
        TurnPhase phase,
        PlayerId awaitingPromotionPlayer,
        Map<PlayerId, PlayerState> players,
        PlayerId winner,
        WinReason winReason,
        long nextEventSequence
) {
    public GameState {
        turnNumber = Math.max(0, turnNumber);
        activePlayer = Objects.requireNonNull(activePlayer, "activePlayer must not be null");
        startingPlayer = Objects.requireNonNull(startingPlayer, "startingPlayer must not be null");
        phase = Objects.requireNonNull(phase, "phase must not be null");
        players = players == null ? Map.of() : Map.copyOf(players);
        nextEventSequence = Math.max(1L, nextEventSequence);
    }

    public PlayerState player(PlayerId playerId) {
        PlayerState state = players.get(playerId);
        if (state == null) {
            throw new IllegalArgumentException("Missing player state for " + playerId);
        }
        return state;
    }

    public GameState withPlayer(PlayerId playerId, PlayerState updatedState) {
        EnumMap<PlayerId, PlayerState> nextPlayers = new EnumMap<>(PlayerId.class);
        nextPlayers.putAll(players);
        nextPlayers.put(playerId, updatedState);
        return new GameState(
                turnNumber,
                activePlayer,
                startingPlayer,
                phase,
                awaitingPromotionPlayer,
                nextPlayers,
                winner,
                winReason,
                nextEventSequence
        );
    }
}
