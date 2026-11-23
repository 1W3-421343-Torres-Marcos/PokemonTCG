package com.pokemontcg.engine.dto;

import java.util.List;

public record AttackViewDTO(
        int index,
        String name,
        List<String> cost,
        int baseDamage,
        String rawDamage,
        String text,
        boolean canPay
) {
    public AttackViewDTO {
        cost = cost == null ? List.of() : List.copyOf(cost);
    }
}
