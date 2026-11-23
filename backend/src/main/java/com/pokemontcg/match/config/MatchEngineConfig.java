package com.pokemontcg.match.config;

import com.pokemontcg.bot.BotTurnRunner;
import com.pokemontcg.engine.random.DefaultRandomProvider;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.service.GameEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchEngineConfig {

    @Bean
    public GameEngine gameEngine() {
        return new GameEngine();
    }

    @Bean
    public BotTurnRunner botTurnRunner(GameEngine gameEngine) {
        return new BotTurnRunner(gameEngine);
    }

    @Bean
    public RandomProvider randomProvider() {
        return new DefaultRandomProvider();
    }
}
