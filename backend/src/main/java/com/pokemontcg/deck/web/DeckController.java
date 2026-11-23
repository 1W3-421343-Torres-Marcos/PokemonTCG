package com.pokemontcg.deck.web;

import com.pokemontcg.auth.security.AuthenticatedUser;
import com.pokemontcg.deck.dto.DeckDetailDTO;
import com.pokemontcg.deck.dto.DeckSummaryDTO;
import com.pokemontcg.deck.dto.SaveDeckRequest;
import com.pokemontcg.deck.service.DeckService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public List<DeckSummaryDTO> listMyDecks(@AuthenticationPrincipal AuthenticatedUser user) {
        return deckService.listMyDecks(user.userId());
    }

    @GetMapping("/starters")
    public List<DeckSummaryDTO> listStarterDecks() {
        return deckService.listStarterDecks();
    }

    @PostMapping("/starters/{id}/clone")
    public ResponseEntity<DeckDetailDTO> cloneStarterDeck(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.cloneStarterDeck(id, user.userId()));
    }

    @GetMapping("/{id}")
    public DeckDetailDTO getDeck(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return deckService.getDeck(id, user.userId());
    }

    @PostMapping
    public ResponseEntity<DeckDetailDTO> createDeck(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SaveDeckRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.createDeck(user.userId(), request));
    }

    @PutMapping("/{id}")
    public DeckDetailDTO updateDeck(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SaveDeckRequest request
    ) {
        return deckService.updateDeck(id, user.userId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        deckService.deleteDeck(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
