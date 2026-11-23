package com.pokemontcg.bot;

import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.dto.PlayerViewDTO;

public interface BotStrategy {

    GameAction decideNextAction(PlayerViewDTO botView);
}
