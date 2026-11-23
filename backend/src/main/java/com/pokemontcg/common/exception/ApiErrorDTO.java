package com.pokemontcg.common.exception;

import java.time.Instant;

public record ApiErrorDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
