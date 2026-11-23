package com.pokemontcg.catalog.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "cards")
public class CardEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private CardSetEntity set;

    @Column(name = "local_id", nullable = false, length = 32)
    private String localId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 32)
    private String category;

    private Integer hp;

    @Column(length = 32)
    private String stage;

    @Column(name = "evolve_from", length = 150)
    private String evolveFrom;

    private Integer retreat;

    @Column(name = "trainer_type", length = 64)
    private String trainerType;

    @Column(name = "energy_type", length = 32)
    private String energyType;

    @Column(length = 64)
    private String rarity;

    @Column(length = 150)
    private String illustrator;

    @Column(name = "image_low_url")
    private String imageLowUrl;

    @Column(name = "image_high_url")
    private String imageHighUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_types", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "type", nullable = false, length = 32)
    @BatchSize(size = 50)
    private Set<String> types = new LinkedHashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<CardAttack> attacks = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<CardTypeModifier> weaknesses = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<CardTypeModifier> resistances = new ArrayList<>();

    @Column(name = "effect_text")
    private String effectText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rawPayload = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CardSetEntity getSet() {
        return set;
    }

    public void setSet(CardSetEntity set) {
        this.set = set;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getHp() {
        return hp;
    }

    public void setHp(Integer hp) {
        this.hp = hp;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getEvolveFrom() {
        return evolveFrom;
    }

    public void setEvolveFrom(String evolveFrom) {
        this.evolveFrom = evolveFrom;
    }

    public Integer getRetreat() {
        return retreat;
    }

    public void setRetreat(Integer retreat) {
        this.retreat = retreat;
    }

    public String getTrainerType() {
        return trainerType;
    }

    public void setTrainerType(String trainerType) {
        this.trainerType = trainerType;
    }

    public String getEnergyType() {
        return energyType;
    }

    public void setEnergyType(String energyType) {
        this.energyType = energyType;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getIllustrator() {
        return illustrator;
    }

    public void setIllustrator(String illustrator) {
        this.illustrator = illustrator;
    }

    public String getImageLowUrl() {
        return imageLowUrl;
    }

    public void setImageLowUrl(String imageLowUrl) {
        this.imageLowUrl = imageLowUrl;
    }

    public String getImageHighUrl() {
        return imageHighUrl;
    }

    public void setImageHighUrl(String imageHighUrl) {
        this.imageHighUrl = imageHighUrl;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types != null ? new LinkedHashSet<>(types) : new LinkedHashSet<>();
    }

    public List<CardAttack> getAttacks() {
        return attacks;
    }

    public void setAttacks(List<CardAttack> attacks) {
        this.attacks = attacks != null ? new ArrayList<>(attacks) : new ArrayList<>();
    }

    public List<CardTypeModifier> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<CardTypeModifier> weaknesses) {
        this.weaknesses = weaknesses != null ? new ArrayList<>(weaknesses) : new ArrayList<>();
    }

    public List<CardTypeModifier> getResistances() {
        return resistances;
    }

    public void setResistances(List<CardTypeModifier> resistances) {
        this.resistances = resistances != null ? new ArrayList<>(resistances) : new ArrayList<>();
    }

    public String getEffectText() {
        return effectText;
    }

    public void setEffectText(String effectText) {
        this.effectText = effectText;
    }

    public Map<String, Object> getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(Map<String, Object> rawPayload) {
        this.rawPayload = rawPayload != null ? new LinkedHashMap<>(rawPayload) : new LinkedHashMap<>();
    }
}
