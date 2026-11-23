package com.pokemontcg.catalog.client;

import java.util.Map;

public record FetchedCardPayload(
        TcgdexCardDetailResponse detail,
        Map<String, Object> rawPayload
) {
}
