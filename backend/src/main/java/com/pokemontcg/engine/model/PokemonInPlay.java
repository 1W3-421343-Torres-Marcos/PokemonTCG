package com.pokemontcg.engine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PokemonInPlay(
        String instanceId,
        CardInstance activeCard,
        List<CardInstance> evolutionChain,
        List<CardInstance> attachedEnergies,
        int damage,
        Set<StatusCondition> statusConditions,
        int turnEnteredPlay,
        int turnLastEvolved,
        List<ActiveModifier> modifiers
) {
    public PokemonInPlay {
        instanceId = Objects.requireNonNull(instanceId, "pokemon instanceId must not be null");
        activeCard = Objects.requireNonNull(activeCard, "activeCard must not be null");
        evolutionChain = (evolutionChain == null || evolutionChain.isEmpty())
                ? List.of(activeCard)
                : List.copyOf(evolutionChain);
        attachedEnergies = attachedEnergies == null ? List.of() : List.copyOf(attachedEnergies);
        damage = Math.max(0, damage);
        statusConditions = statusConditions == null ? Set.of() : Set.copyOf(statusConditions);
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    }

    public PokemonInPlay(
            String instanceId,
            CardInstance activeCard,
            List<CardInstance> evolutionChain,
            List<CardInstance> attachedEnergies,
            int damage,
            Set<StatusCondition> statusConditions,
            int turnEnteredPlay,
            int turnLastEvolved
    ) {
        this(instanceId, activeCard, evolutionChain, attachedEnergies, damage, statusConditions, turnEnteredPlay, turnLastEvolved, List.of());
    }

    public static PokemonInPlay fromBasic(CardInstance basicCard, int turnNumber) {
        return new PokemonInPlay(
                basicCard.instanceId(),
                basicCard,
                List.of(basicCard),
                List.of(),
                0,
                Set.of(),
                turnNumber,
                turnNumber,
                List.of()
        );
    }

    public int maxHp() {
        Integer hp = activeCard.definition().hp();
        return hp == null ? 0 : Math.max(0, hp);
    }

    public int remainingHp() {
        return Math.max(0, maxHp() - damage);
    }

    public boolean knockedOut() {
        return maxHp() > 0 && damage >= maxHp();
    }

    public PokemonInPlay withDamage(int nextDamage) {
        return new PokemonInPlay(
                instanceId, activeCard, evolutionChain, attachedEnergies,
                nextDamage, statusConditions, turnEnteredPlay, turnLastEvolved, modifiers
        );
    }

    public PokemonInPlay withAttachedEnergies(List<CardInstance> nextEnergies) {
        return new PokemonInPlay(
                instanceId, activeCard, evolutionChain, nextEnergies,
                damage, statusConditions, turnEnteredPlay, turnLastEvolved, modifiers
        );
    }

    public PokemonInPlay withStatusConditions(Set<StatusCondition> nextStatuses) {
        return new PokemonInPlay(
                instanceId, activeCard, evolutionChain, attachedEnergies,
                damage, nextStatuses, turnEnteredPlay, turnLastEvolved, modifiers
        );
    }

    public PokemonInPlay withModifiers(List<ActiveModifier> nextModifiers) {
        return new PokemonInPlay(
                instanceId, activeCard, evolutionChain, attachedEnergies,
                damage, statusConditions, turnEnteredPlay, turnLastEvolved, nextModifiers
        );
    }

    public PokemonInPlay withClearedCombatState() {
        return new PokemonInPlay(
                instanceId, activeCard, evolutionChain, attachedEnergies,
                damage, Set.of(), turnEnteredPlay, turnLastEvolved, List.of()
        );
    }

    public List<CardInstance> allCards() {
        List<CardInstance> all = new ArrayList<>(evolutionChain.size() + attachedEnergies.size());
        all.addAll(evolutionChain);
        boolean activeInChain = evolutionChain.stream()
                .anyMatch(card -> card.instanceId().equals(activeCard.instanceId()));
        if (!activeInChain) {
            all.add(activeCard);
        }
        all.addAll(attachedEnergies);
        return List.copyOf(all);
    }
}
