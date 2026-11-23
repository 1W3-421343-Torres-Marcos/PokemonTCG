package com.pokemontcg.engine.dto;

import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;

import java.util.List;

public record PlayerViewDTO(
        PlayerId viewer,
        int turnNumber,
        PlayerId activePlayer,
        PlayerId startingPlayer,
        TurnPhase phase,
        PlayerId awaitingPromotionPlayer,
        boolean energyAttachedThisTurn,
        boolean retreatedThisTurn,
        boolean canAttackThisTurn,
        boolean mySetupReady,
        boolean opponentSetupReady,
        PlayerId winner,
        WinReason winReason,
        List<CardViewDTO> myHand,
        PokemonViewDTO myActive,
        List<PokemonViewDTO> myBench,
        List<CardViewDTO> myDiscard,
        int myDeckCount,
        int myPrizesRemaining,
        OpponentViewDTO opponent
) {
    public PlayerViewDTO {
        myHand = myHand == null ? List.of() : List.copyOf(myHand);
        myBench = myBench == null ? List.of() : List.copyOf(myBench);
        myDiscard = myDiscard == null ? List.of() : List.copyOf(myDiscard);
    }
}
