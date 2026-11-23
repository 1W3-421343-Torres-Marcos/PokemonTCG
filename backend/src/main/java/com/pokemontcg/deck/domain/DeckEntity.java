package com.pokemontcg.deck.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "decks")
public class DeckEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_starter", nullable = false)
    private boolean starter;

    @Column(name = "cpu_eligible", nullable = false)
    private boolean cpuEligible;

    @Column(name = "is_valid", nullable = false)
    private boolean valid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckCardEntity> cards = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStarter() {
        return starter;
    }

    public void setStarter(boolean starter) {
        this.starter = starter;
    }

    public boolean isCpuEligible() {
        return cpuEligible;
    }

    public void setCpuEligible(boolean cpuEligible) {
        this.cpuEligible = cpuEligible;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DeckCardEntity> getCards() {
        return cards;
    }
}
