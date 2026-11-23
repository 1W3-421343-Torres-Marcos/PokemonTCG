package com.pokemontcg.deck.service;

import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.repository.CardRepository;
import com.pokemontcg.deck.domain.DeckCardEntity;
import com.pokemontcg.deck.domain.DeckEntity;
import com.pokemontcg.deck.repository.DeckRepository;
import com.pokemontcg.deck.validator.DeckCardEntry;
import com.pokemontcg.deck.validator.DeckCardMetadata;
import com.pokemontcg.deck.validator.DeckValidationResult;
import com.pokemontcg.deck.validator.DeckValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(20)
public class StarterDeckSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StarterDeckSeeder.class);

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckValidator deckValidator = new DeckValidator();

    public StarterDeckSeeder(DeckRepository deckRepository, CardRepository cardRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedStarterDecks();
    }

    @Transactional
    public int seedStarterDecks() {
        List<StarterSpec> specs = List.of(
                new StarterSpec(
                        "Brushfire (Fuego / Planta)",
                        "Mazo prearmado equilibrado de Base Set con línea Charizard, Arcanine y apoyo Planta.",
                        List.of(
                                new DeckCardEntry("base1-46", 4),
                                new DeckCardEntry("base1-24", 2),
                                new DeckCardEntry("base1-4", 1),
                                new DeckCardEntry("base1-28", 2),
                                new DeckCardEntry("base1-23", 1),
                                new DeckCardEntry("base1-36", 2),
                                new DeckCardEntry("base1-44", 3),
                                new DeckCardEntry("base1-30", 2),
                                new DeckCardEntry("base1-66", 2),
                                new DeckCardEntry("base1-88", 2),
                                new DeckCardEntry("base1-91", 4),
                                new DeckCardEntry("base1-94", 3),
                                new DeckCardEntry("base1-95", 2),
                                new DeckCardEntry("base1-81", 2),
                                new DeckCardEntry("base1-93", 2),
                                new DeckCardEntry("base1-96", 2),
                                new DeckCardEntry("base1-98", 16),
                                new DeckCardEntry("base1-99", 8)
                        )
                ),
                new StarterSpec(
                        "Zap! & Torrent (Agua / Rayo)",
                        "Mazo rápido de Base Set con Blastoise, Pikachu/Raichu, Electabuzz, Zapdos y control de energía.",
                        List.of(
                                new DeckCardEntry("base1-63", 4),
                                new DeckCardEntry("base1-42", 2),
                                new DeckCardEntry("base1-2", 1),
                                new DeckCardEntry("base1-65", 3),
                                new DeckCardEntry("base1-64", 2),
                                new DeckCardEntry("base1-58", 3),
                                new DeckCardEntry("base1-14", 1),
                                new DeckCardEntry("base1-20", 2),
                                new DeckCardEntry("base1-16", 1),
                                new DeckCardEntry("base1-91", 4),
                                new DeckCardEntry("base1-88", 2),
                                new DeckCardEntry("base1-90", 2),
                                new DeckCardEntry("base1-95", 2),
                                new DeckCardEntry("base1-92", 3),
                                new DeckCardEntry("base1-84", 2),
                                new DeckCardEntry("base1-102", 15),
                                new DeckCardEntry("base1-100", 11)
                        )
                ),
                new StarterSpec(
                        "Blackout (Lucha / Psíquico)",
                        "Mazo resistente de Base Set con Machamp, Hitmonchan, Alakazam, Mewtwo y Chansey.",
                        List.of(
                                new DeckCardEntry("base1-52", 4),
                                new DeckCardEntry("base1-34", 2),
                                new DeckCardEntry("base1-8", 1),
                                new DeckCardEntry("base1-7", 2),
                                new DeckCardEntry("base1-56", 2),
                                new DeckCardEntry("base1-43", 3),
                                new DeckCardEntry("base1-32", 2),
                                new DeckCardEntry("base1-1", 1),
                                new DeckCardEntry("base1-10", 1),
                                new DeckCardEntry("base1-3", 1),
                                new DeckCardEntry("base1-91", 4),
                                new DeckCardEntry("base1-88", 2),
                                new DeckCardEntry("base1-80", 2),
                                new DeckCardEntry("base1-93", 2),
                                new DeckCardEntry("base1-94", 3),
                                new DeckCardEntry("base1-92", 2),
                                new DeckCardEntry("base1-96", 2),
                                new DeckCardEntry("base1-97", 14),
                                new DeckCardEntry("base1-101", 10)
                        )
                )
        );

        int created = 0;
        for (StarterSpec spec : specs) {
            if (deckRepository.existsByStarterTrueAndNameIgnoreCase(spec.name())) {
                continue;
            }
            List<String> cardIds = spec.entries().stream().map(DeckCardEntry::cardId).toList();
            Map<String, CardEntity> cardsById = cardRepository.findAllById(cardIds).stream()
                    .collect(Collectors.toMap(CardEntity::getId, Function.identity()));

            if (cardsById.size() != cardIds.size()) {
                log.warn("Skipping starter deck '{}' because catalog cards are missing.", spec.name());
                continue;
            }

            Map<String, DeckCardMetadata> metaMap = new LinkedHashMap<>();
            for (CardEntity card : cardsById.values()) {
                metaMap.put(card.getId(), new DeckCardMetadata(
                        card.getId(), card.getName(), card.getCategory(), card.getStage(), card.getEnergyType()
                ));
            }
            DeckValidationResult validation = deckValidator.validate(spec.entries(), metaMap);
            if (!validation.valid()) {
                throw new IllegalStateException("Starter deck '" + spec.name() + "' failed validation: " + validation.errors());
            }

            Instant now = Instant.now();
            DeckEntity deck = new DeckEntity();
            deck.setId(UUID.randomUUID());
            deck.setOwnerId(null);
            deck.setName(spec.name());
            deck.setDescription(spec.description());
            deck.setStarter(true);
            deck.setCpuEligible(true);
            deck.setValid(true);
            deck.setCreatedAt(now);
            deck.setUpdatedAt(now);

            for (DeckCardEntry entry : spec.entries()) {
                deck.getCards().add(new DeckCardEntity(deck, cardsById.get(entry.cardId()), entry.quantity()));
            }
            deckRepository.save(deck);
            created++;
        }

        if (created > 0) {
            log.info("Seeded {} starter decks.", created);
        }
        return created;
    }

    private record StarterSpec(String name, String description, List<DeckCardEntry> entries) {
    }
}
