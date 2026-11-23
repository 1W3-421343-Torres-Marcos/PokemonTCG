package com.pokemontcg.match.web;

import com.pokemontcg.auth.security.AuthenticatedUser;
import com.pokemontcg.match.dto.ActiveMatchSummaryDTO;
import com.pokemontcg.match.dto.CreateMatchRequest;
import com.pokemontcg.match.dto.MatchActionRequest;
import com.pokemontcg.match.dto.MatchTurnResponse;
import com.pokemontcg.match.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchTurnResponse> createMatch(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateMatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.createMatch(user.userId(), request));
    }

    @GetMapping("/active")
    public ResponseEntity<ActiveMatchSummaryDTO> getActiveMatch(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return matchService.getActiveMatch(user.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public MatchTurnResponse getMatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return matchService.getMatch(id, user.userId());
    }

    @PostMapping("/{id}/actions")
    public MatchTurnResponse submitAction(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody MatchActionRequest request
    ) {
        return matchService.submitAction(id, user.userId(), request);
    }

    @PostMapping("/{id}/concede")
    public MatchTurnResponse concedeMatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return matchService.concedeMatch(id, user.userId());
    }
}
