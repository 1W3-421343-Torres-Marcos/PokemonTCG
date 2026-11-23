package com.pokemontcg.match.dto;

import com.pokemontcg.bot.BotDifficulty;
import com.pokemontcg.engine.model.TurnPhase;

import java.time.Instant;
import java.util.UUID;

public record ActiveMatchSummaryDTO(
        UUID matchId,
        String playerDeckName,
        String cpuDeckName,
        BotDifficulty botDifficulty,
        int turnNumber,
        TurnPhase phase,
        Instant updatedAt
) {
}
