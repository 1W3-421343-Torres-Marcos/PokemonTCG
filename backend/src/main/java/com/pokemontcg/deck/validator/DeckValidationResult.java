package com.pokemontcg.deck.validator;

import java.util.List;

public record DeckValidationResult(
        boolean valid,
        List<String> errors
) {
}
