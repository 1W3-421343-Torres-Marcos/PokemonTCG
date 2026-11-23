package com.pokemontcg.deck.validator;

public record DeckCardMetadata(
        String cardId,
        String name,
        String category,
        String stage,
        String energyType
) {
}
