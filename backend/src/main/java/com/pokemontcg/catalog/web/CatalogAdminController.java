package com.pokemontcg.catalog.web;

import com.pokemontcg.catalog.dto.SyncResultDTO;
import com.pokemontcg.catalog.service.CatalogSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/catalog")
public class CatalogAdminController {

    private final CatalogSyncService catalogSyncService;

    public CatalogAdminController(CatalogSyncService catalogSyncService) {
        this.catalogSyncService = catalogSyncService;
    }

    @PostMapping("/sync")
    public SyncResultDTO syncSet(
            @RequestParam(defaultValue = "base1") String setId,
            @RequestParam(defaultValue = "en") String lang
    ) {
        return catalogSyncService.syncSet(setId, lang);
    }
}
