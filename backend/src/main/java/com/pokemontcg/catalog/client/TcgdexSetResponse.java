package com.pokemontcg.catalog.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TcgdexSetResponse(
        String id,
        String name,
        TcgdexSerieSummary serie,
        String releaseDate,
        TcgdexCardCount cardCount,
        String logo,
        List<TcgdexCardSummary> cards
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TcgdexSerieSummary(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TcgdexCardCount(Integer total, Integer official) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TcgdexCardSummary(String id, String localId, String name, String image) {
    }
}
