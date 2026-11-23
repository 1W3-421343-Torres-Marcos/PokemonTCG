package com.pokemontcg.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_favorite_cards")
public class UserFavoriteCardEntity {

    @EmbeddedId
    private UserFavoriteCardId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UserFavoriteCardEntity() {
    }

    public UserFavoriteCardEntity(UUID userId, String cardId, Instant createdAt) {
        this.id = new UserFavoriteCardId(userId, cardId);
        this.createdAt = createdAt;
    }

    public UserFavoriteCardId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
