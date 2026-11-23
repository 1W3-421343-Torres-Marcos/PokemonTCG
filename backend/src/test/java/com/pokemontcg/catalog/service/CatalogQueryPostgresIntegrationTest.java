package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.dto.CardDetailDTO;
import com.pokemontcg.catalog.dto.CardSummaryDTO;
import com.pokemontcg.catalog.dto.PagedResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "pokemontcg.catalog.startup-sync=false")
class CatalogQueryPostgresIntegrationTest {

    @Autowired
    private CatalogQueryService catalogQueryService;

    @Test
    void searchCardsReturnsNumericalOrderAndFiltersByTypeAndCategoryAgainstPostgres() {
        PagedResponseDTO<CardSummaryDTO> firstPage = catalogQueryService.searchCards(null, "base1", null, null, null, 0, 12);
        assertThat(firstPage.totalElements()).isEqualTo(102);
        assertThat(firstPage.items().stream().map(CardSummaryDTO::sortOrder))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        PagedResponseDTO<CardSummaryDTO> firePokemon = catalogQueryService.searchCards(null, "base1", "Pokemon", "Fire", null, 0, 50);
        assertThat(firePokemon.items()).isNotEmpty();
        assertThat(firePokemon.items()).allSatisfy(card -> assertThat(card.types()).contains("Fire"));
        assertThat(firePokemon.items()).extracting(CardSummaryDTO::name).contains("Charizard", "Charmander");

        PagedResponseDTO<CardSummaryDTO> energies = catalogQueryService.searchCards(null, "base1", "Energy", null, null, 0, 20);
        assertThat(energies.totalElements()).isEqualTo(7);
        assertThat(energies.items().stream().filter(c -> "Special".equals(c.energyType())).count()).isEqualTo(1);
        assertThat(energies.items().stream().filter(c -> "Basic".equals(c.energyType())).count()).isEqualTo(6);

        CardDetailDTO dce = catalogQueryService.getCardById("base1-96");
        assertThat(dce.name()).isEqualTo("Double Colorless Energy");
        assertThat(dce.energyType()).isEqualTo("Special");
    }
}
