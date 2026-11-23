package com.pokemontcg.deck.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveDeckRequest(
        @NotBlank(message = "Deck name is required")
        @Size(max = 120, message = "Deck name must be at most 120 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Cards list is required")
        @Valid
        List<SaveDeckCardRequest> cards
) {
}
