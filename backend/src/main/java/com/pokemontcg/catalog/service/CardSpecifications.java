package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.domain.CardEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<CardEntity> withFilters(
            String q,
            String setId,
            String category,
            String type,
            String stage
    ) {
        return Specification.where(nameContains(q))
                .and(hasSetId(setId))
                .and(hasCategory(category))
                .and(hasType(type))
                .and(hasStage(stage));
    }

    private static Specification<CardEntity> nameContains(String q) {
        if (!hasText(q)) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    private static Specification<CardEntity> hasSetId(String setId) {
        if (!hasText(setId)) {
            return null;
        }
        String normalized = setId.trim();
        return (root, query, cb) -> cb.equal(root.get("set").get("id"), normalized);
    }

    private static Specification<CardEntity> hasCategory(String category) {
        if (!hasText(category)) {
            return null;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), normalized);
    }

    private static Specification<CardEntity> hasType(String type) {
        if (!hasText(type)) {
            return null;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<CardEntity, String> typesJoin = root.join("types", JoinType.INNER);
            return cb.equal(cb.lower(typesJoin), normalized);
        };
    }

    private static Specification<CardEntity> hasStage(String stage) {
        if (!hasText(stage)) {
            return null;
        }
        String normalized = stage.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("stage")), normalized);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
