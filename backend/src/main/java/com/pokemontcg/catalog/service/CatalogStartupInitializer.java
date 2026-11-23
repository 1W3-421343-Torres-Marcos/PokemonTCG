package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.repository.CardSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@ConditionalOnProperty(name = "pokemontcg.catalog.startup-sync", havingValue = "true")
public class CatalogStartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogStartupInitializer.class);

    private final CardSetRepository cardSetRepository;
    private final CatalogSyncService catalogSyncService;
    private final String defaultSetId;
    private final String defaultLang;

    public CatalogStartupInitializer(
            CardSetRepository cardSetRepository,
            CatalogSyncService catalogSyncService,
            @Value("${pokemontcg.catalog.default-set-id:base1}") String defaultSetId,
            @Value("${pokemontcg.catalog.default-lang:en}") String defaultLang
    ) {
        this.cardSetRepository = cardSetRepository;
        this.catalogSyncService = catalogSyncService;
        this.defaultSetId = defaultSetId;
        this.defaultLang = defaultLang;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardSetRepository.count() > 0) {
            log.info("Catalog already populated; skipping startup sync.");
            return;
        }
        log.info("Catalog empty; synchronizing initial set '{}' ({})", defaultSetId, defaultLang);
        var result = catalogSyncService.syncSet(defaultSetId, defaultLang);
        log.info("Initial catalog sync complete: {} cards in set '{}'", result.syncedCards(), result.setId());
    }
}
