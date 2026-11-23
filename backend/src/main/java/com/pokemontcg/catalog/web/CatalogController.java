package com.pokemontcg.catalog.web;

import com.pokemontcg.catalog.dto.CardDetailDTO;
import com.pokemontcg.catalog.dto.CardSetDTO;
import com.pokemontcg.catalog.dto.CardSummaryDTO;
import com.pokemontcg.catalog.dto.PagedResponseDTO;
import com.pokemontcg.catalog.service.CatalogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogQueryService catalogQueryService;

    public CatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping("/sets")
    public List<CardSetDTO> listSets() {
        return catalogQueryService.listSets();
    }

    @GetMapping("/cards")
    public PagedResponseDTO<CardSummaryDTO> searchCards(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String setId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return catalogQueryService.searchCards(q, setId, category, type, stage, page, size);
    }

    @GetMapping("/cards/{id}")
    public CardDetailDTO getCard(@PathVariable String id) {
        return catalogQueryService.getCardById(id);
    }
}
