package com.pokemontcg.catalog.repository;

import com.pokemontcg.catalog.domain.CardSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetRepository extends JpaRepository<CardSetEntity, String> {
}
