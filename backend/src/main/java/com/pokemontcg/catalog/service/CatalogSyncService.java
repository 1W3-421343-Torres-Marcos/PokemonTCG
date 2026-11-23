package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.client.FetchedCardPayload;
import com.pokemontcg.catalog.client.TcgdexClient;
import com.pokemontcg.catalog.client.TcgdexSetResponse;
import com.pokemontcg.catalog.client.TcgdexSetResponse.TcgdexCardSummary;
import com.pokemontcg.catalog.dto.SyncResultDTO;
import com.pokemontcg.catalog.service.CatalogSyncMapper.PreparedCardData;
import com.pokemontcg.catalog.service.CatalogSyncMapper.PreparedSetData;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogSyncService {

    private final TcgdexClient tcgdexClient;
    private final CatalogSyncMapper mapper;
    private final CatalogPersistenceService persistenceService;

    public CatalogSyncService(
            TcgdexClient tcgdexClient,
            CatalogSyncMapper mapper,
            CatalogPersistenceService persistenceService
    ) {
        this.tcgdexClient = tcgdexClient;
        this.mapper = mapper;
        this.persistenceService = persistenceService;
    }

    /**
     * Downloads and normalizes all set cards outside any DB transaction,
     * opening a transaction only when persisting the batch.
     */
    public SyncResultDTO syncSet(String setId, String lang) {
        String effectiveLang = (lang == null || lang.isBlank()) ? "en" : lang.trim();
        TcgdexSetResponse setResponse = tcgdexClient.fetchSet(setId, effectiveLang);
        Instant syncedAt = Instant.now();

        PreparedSetData preparedSet = mapper.toPreparedSet(setResponse, syncedAt);
        List<TcgdexCardSummary> summaries = setResponse.cards() != null ? setResponse.cards() : List.of();
        List<PreparedCardData> preparedCards = new ArrayList<>(summaries.size());

        for (TcgdexCardSummary summary : summaries) {
            FetchedCardPayload payload = tcgdexClient.fetchCard(summary.id(), effectiveLang);
            preparedCards.add(mapper.toPreparedCard(payload, preparedSet.id()));
        }

        return persistenceService.upsertCatalogBatch(preparedSet, preparedCards, effectiveLang);
    }
}
