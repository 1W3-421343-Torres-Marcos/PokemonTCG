package com.pokemontcg.deck.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.auth.security.JwtAuthenticationFilter;
import com.pokemontcg.auth.security.JwtService;
import com.pokemontcg.auth.security.SecurityConfig;
import com.pokemontcg.common.exception.ForbiddenException;
import com.pokemontcg.deck.dto.DeckDetailDTO;
import com.pokemontcg.deck.dto.DeckSummaryDTO;
import com.pokemontcg.deck.dto.SaveDeckCardRequest;
import com.pokemontcg.deck.dto.SaveDeckRequest;
import com.pokemontcg.deck.service.DeckService;
import com.pokemontcg.deck.validator.DeckValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeckController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "pokemontcg.jwt.secret=test-secret-key-for-pokemon-tcg-at-least-32-bytes",
        "pokemontcg.jwt.expiration-ms=3600000"
})
class DeckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private DeckService deckService;

    @Test
    void deckEndpointsRequireAuthAndEnforceOwnership() throws Exception {
        mockMvc.perform(get("/api/decks")).andExpect(status().isUnauthorized());

        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "gary", "USER");

        when(deckService.listMyDecks(userId)).thenReturn(List.of(
                new DeckSummaryDTO(deckId, "Draft", null, false, false, false, 10, Instant.now(), Instant.now())
        ));
        DeckDetailDTO detail = new DeckDetailDTO(
                deckId, userId, "Draft", null, false, false, false, 10,
                new DeckValidationResult(false, List.of("Deck must contain exactly 60 cards")),
                List.of(), Instant.now(), Instant.now()
        );
        when(deckService.createDeck(eq(userId), any(SaveDeckRequest.class))).thenReturn(detail);
        when(deckService.updateDeck(eq(deckId), eq(userId), any(SaveDeckRequest.class))).thenReturn(detail);
        when(deckService.getDeck(deckId, userId)).thenThrow(new ForbiddenException("Forbidden"));
        doNothing().when(deckService).deleteDeck(deckId, userId);

        mockMvc.perform(get("/api/decks").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Draft"));

        SaveDeckRequest saveReq = new SaveDeckRequest("Draft", null, List.of(new SaveDeckCardRequest("base1-46", 4)));
        mockMvc.perform(post("/api/decks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(put("/api/decks/" + deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
