package com.pokemontcg.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserFavoriteCardId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false, length = 64)
    private String cardId;

    public UserFavoriteCardId() {
    }

    public UserFavoriteCardId(UUID userId, String cardId) {
        this.userId = userId;
        this.cardId = cardId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCardId() {
        return cardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserFavoriteCardId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, cardId);
    }
}
