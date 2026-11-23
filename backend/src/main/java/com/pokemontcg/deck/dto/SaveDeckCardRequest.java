package com.pokemontcg.deck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SaveDeckCardRequest(
        @NotBlank(message = "Card ID is required")
        String cardId,

        @Min(value = 1, message = "Quantity must be >= 1")
        int quantity
) {
}
