package com.pokemontcg.catalog.dto;

import java.util.List;

public record PagedResponseDTO<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
