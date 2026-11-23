package com.pokemontcg.deck.service;

import com.pokemontcg.auth.dto.RegisterRequest;
import com.pokemontcg.auth.service.AuthService;
import com.pokemontcg.common.exception.ForbiddenException;
import com.pokemontcg.deck.dto.DeckDetailDTO;
import com.pokemontcg.deck.dto.SaveDeckCardRequest;
import com.pokemontcg.deck.dto.SaveDeckRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "pokemontcg.catalog.startup-sync=false")
@Transactional
class DeckCrudPostgresIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DeckService deckService;

    @Test
    void createConsolidatesDuplicateCardIdsSupportsDraftsAndEnforcesOwnershipInPostgres() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID ownerId = authService.register(new RegisterRequest("deckuser_" + suffix, "du_" + suffix + "@test.com", "pass1234")).user().id();
        UUID otherId = authService.register(new RegisterRequest("otheruser_" + suffix, "ou_" + suffix + "@test.com", "pass1234")).user().id();

        SaveDeckRequest draftReq = new SaveDeckRequest(
                "My Fire Draft",
                "Work in progress",
                List.of(
                        new SaveDeckCardRequest("base1-46", 2),
                        new SaveDeckCardRequest("base1-46", 2),
                        new SaveDeckCardRequest("base1-98", 6)
                )
        );

        DeckDetailDTO draft = deckService.createDeck(ownerId, draftReq);
        assertThat(draft.valid()).isFalse();
        assertThat(draft.totalCards()).isEqualTo(10);
        assertThat(draft.cards()).hasSize(2);
        assertThat(draft.cards().get(0).quantity()).isEqualTo(4);

        SaveDeckRequest validReq = new SaveDeckRequest(
                "Complete Fire Deck",
                "60 cards",
                List.of(
                        new SaveDeckCardRequest("base1-46", 4),
                        new SaveDeckCardRequest("base1-24", 3),
                        new SaveDeckCardRequest("base1-4", 2),
                        new SaveDeckCardRequest("base1-91", 4),
                        new SaveDeckCardRequest("base1-88", 4),
                        new SaveDeckCardRequest("base1-98", 43)
                )
        );

        DeckDetailDTO updated = deckService.updateDeck(draft.id(), ownerId, validReq);
        assertThat(updated.valid()).isTrue();
        assertThat(updated.totalCards()).isEqualTo(60);

        assertThatThrownBy(() -> deckService.getDeck(updated.id(), otherId))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> deckService.updateDeck(updated.id(), otherId, validReq))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> deckService.deleteDeck(updated.id(), otherId))
                .isInstanceOf(ForbiddenException.class);

        deckService.deleteDeck(updated.id(), ownerId);
        assertThat(deckService.listMyDecks(ownerId)).isEmpty();
    }
}
