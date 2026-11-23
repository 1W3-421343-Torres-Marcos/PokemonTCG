package com.pokemontcg.match.dto;

import com.pokemontcg.bot.BotDifficulty;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.match.domain.MatchStatus;

import java.util.List;
import java.util.UUID;

public record MatchTurnResponse(
        UUID matchId,
        MatchStatus status,
        BotDifficulty botDifficulty,
        String playerDeckName,
        String cpuDeckName,
        PlayerViewDTO playerView,
        List<GameEvent> events
) {
}
