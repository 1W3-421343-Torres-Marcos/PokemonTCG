package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.client.FetchedCardPayload;
import com.pokemontcg.catalog.client.TcgdexCardDetailResponse;
import com.pokemontcg.catalog.client.TcgdexCardDetailResponse.TcgdexAttack;
import com.pokemontcg.catalog.client.TcgdexCardDetailResponse.TcgdexTypeModifier;
import com.pokemontcg.catalog.client.TcgdexClient;
import com.pokemontcg.catalog.client.TcgdexSetResponse;
import com.pokemontcg.catalog.client.TcgdexSetResponse.TcgdexCardCount;
import com.pokemontcg.catalog.client.TcgdexSetResponse.TcgdexCardSummary;
import com.pokemontcg.catalog.client.TcgdexSetResponse.TcgdexSerieSummary;
import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.domain.CardSetEntity;
import com.pokemontcg.catalog.dto.SyncResultDTO;
import com.pokemontcg.catalog.repository.CardRepository;
import com.pokemontcg.catalog.repository.CardSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock
    private TcgdexClient tcgdexClient;
    @Mock
    private CardSetRepository cardSetRepository;
    @Mock
    private CardRepository cardRepository;

    private final CatalogSyncMapper mapper = new CatalogSyncMapper();

    @Test
    void syncSetDownloadsAllCardsBeforeOpeningPersistenceTransaction() throws NoSuchMethodException {
        CatalogPersistenceService persistenceService = new CatalogPersistenceService(cardSetRepository, cardRepository);
        CatalogSyncService syncService = new CatalogSyncService(tcgdexClient, mapper, persistenceService);

        assertThat(CatalogSyncService.class.getDeclaredMethod("syncSet", String.class, String.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(CatalogPersistenceService.class.getDeclaredMethod(
                        "upsertCatalogBatch",
                        CatalogSyncMapper.PreparedSetData.class,
                        List.class,
                        String.class)
                .isAnnotationPresent(Transactional.class)).isTrue();

        TcgdexSetResponse setResponse = new TcgdexSetResponse(
                "base1", "Base Set", new TcgdexSerieSummary("base", "Base"), "1999-01-09",
                new TcgdexCardCount(102, 102), "https://assets.tcgdex.net/en/base/base1/logo",
                List.of(
                        new TcgdexCardSummary("base1-4", "4", "Charizard", null),
                        new TcgdexCardSummary("base1-96", "96", "Double Colorless Energy", null),
                        new TcgdexCardSummary("base1-98", "98", "Fire Energy", null),
                        new TcgdexCardSummary("base1-80", "80", "Defender", null)
                )
        );

        when(tcgdexClient.fetchSet("base1", "en")).thenReturn(setResponse);
        when(tcgdexClient.fetchCard("base1-4", "en")).thenReturn(new FetchedCardPayload(
                new TcgdexCardDetailResponse("base1-4", "4", "Charizard", "Pokemon",
                        "https://assets.tcgdex.net/en/base/base1/4", 120, List.of("Fire"),
                        "Charmeleon", "Stage2", 3, "Rare Holo", "Mitsuhiro Arita",
                        null, null, null, null,
                        List.of(new TcgdexAttack(List.of("Fire", "Fire", "Fire", "Fire"), "Fire Spin", "Discard 2 Energy.", "100")),
                        List.of(new TcgdexTypeModifier("Water", "×2")),
                        List.of(new TcgdexTypeModifier("Fighting", "-30"))),
                Map.of("id", "base1-4")
        ));
        when(tcgdexClient.fetchCard("base1-96", "en")).thenReturn(new FetchedCardPayload(
                new TcgdexCardDetailResponse("base1-96", "96", "Double Colorless Energy", "Energy",
                        "https://assets.tcgdex.net/en/base/base1/96/", null, null,
                        null, null, null, "Uncommon", null,
                        null, "Special", "Provides 2 Colorless energy.", null,
                        null, null, null),
                Map.of("id", "base1-96")
        ));
        when(tcgdexClient.fetchCard("base1-98", "en")).thenReturn(new FetchedCardPayload(
                new TcgdexCardDetailResponse("base1-98", "98", "Fire Energy", "Energy",
                        "https://assets.tcgdex.net/en/base/base1/98", null, null,
                        null, null, null, "Common", null,
                        null, "Normal", null, null,
                        null, null, null),
                Map.of("id", "base1-98")
        ));
        when(tcgdexClient.fetchCard("base1-80", "en")).thenReturn(new FetchedCardPayload(
                new TcgdexCardDetailResponse("base1-80", "80", "Defender", "Trainer",
                        null, null, null,
                        null, null, null, "Uncommon", "Keiji Kinebuchi",
                        "Item", null, "Attach Defender to 1 of your Pokémon.", null,
                        null, null, null),
                Map.of("id", "base1-80")
        ));

        when(cardSetRepository.findById("base1")).thenReturn(Optional.empty());
        when(cardSetRepository.save(any(CardSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepository.findAllById(anyList())).thenReturn(List.of());
        when(cardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResultDTO result = syncService.syncSet("base1", "en");

        assertThat(result.setId()).isEqualTo("base1");
        assertThat(result.syncedCards()).isEqualTo(4);

        InOrder order = inOrder(tcgdexClient, cardSetRepository, cardRepository);
        order.verify(tcgdexClient).fetchSet("base1", "en");
        order.verify(tcgdexClient).fetchCard("base1-4", "en");
        order.verify(tcgdexClient).fetchCard("base1-96", "en");
        order.verify(tcgdexClient).fetchCard("base1-98", "en");
        order.verify(tcgdexClient).fetchCard("base1-80", "en");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CardEntity>> savedCardsCaptor = ArgumentCaptor.forClass(List.class);
        order.verify(cardSetRepository).save(any(CardSetEntity.class));
        order.verify(cardRepository).saveAll(savedCardsCaptor.capture());
        List<CardEntity> saved = savedCardsCaptor.getValue();

        CardEntity charizard = saved.stream().filter(c -> c.getId().equals("base1-4")).findFirst().orElseThrow();
        assertThat(charizard.getSortOrder()).isEqualTo(4);
        assertThat(charizard.getEvolveFrom()).isEqualTo("Charmeleon");
        assertThat(charizard.getRetreat()).isEqualTo(3);
        assertThat(charizard.getImageLowUrl()).isEqualTo("https://assets.tcgdex.net/en/base/base1/4/low.webp");
        assertThat(charizard.getImageHighUrl()).isEqualTo("https://assets.tcgdex.net/en/base/base1/4/high.webp");
        assertThat(charizard.getEnergyType()).isNull();

        CardEntity doubleColorless = saved.stream().filter(c -> c.getId().equals("base1-96")).findFirst().orElseThrow();
        assertThat(doubleColorless.getEnergyType()).isEqualTo("Special");
        assertThat(doubleColorless.getImageLowUrl()).isEqualTo("https://assets.tcgdex.net/en/base/base1/96/low.webp");

        CardEntity fireEnergy = saved.stream().filter(c -> c.getId().equals("base1-98")).findFirst().orElseThrow();
        assertThat(fireEnergy.getEnergyType()).isEqualTo("Basic");

        CardEntity defender = saved.stream().filter(c -> c.getId().equals("base1-80")).findFirst().orElseThrow();
        assertThat(defender.getTrainerType()).isEqualTo("Item");
        assertThat(defender.getImageLowUrl()).isNull();
    }

    @Test
    void upsertCatalogBatchReusesExistingEntitiesIdempotently() {
        CatalogPersistenceService persistenceService = new CatalogPersistenceService(cardSetRepository, cardRepository);
        CardSetEntity existingSet = new CardSetEntity();
        existingSet.setId("base1");
        CardEntity existingCard = new CardEntity();
        existingCard.setId("base1-98");
        existingCard.setName("Old Name");

        when(cardSetRepository.findById("base1")).thenReturn(Optional.of(existingSet));
        when(cardSetRepository.save(existingSet)).thenReturn(existingSet);
        when(cardRepository.findAllById(List.of("base1-98"))).thenReturn(List.of(existingCard));
        when(cardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var setData = new CatalogSyncMapper.PreparedSetData("base1", "Base Set", "Base", null, 102, null, Instant.now());
        var cardData = new CatalogSyncMapper.PreparedCardData(
                "base1-98", "base1", "98", 98, "Fire Energy", "Energy",
                null, null, null, null, null, "Basic", "Common", null,
                null, null, java.util.Set.of(), List.of(), List.of(), List.of(), null, Map.of()
        );

        persistenceService.upsertCatalogBatch(setData, List.of(cardData), "en");

        assertThat(existingCard.getName()).isEqualTo("Fire Energy");
        assertThat(existingCard.getEnergyType()).isEqualTo("Basic");
        assertThat(existingSet.getName()).isEqualTo("Base Set");
    }
}
