package com.pokemontcg.deck.repository;

import com.pokemontcg.deck.domain.DeckEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<DeckEntity, UUID> {

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    List<DeckEntity> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    List<DeckEntity> findByStarterTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    Optional<DeckEntity> findWithCardsById(UUID id);

    boolean existsByStarterTrueAndNameIgnoreCase(String name);
}
