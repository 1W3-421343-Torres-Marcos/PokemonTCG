package com.pokemontcg.deck.domain;

import com.pokemontcg.catalog.domain.CardEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "deck_cards")
public class DeckCardEntity {

    @EmbeddedId
    private DeckCardId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("deckId")
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("cardId")
    @JoinColumn(name = "card_id", nullable = false)
    private CardEntity card;

    @Column(nullable = false)
    private int quantity;

    public DeckCardEntity() {
    }

    public DeckCardEntity(DeckEntity deck, CardEntity card, int quantity) {
        this.id = new DeckCardId(deck.getId(), card.getId());
        this.deck = deck;
        this.card = card;
        this.quantity = quantity;
    }

    public DeckCardId getId() {
        return id;
    }

    public DeckEntity getDeck() {
        return deck;
    }

    public CardEntity getCard() {
        return card;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
