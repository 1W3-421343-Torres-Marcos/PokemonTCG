package com.pokemontcg.catalog.domain;

import java.util.List;

public record CardAttack(
        List<String> cost,
        String name,
        String effect,
        String damage
) {
}
