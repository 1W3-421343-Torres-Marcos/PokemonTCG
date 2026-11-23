package com.pokemontcg.catalog.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TcgdexClientTest {

    private MockRestServiceServer mockServer;
    private TcgdexClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new TcgdexClient(builder, new ObjectMapper(), "https://api.tcgdex.net/v2");
    }

    @Test
    void fetchSetParsesSetAndSummaries() {
        String json = """
                {
                  "id": "base1",
                  "name": "Base Set",
                  "releaseDate": "1999-01-09",
                  "serie": { "id": "base", "name": "Base" },
                  "cardCount": { "total": 102, "official": 102 },
                  "logo": "https://assets.tcgdex.net/en/base/base1/logo",
                  "cards": [
                    { "id": "base1-4", "localId": "4", "name": "Charizard", "image": "https://assets.tcgdex.net/en/base/base1/4" }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.tcgdex.net/v2/en/sets/base1"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        TcgdexSetResponse result = client.fetchSet("base1", "en");

        assertThat(result.id()).isEqualTo("base1");
        assertThat(result.name()).isEqualTo("Base Set");
        assertThat(result.serie().name()).isEqualTo("Base");
        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().get(0).localId()).isEqualTo("4");
        mockServer.verify();
    }

    @Test
    void fetchCardParsesOptionalFieldsAndRawPayload() {
        String json = """
                {
                  "id": "base1-96",
                  "localId": "96",
                  "name": "Double Colorless Energy",
                  "category": "Energy",
                  "energyType": "Special",
                  "effect": "Provides Colorless Colorless energy.",
                  "image": "https://assets.tcgdex.net/en/base/base1/96"
                }
                """;

        mockServer.expect(requestTo("https://api.tcgdex.net/v2/en/cards/base1-96"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        FetchedCardPayload payload = client.fetchCard("base1-96", "en");

        assertThat(payload.detail().id()).isEqualTo("base1-96");
        assertThat(payload.detail().category()).isEqualTo("Energy");
        assertThat(payload.detail().energyType()).isEqualTo("Special");
        assertThat(payload.detail().hp()).isNull();
        assertThat(payload.rawPayload()).containsEntry("name", "Double Colorless Energy");
        mockServer.verify();
    }

    @Test
    void fetchSetThrowsResourceNotFoundOn404() {
        mockServer.expect(requestTo("https://api.tcgdex.net/v2/en/sets/missing"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.fetchSet("missing", "en"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
