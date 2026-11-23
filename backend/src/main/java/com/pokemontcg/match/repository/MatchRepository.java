package com.pokemontcg.match.repository;

import com.pokemontcg.match.domain.MatchEntity;
import com.pokemontcg.match.domain.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    Optional<MatchEntity> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, MatchStatus status);
}
