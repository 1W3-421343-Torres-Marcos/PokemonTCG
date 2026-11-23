package com.pokemontcg.bot;

import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PlayerViewProjector;
import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BotTurnRunner {

    public static final int MAX_ACTIONS_PER_TURN = 25;

    private final GameEngine gameEngine;
    private final int maxActionsPerTurn;

    public BotTurnRunner() {
        this(new GameEngine(), MAX_ACTIONS_PER_TURN);
    }

    public BotTurnRunner(GameEngine gameEngine) {
        this(gameEngine, MAX_ACTIONS_PER_TURN);
    }

    public BotTurnRunner(GameEngine gameEngine, int maxActionsPerTurn) {
        this.gameEngine = Objects.requireNonNull(gameEngine, "gameEngine must not be null");
        if (maxActionsPerTurn <= 0) {
            throw new IllegalArgumentException("maxActionsPerTurn must be positive");
        }
        this.maxActionsPerTurn = maxActionsPerTurn;
    }

    public boolean requiresBotAction(GameState state, PlayerId botPlayer) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(botPlayer, "botPlayer must not be null");

        if (state.phase() == TurnPhase.GAME_OVER) {
            return false;
        }
        if (state.phase() == TurnPhase.SETUP) {
            return !state.player(botPlayer).setupReady();
        }
        if (state.awaitingPromotionPlayer() != null) {
            return state.awaitingPromotionPlayer() == botPlayer;
        }
        return state.activePlayer() == botPlayer;
    }

    public EngineStepResult runUntilHumanTurn(GameState initialState, PlayerId botPlayer, BotStrategy strategy) {
        Objects.requireNonNull(initialState, "initialState must not be null");
        Objects.requireNonNull(botPlayer, "botPlayer must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");

        GameState current = initialState;
        List<GameEvent> accumulatedEvents = new ArrayList<>();
        int actionsInTurn = 0;
        int trackedTurn = current.turnNumber();

        while (requiresBotAction(current, botPlayer)) {
            if (current.turnNumber() != trackedTurn) {
                trackedTurn = current.turnNumber();
                actionsInTurn = 0;
            }

            GameAction action;
            if (actionsInTurn >= maxActionsPerTurn) {
                if (current.phase() == TurnPhase.MAIN_PHASE && current.awaitingPromotionPlayer() == null) {
                    action = new EndTurnAction();
                } else {
                    throw new IllegalStateException("Bot exceeded max actions in non-MAIN phase: " + current.phase());
                }
            } else {
                PlayerViewDTO botView = PlayerViewProjector.project(current, botPlayer);
                action = strategy.decideNextAction(botView);
            }

            EngineStepResult step = gameEngine.applyAction(current, botPlayer, action);
            current = step.state();
            accumulatedEvents.addAll(step.events());
            actionsInTurn++;
        }

        return new EngineStepResult(current, accumulatedEvents);
    }
}
