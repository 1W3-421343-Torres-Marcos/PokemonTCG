package com.pokemontcg.engine.model;

import java.util.List;

public record PlayerState(
        List<CardInstance> deck,
        List<CardInstance> hand,
        PokemonInPlay active,
        List<PokemonInPlay> bench,
        List<CardInstance> prizes,
        List<CardInstance> discardPile,
        boolean energyAttachedThisTurn,
        boolean retreatedThisTurn,
        boolean setupReady,
        int mulliganCount,
        int plusPowerBonus
) {
    public PlayerState {
        deck = deck == null ? List.of() : List.copyOf(deck);
        hand = hand == null ? List.of() : List.copyOf(hand);
        bench = bench == null ? List.of() : List.copyOf(bench);
        prizes = prizes == null ? List.of() : List.copyOf(prizes);
        discardPile = discardPile == null ? List.of() : List.copyOf(discardPile);
        mulliganCount = Math.max(0, mulliganCount);
        plusPowerBonus = Math.max(0, plusPowerBonus);
    }

    public PlayerState(
            List<CardInstance> deck,
            List<CardInstance> hand,
            PokemonInPlay active,
            List<PokemonInPlay> bench,
            List<CardInstance> prizes,
            List<CardInstance> discardPile,
            boolean energyAttachedThisTurn,
            boolean retreatedThisTurn,
            boolean setupReady,
            int mulliganCount
    ) {
        this(deck, hand, active, bench, prizes, discardPile,
                energyAttachedThisTurn, retreatedThisTurn, setupReady, mulliganCount, 0);
    }

    public static PlayerState initial(List<CardInstance> deck, List<CardInstance> hand, int mulliganCount) {
        return new PlayerState(
                deck,
                hand,
                null,
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                false,
                mulliganCount,
                0
        );
    }
}
