package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.domain.CardEntity;
import com.pokemontcg.catalog.domain.CardSetEntity;
import com.pokemontcg.catalog.dto.SyncResultDTO;
import com.pokemontcg.catalog.repository.CardRepository;
import com.pokemontcg.catalog.repository.CardSetRepository;
import com.pokemontcg.catalog.service.CatalogSyncMapper.PreparedCardData;
import com.pokemontcg.catalog.service.CatalogSyncMapper.PreparedSetData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogPersistenceService {

    private final CardSetRepository cardSetRepository;
    private final CardRepository cardRepository;

    public CatalogPersistenceService(CardSetRepository cardSetRepository, CardRepository cardRepository) {
        this.cardSetRepository = cardSetRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public SyncResultDTO upsertCatalogBatch(PreparedSetData setData, List<PreparedCardData> cardsData, String lang) {
        CardSetEntity setEntity = cardSetRepository.findById(setData.id())
                .orElseGet(CardSetEntity::new);
        setEntity.setId(setData.id());
        setEntity.setName(setData.name());
        setEntity.setSeries(setData.series());
        setEntity.setReleaseDate(setData.releaseDate());
        setEntity.setTotalCards(setData.totalCards());
        setEntity.setLogoUrl(setData.logoUrl());
        setEntity.setSyncedAt(setData.syncedAt());
        CardSetEntity savedSet = cardSetRepository.save(setEntity);

        List<String> cardIds = cardsData.stream().map(PreparedCardData::id).toList();
        Map<String, CardEntity> existingById = cardRepository.findAllById(cardIds).stream()
                .collect(Collectors.toMap(CardEntity::getId, Function.identity()));

        List<CardEntity> toSave = new ArrayList<>(cardsData.size());
        for (PreparedCardData data : cardsData) {
            CardEntity entity = existingById.getOrDefault(data.id(), new CardEntity());
            entity.setId(data.id());
            entity.setSet(savedSet);
            entity.setLocalId(data.localId());
            entity.setSortOrder(data.sortOrder());
            entity.setName(data.name());
            entity.setCategory(data.category());
            entity.setHp(data.hp());
            entity.setStage(data.stage());
            entity.setEvolveFrom(data.evolveFrom());
            entity.setRetreat(data.retreat());
            entity.setTrainerType(data.trainerType());
            entity.setEnergyType(data.energyType());
            entity.setRarity(data.rarity());
            entity.setIllustrator(data.illustrator());
            entity.setImageLowUrl(data.imageLowUrl());
            entity.setImageHighUrl(data.imageHighUrl());
            entity.setTypes(data.types());
            entity.setAttacks(data.attacks());
            entity.setWeaknesses(data.weaknesses());
            entity.setResistances(data.resistances());
            entity.setEffectText(data.effectText());
            entity.setRawPayload(data.rawPayload());
            toSave.add(entity);
        }

        cardRepository.saveAll(toSave);
        return new SyncResultDTO(savedSet.getId(), lang, toSave.size(), setData.syncedAt());
    }
}
