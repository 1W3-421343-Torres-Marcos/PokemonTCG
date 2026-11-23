package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;

import java.util.List;

/**
 * Strategy contract for Trainer card legality, target projection and execution.
 */
public interface TrainerEffect {

    List<String> validTargetIds(GameState state, PlayerId actor, CardInstance trainerCard);

    default boolean canPlay(GameState state, PlayerId actor, CardInstance trainerCard) {
        return !validTargetIds(state, actor, trainerCard).isEmpty();
    }

    void execute(EffectContext ctx, CardInstance trainerCard, String targetId);
}
