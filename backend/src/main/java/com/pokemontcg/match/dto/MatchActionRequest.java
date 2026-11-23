package com.pokemontcg.match.dto;

import com.pokemontcg.engine.action.AttachEnergyAction;
import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.ConcedeAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.EvolveAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.action.PlayBasicAction;
import com.pokemontcg.engine.action.PlayTrainerAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.RetreatAction;
import com.pokemontcg.engine.action.SelectSetupAction;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MatchActionRequest(
        @NotNull(message = "action type is required") MatchActionType type,
        String activeInstanceId,
        List<String> benchInstanceIds,
        String handInstanceId,
        String energyInstanceId,
        String targetPokemonInstanceId,
        Integer attackIndex,
        String benchInstanceId,
        String evolutionCardInstanceId,
        String trainerCardInstanceId,
        List<String> discardEnergyInstanceIds
) {
    public GameAction toGameAction() {
        return switch (type) {
            case SELECT_SETUP -> new SelectSetupAction(activeInstanceId, benchInstanceIds);
            case PLAY_BASIC -> new PlayBasicAction(handInstanceId);
            case ATTACH_ENERGY -> new AttachEnergyAction(energyInstanceId, targetPokemonInstanceId);
            case ATTACK -> {
                if (attackIndex == null) {
                    throw new IllegalArgumentException("attackIndex is required for ATTACK action");
                }
                yield new AttackAction(attackIndex);
            }
            case PROMOTE_ACTIVE -> new PromoteActiveAction(benchInstanceId);
            case END_TURN -> new EndTurnAction();
            case CONCEDE -> new ConcedeAction();
            case EVOLVE -> new EvolveAction(evolutionCardInstanceId, targetPokemonInstanceId);
            case PLAY_TRAINER -> new PlayTrainerAction(trainerCardInstanceId, targetPokemonInstanceId);
            case RETREAT -> new RetreatAction(benchInstanceId, discardEnergyInstanceIds);
        };
    }
}
