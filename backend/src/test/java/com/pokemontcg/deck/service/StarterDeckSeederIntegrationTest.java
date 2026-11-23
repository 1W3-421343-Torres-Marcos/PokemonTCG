package com.pokemontcg.deck.service;

import com.pokemontcg.auth.dto.RegisterRequest;
import com.pokemontcg.auth.service.AuthService;
import com.pokemontcg.deck.dto.DeckDetailDTO;
import com.pokemontcg.deck.dto.DeckSummaryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "pokemontcg.catalog.startup-sync=false")
@Transactional
class StarterDeckSeederIntegrationTest {

    @Autowired
    private StarterDeckSeeder starterDeckSeeder;

    @Autowired
    private DeckService deckService;

    @Autowired
    private AuthService authService;

    @Test
    void seedsThreeValidStarterDecksIdempotentlyAndClonesForUser() {
        List<DeckSummaryDTO> starters = deckService.listStarterDecks();
        assertThat(starters).hasSize(3);
        assertThat(starters).allSatisfy(deck -> {
            assertThat(deck.starter()).isTrue();
            assertThat(deck.cpuEligible()).isTrue();
            assertThat(deck.valid()).isTrue();
            assertThat(deck.totalCards()).isEqualTo(60);
        });

        int secondRunCreated = starterDeckSeeder.seedStarterDecks();
        assertThat(secondRunCreated).isZero();
        assertThat(deckService.listStarterDecks()).hasSize(3);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID userId = authService.register(new RegisterRequest("clone_" + suffix, "clone_" + suffix + "@test.com", "pass1234")).user().id();

        DeckSummaryDTO starter = starters.get(0);
        DeckDetailDTO cloned = deckService.cloneStarterDeck(starter.id(), userId);

        assertThat(cloned.ownerId()).isEqualTo(userId);
        assertThat(cloned.starter()).isFalse();
        assertThat(cloned.cpuEligible()).isFalse();
        assertThat(cloned.valid()).isTrue();
        assertThat(cloned.totalCards()).isEqualTo(60);
        assertThat(cloned.validation().valid()).isTrue();
        assertThat(deckService.listMyDecks(userId)).hasSize(1);
    }
}
