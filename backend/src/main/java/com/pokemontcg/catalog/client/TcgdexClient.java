package com.pokemontcg.catalog.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TcgdexClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TcgdexClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${pokemontcg.tcgdex.base-url:https://api.tcgdex.net/v2}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public TcgdexSetResponse fetchSet(String setId, String lang) {
        TcgdexSetResponse response = restClient.get()
                .uri("/{lang}/sets/{setId}", lang, setId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new ResourceNotFoundException("TCGdex set not found: " + setId + " (" + lang + ")");
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("TCGdex error fetching set " + setId + ": HTTP " + res.getStatusCode());
                })
                .body(TcgdexSetResponse.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from TCGdex for set: " + setId);
        }
        return response;
    }

    public FetchedCardPayload fetchCard(String cardId, String lang) {
        JsonNode node = restClient.get()
                .uri("/{lang}/cards/{cardId}", lang, cardId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new ResourceNotFoundException("TCGdex card not found: " + cardId + " (" + lang + ")");
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("TCGdex error fetching card " + cardId + ": HTTP " + res.getStatusCode());
                })
                .body(JsonNode.class);

        if (node == null || node.isNull()) {
            throw new IllegalStateException("Empty response from TCGdex for card: " + cardId);
        }

        try {
            TcgdexCardDetailResponse detail = objectMapper.treeToValue(node, TcgdexCardDetailResponse.class);
            Map<String, Object> raw = objectMapper.convertValue(node, MAP_TYPE);
            return new FetchedCardPayload(detail, raw);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize TCGdex card payload for " + cardId, ex);
        }
    }
}
