package com.pokemontcg.engine.dto;

import com.pokemontcg.engine.model.PlayerId;

import java.util.List;

public record OpponentViewDTO(
        PlayerId playerId,
        int handCount,
        int deckCount,
        int prizesRemaining,
        PokemonViewDTO active,
        List<PokemonViewDTO> bench,
        List<CardViewDTO> discardPile
) {
    public OpponentViewDTO {
        bench = bench == null ? List.of() : List.copyOf(bench);
        discardPile = discardPile == null ? List.of() : List.copyOf(discardPile);
    }
}
