package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.domain.CardSetEntity;
import com.pokemontcg.catalog.dto.CardDetailDTO;
import com.pokemontcg.catalog.dto.CardSetDTO;
import com.pokemontcg.catalog.dto.CardSummaryDTO;
import com.pokemontcg.catalog.dto.PagedResponseDTO;
import com.pokemontcg.catalog.repository.CardRepository;
import com.pokemontcg.catalog.repository.CardSetRepository;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_CARD_SORT = Sort.by(
            Sort.Order.asc("sortOrder"),
            Sort.Order.asc("id")
    );

    private final CardSetRepository cardSetRepository;
    private final CardRepository cardRepository;

    public CatalogQueryService(CardSetRepository cardSetRepository, CardRepository cardRepository) {
        this.cardSetRepository = cardSetRepository;
        this.cardRepository = cardRepository;
    }

    public List<CardSetDTO> listSets() {
        return cardSetRepository.findAll(Sort.by(Sort.Order.asc("releaseDate"), Sort.Order.asc("id"))).stream()
                .map(this::toSetDto)
                .toList();
    }

    public PagedResponseDTO<CardSummaryDTO> searchCards(
            String q,
            String setId,
            String category,
            String type,
            String stage,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(page, size, DEFAULT_CARD_SORT);
        Specification<CardEntity> spec = CardSpecifications.withFilters(q, setId, category, type, stage);
        Page<CardEntity> result = cardRepository.findAll(spec, pageable);

        List<CardSummaryDTO> items = result.getContent().stream()
                .map(this::toSummaryDto)
                .toList();

        return new PagedResponseDTO<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public CardDetailDTO getCardById(String id) {
        CardEntity entity = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + id));
        return toDetailDto(entity);
    }

    private CardSetDTO toSetDto(CardSetEntity set) {
        return new CardSetDTO(
                set.getId(),
                set.getName(),
                set.getSeries(),
                set.getReleaseDate(),
                set.getTotalCards(),
                set.getLogoUrl(),
                set.getSyncedAt()
        );
    }

    private CardSummaryDTO toSummaryDto(CardEntity card) {
        return new CardSummaryDTO(
                card.getId(),
                card.getSet().getId(),
                card.getLocalId(),
                card.getSortOrder(),
                card.getName(),
                card.getCategory(),
                card.getHp(),
                card.getStage(),
                card.getTrainerType(),
                card.getEnergyType(),
                card.getRarity(),
                card.getImageLowUrl(),
                List.copyOf(card.getTypes())
        );
    }

    private CardDetailDTO toDetailDto(CardEntity card) {
        return new CardDetailDTO(
                card.getId(),
                card.getSet().getId(),
                card.getSet().getName(),
                card.getLocalId(),
                card.getSortOrder(),
                card.getName(),
                card.getCategory(),
                card.getHp(),
                card.getStage(),
                card.getEvolveFrom(),
                card.getRetreat(),
                card.getTrainerType(),
                card.getEnergyType(),
                card.getRarity(),
                card.getIllustrator(),
                card.getImageLowUrl(),
                card.getImageHighUrl(),
                List.copyOf(card.getTypes()),
                List.copyOf(card.getAttacks()),
                List.copyOf(card.getWeaknesses()),
                List.copyOf(card.getResistances()),
                card.getEffectText()
        );
    }
}
