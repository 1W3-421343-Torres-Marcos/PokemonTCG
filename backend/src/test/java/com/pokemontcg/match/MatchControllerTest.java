package com.pokemontcg.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.auth.security.JwtAuthenticationFilter;
import com.pokemontcg.auth.security.JwtService;
import com.pokemontcg.auth.security.SecurityConfig;
import com.pokemontcg.bot.BotDifficulty;
import com.pokemontcg.common.exception.ForbiddenException;
import com.pokemontcg.engine.dto.OpponentViewDTO;
import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.match.domain.MatchStatus;
import com.pokemontcg.match.dto.ActiveMatchSummaryDTO;
import com.pokemontcg.match.dto.CreateMatchRequest;
import com.pokemontcg.match.dto.MatchActionRequest;
import com.pokemontcg.match.dto.MatchActionType;
import com.pokemontcg.match.dto.MatchTurnResponse;
import com.pokemontcg.match.service.MatchService;
import com.pokemontcg.match.web.MatchController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "pokemontcg.jwt.secret=test-secret-key-for-pokemon-tcg-at-least-32-bytes",
        "pokemontcg.jwt.expiration-ms=3600000"
})
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private MatchService matchService;

    @Test
    void enforcesAuthOwnershipAndDomainStatusCodes() throws Exception {
        mockMvc.perform(get("/api/matches/active")).andExpect(status().isUnauthorized());

        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "ash", "USER");

        PlayerViewDTO sampleView = new PlayerViewDTO(
                PlayerId.PLAYER, 1, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                false, false, false, true, true, null, null,
                List.of(), null, List.of(), List.of(), 47, 6,
                new OpponentViewDTO(PlayerId.CPU, 6, 47, 6, null, List.of(), List.of())
        );
        MatchTurnResponse createdResponse = new MatchTurnResponse(
                matchId,
                MatchStatus.IN_PROGRESS,
                BotDifficulty.BASIC,
                "Brushfire",
                "Blackout",
                sampleView,
                List.of(new GameEvent(1L, GameEventType.GAME_STARTED, PlayerId.PLAYER, "Iniciada", Map.of()))
        );

        when(matchService.createMatch(eq(userId), any(CreateMatchRequest.class))).thenReturn(createdResponse);
        when(matchService.getActiveMatch(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ActiveMatchSummaryDTO(
                        matchId, "Brushfire", "Blackout", BotDifficulty.BASIC, 1, TurnPhase.MAIN_PHASE, Instant.now()
                )));
        when(matchService.getMatch(matchId, userId)).thenThrow(new ForbiddenException("Not owner"));
        when(matchService.submitAction(eq(matchId), eq(userId), any(MatchActionRequest.class)))
                .thenThrow(new IllegalGameActionException("No puedes atacar en Turno 1"))
                .thenThrow(new ObjectOptimisticLockingFailureException("MatchEntity", matchId));

        // 204 when no active match, then 200 with summary
        mockMvc.perform(get("/api/matches/active").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/matches/active").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.turnNumber").value(1));

        // 201 on create
        CreateMatchRequest createReq = new CreateMatchRequest(deckId, deckId, BotDifficulty.BASIC);
        mockMvc.perform(post("/api/matches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.playerView.opponent.handCount").value(6));

        // 403 on other user's match
        mockMvc.perform(get("/api/matches/" + matchId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        // 400 on IllegalGameActionException, then 409 on OptimisticLock
        MatchActionRequest attackReq = new MatchActionRequest(
                MatchActionType.ATTACK, null, null, null, null, null, 0, null, null, null, null
        );
        mockMvc.perform(post("/api/matches/" + matchId + "/actions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No puedes atacar en Turno 1"));

        mockMvc.perform(post("/api/matches/" + matchId + "/actions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isConflict());
    }
}
