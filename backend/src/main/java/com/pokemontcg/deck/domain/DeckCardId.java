package com.pokemontcg.deck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DeckCardId implements Serializable {

    @Column(name = "deck_id", nullable = false)
    private UUID deckId;

    @Column(name = "card_id", nullable = false, length = 64)
    private String cardId;

    public DeckCardId() {
    }

    public DeckCardId(UUID deckId, String cardId) {
        this.deckId = deckId;
        this.cardId = cardId;
    }

    public UUID getDeckId() {
        return deckId;
    }

    public String getCardId() {
        return cardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeckCardId that)) return false;
        return Objects.equals(deckId, that.deckId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckId, cardId);
    }
}
