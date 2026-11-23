package com.pokemontcg.deck.service;

import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.repository.CardRepository;
import com.pokemontcg.common.exception.ForbiddenException;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import com.pokemontcg.deck.domain.DeckCardEntity;
import com.pokemontcg.deck.domain.DeckEntity;
import com.pokemontcg.deck.dto.DeckCardItemDTO;
import com.pokemontcg.deck.dto.DeckDetailDTO;
import com.pokemontcg.deck.dto.DeckSummaryDTO;
import com.pokemontcg.deck.dto.SaveDeckRequest;
import com.pokemontcg.deck.repository.DeckRepository;
import com.pokemontcg.deck.validator.DeckCardEntry;
import com.pokemontcg.deck.validator.DeckCardMetadata;
import com.pokemontcg.deck.validator.DeckValidationResult;
import com.pokemontcg.deck.validator.DeckValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckValidator deckValidator = new DeckValidator();

    public DeckService(DeckRepository deckRepository, CardRepository cardRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public List<DeckSummaryDTO> listMyDecks(UUID userId) {
        return deckRepository.findByOwnerIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeckSummaryDTO> listStarterDecks() {
        return deckRepository.findByStarterTrueOrderByNameAsc().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckDetailDTO getDeck(UUID deckId, UUID userId) {
        DeckEntity deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        if (!deck.isStarter() && !userId.equals(deck.getOwnerId())) {
            throw new ForbiddenException("You do not have permission to access this deck");
        }
        return toDetailDto(deck);
    }

    @Transactional
    public DeckDetailDTO createDeck(UUID userId, SaveDeckRequest request) {
        Instant now = Instant.now();
        DeckEntity deck = new DeckEntity();
        deck.setId(UUID.randomUUID());
        deck.setOwnerId(userId);
        deck.setStarter(false);
        deck.setCpuEligible(false);
        deck.setCreatedAt(now);

        applyDeckUpdate(deck, request, now);
        DeckEntity saved = deckRepository.save(deck);
        return toDetailDto(saved);
    }

    @Transactional
    public DeckDetailDTO updateDeck(UUID deckId, UUID userId, SaveDeckRequest request) {
        DeckEntity deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        if (deck.isStarter() || !userId.equals(deck.getOwnerId())) {
            throw new ForbiddenException("You do not have permission to modify this deck");
        }

        applyDeckUpdate(deck, request, Instant.now());
        DeckEntity saved = deckRepository.save(deck);
        return toDetailDto(saved);
    }

    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        DeckEntity deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        if (deck.isStarter() || !userId.equals(deck.getOwnerId())) {
            throw new ForbiddenException("You do not have permission to delete this deck");
        }
        deckRepository.delete(deck);
    }

    private void applyDeckUpdate(DeckEntity deck, SaveDeckRequest request, Instant now) {
        deck.setName(request.name().trim());
        deck.setDescription(request.description() != null ? request.description().trim() : null);
        deck.setUpdatedAt(now);

        List<DeckCardEntry> rawEntries = request.cards().stream()
                .map(c -> new DeckCardEntry(c.cardId(), c.quantity()))
                .toList();
        List<DeckCardEntry> consolidated = DeckValidator.consolidateEntries(rawEntries);

        List<String> requestedIds = consolidated.stream().map(DeckCardEntry::cardId).toList();
        Map<String, CardEntity> cardsById = cardRepository.findAllById(requestedIds).stream()
                .collect(Collectors.toMap(CardEntity::getId, Function.identity()));

        for (String cardId : requestedIds) {
            if (!cardsById.containsKey(cardId)) {
                throw new IllegalArgumentException("Unknown card ID: " + cardId);
            }
        }

        Map<String, DeckCardMetadata> metadataById = toMetadataMap(cardsById.values());
        DeckValidationResult validation = deckValidator.validate(consolidated, metadataById);
        deck.setValid(validation.valid());

        Map<String, Integer> targetQuantities = consolidated.stream()
                .collect(Collectors.toMap(DeckCardEntry::cardId, DeckCardEntry::quantity));

        deck.getCards().removeIf(dc -> !targetQuantities.containsKey(dc.getCard().getId()));

        Map<String, DeckCardEntity> existingByCardId = deck.getCards().stream()
                .collect(Collectors.toMap(dc -> dc.getCard().getId(), Function.identity()));

        for (DeckCardEntry entry : consolidated) {
            DeckCardEntity existing = existingByCardId.get(entry.cardId());
            if (existing != null) {
                existing.setQuantity(entry.quantity());
            } else {
                deck.getCards().add(new DeckCardEntity(deck, cardsById.get(entry.cardId()), entry.quantity()));
            }
        }
    }

    private DeckSummaryDTO toSummaryDto(DeckEntity deck) {
        int totalCards = deck.getCards().stream().mapToInt(DeckCardEntity::getQuantity).sum();
        return new DeckSummaryDTO(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.isStarter(),
                deck.isCpuEligible(),
                deck.isValid(),
                totalCards,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }

    DeckDetailDTO toDetailDto(DeckEntity deck) {
        List<DeckCardEntity> sortedCards = deck.getCards().stream()
                .sorted(Comparator
                        .comparingInt((DeckCardEntity dc) -> dc.getCard().getSortOrder())
                        .thenComparing(dc -> dc.getCard().getId()))
                .toList();

        List<DeckCardEntry> entries = sortedCards.stream()
                .map(dc -> new DeckCardEntry(dc.getCard().getId(), dc.getQuantity()))
                .toList();
        Map<String, DeckCardMetadata> metaMap = toMetadataMap(sortedCards.stream().map(DeckCardEntity::getCard).toList());
        DeckValidationResult validation = deckValidator.validate(entries, metaMap);

        List<DeckCardItemDTO> items = sortedCards.stream()
                .map(dc -> {
                    CardEntity c = dc.getCard();
                    return new DeckCardItemDTO(
                            c.getId(),
                            c.getLocalId(),
                            c.getSortOrder(),
                            c.getName(),
                            c.getCategory(),
                            c.getStage(),
                            c.getTrainerType(),
                            c.getEnergyType(),
                            c.getImageLowUrl(),
                            List.copyOf(c.getTypes()),
                            dc.getQuantity()
                    );
                })
                .toList();

        int totalCards = sortedCards.stream().mapToInt(DeckCardEntity::getQuantity).sum();

        return new DeckDetailDTO(
                deck.getId(),
                deck.getOwnerId(),
                deck.getName(),
                deck.getDescription(),
                deck.isStarter(),
                deck.isCpuEligible(),
                deck.isValid(),
                totalCards,
                validation,
                items,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }

    private static Map<String, DeckCardMetadata> toMetadataMap(Iterable<CardEntity> cards) {
        Map<String, DeckCardMetadata> map = new java.util.LinkedHashMap<>();
        for (CardEntity card : cards) {
            map.put(card.getId(), new DeckCardMetadata(
                    card.getId(),
                    card.getName(),
                    card.getCategory(),
                    card.getStage(),
                    card.getEnergyType()
            ));
        }
        return map;
    }
}
