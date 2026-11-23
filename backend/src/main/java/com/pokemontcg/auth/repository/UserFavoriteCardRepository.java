package com.pokemontcg.auth.repository;

import com.pokemontcg.auth.domain.UserFavoriteCardEntity;
import com.pokemontcg.auth.domain.UserFavoriteCardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserFavoriteCardRepository extends JpaRepository<UserFavoriteCardEntity, UserFavoriteCardId> {

    @Query("SELECT f.id.cardId FROM UserFavoriteCardEntity f WHERE f.id.userId = :userId ORDER BY f.createdAt ASC")
    List<String> findCardIdsByUserId(@Param("userId") UUID userId);
}
