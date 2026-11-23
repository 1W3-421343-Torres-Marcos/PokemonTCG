package com.pokemontcg.catalog.web;

import com.pokemontcg.auth.security.JwtAuthenticationFilter;
import com.pokemontcg.auth.security.JwtService;
import com.pokemontcg.auth.security.SecurityConfig;
import com.pokemontcg.catalog.domain.CardAttack;
import com.pokemontcg.catalog.domain.CardTypeModifier;
import com.pokemontcg.catalog.dto.CardDetailDTO;
import com.pokemontcg.catalog.dto.CardSetDTO;
import com.pokemontcg.catalog.dto.CardSummaryDTO;
import com.pokemontcg.catalog.dto.PagedResponseDTO;
import com.pokemontcg.catalog.dto.SyncResultDTO;
import com.pokemontcg.catalog.service.CatalogQueryService;
import com.pokemontcg.catalog.service.CatalogSyncService;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CatalogController.class, CatalogAdminController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogQueryService catalogQueryService;

    @MockBean
    private CatalogSyncService catalogSyncService;

    @MockBean
    private JwtService jwtService;

    @Test
    void listSetsReturnsSyncedSets() throws Exception {
        when(catalogQueryService.listSets()).thenReturn(List.of(
                new CardSetDTO("base1", "Base Set", "Base", LocalDate.of(1999, 1, 9), 102, null, Instant.now())
        ));

        mockMvc.perform(get("/api/sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("base1"))
                .andExpect(jsonPath("$[0].totalCards").value(102));
    }

    @Test
    void searchCardsPassesFiltersAndReturnsPagedResult() throws Exception {
        PagedResponseDTO<CardSummaryDTO> page = new PagedResponseDTO<>(
                List.of(new CardSummaryDTO(
                        "base1-4", "base1", "4", 4, "Charizard", "Pokemon",
                        120, "Stage2", null, null, "Rare Holo",
                        "https://assets.tcgdex.net/en/base/base1/4/low.webp", List.of("Fire")
                )),
                0, 12, 1, 1
        );
        when(catalogQueryService.searchCards("char", "base1", "Pokemon", "Fire", "Stage2", 0, 12))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards")
                        .param("q", "char")
                        .param("setId", "base1")
                        .param("category", "Pokemon")
                        .param("type", "Fire")
                        .param("stage", "Stage2")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("base1-4"))
                .andExpect(jsonPath("$.items[0].sortOrder").value(4))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(catalogQueryService).searchCards("char", "base1", "Pokemon", "Fire", "Stage2", 0, 12);
    }

    @Test
    void getCardReturnsDetailOr404() throws Exception {
        when(catalogQueryService.getCardById("base1-4")).thenReturn(new CardDetailDTO(
                "base1-4", "base1", "Base Set", "4", 4, "Charizard", "Pokemon",
                120, "Stage2", "Charmeleon", 3, null, null, "Rare Holo", "Mitsuhiro Arita",
                "https://assets.tcgdex.net/en/base/base1/4/low.webp",
                "https://assets.tcgdex.net/en/base/base1/4/high.webp",
                List.of("Fire"),
                List.of(new CardAttack(List.of("Fire", "Fire", "Fire", "Fire"), "Fire Spin", "Discard 2 Energy.", "100")),
                List.of(new CardTypeModifier("Water", "×2")),
                List.of(new CardTypeModifier("Fighting", "-30")),
                null
        ));
        when(catalogQueryService.getCardById("missing")).thenThrow(new ResourceNotFoundException("Card not found: missing"));

        mockMvc.perform(get("/api/cards/base1-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolveFrom").value("Charmeleon"))
                .andExpect(jsonPath("$.retreat").value(3));

        mockMvc.perform(get("/api/cards/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminSyncTriggersSetSync() throws Exception {
        when(catalogSyncService.syncSet("base1", "en"))
                .thenReturn(new SyncResultDTO("base1", "en", 102, Instant.now()));

        mockMvc.perform(post("/api/admin/catalog/sync")
                        .param("setId", "base1")
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setId").value("base1"))
                .andExpect(jsonPath("$.syncedCards").value(102));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminSyncRejectsNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/admin/catalog/sync"))
                .andExpect(status().isForbidden());
    }
}
