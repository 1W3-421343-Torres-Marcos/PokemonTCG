package com.pokemontcg.match.dto;

import com.pokemontcg.bot.BotDifficulty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMatchRequest(
        @NotNull(message = "playerDeckId is required") UUID playerDeckId,
        @NotNull(message = "cpuDeckId is required") UUID cpuDeckId,
        BotDifficulty botDifficulty
) {
}
