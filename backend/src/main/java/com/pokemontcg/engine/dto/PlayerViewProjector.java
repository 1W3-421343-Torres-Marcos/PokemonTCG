package com.pokemontcg.engine.dto;

import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.rules.EnergyCostMatcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class PlayerViewProjector {

    private PlayerViewProjector() {
    }

    public static PlayerViewDTO project(GameState state, PlayerId viewer) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(viewer, "viewer must not be null");

        PlayerId opponentId = viewer.opponent();
        PlayerState myState = state.player(viewer);
        PlayerState opponentState = state.player(opponentId);

        boolean activeImmobilized = myState.active() != null && (
                myState.active().statusConditions().contains(StatusCondition.ASLEEP)
                        || myState.active().statusConditions().contains(StatusCondition.PARALYZED)
        );

        boolean canAttackThisTurn = state.phase() == TurnPhase.MAIN_PHASE
                && state.activePlayer() == viewer
                && state.awaitingPromotionPlayer() == null
                && state.turnNumber() > 1
                && myState.active() != null
                && !activeImmobilized;

        boolean canRetreat = state.phase() == TurnPhase.MAIN_PHASE
                && state.activePlayer() == viewer
                && state.awaitingPromotionPlayer() == null
                && myState.active() != null
                && !myState.retreatedThisTurn()
                && !myState.bench().isEmpty()
                && !activeImmobilized
                && totalEnergyUnits(myState.active()) >= myState.active().activeCard().definition().retreatCost();

        OpponentViewDTO opponentView = new OpponentViewDTO(
                opponentId,
                opponentState.hand().size(),
                opponentState.deck().size(),
                opponentState.prizes().size(),
                projectPokemon(opponentState.active()),
                opponentState.bench().stream().map(PlayerViewProjector::projectPokemon).toList(),
                opponentState.discardPile().stream().map(c -> toCardView(c, List.of(), false, List.of())).toList()
        );

        List<CardViewDTO> myHandViews = myState.hand().stream()
                .map(card -> toHandCardView(state, viewer, myState, card))
                .toList();

        return new PlayerViewDTO(
                viewer,
                state.turnNumber(),
                state.activePlayer(),
                state.startingPlayer(),
                state.phase(),
                state.awaitingPromotionPlayer(),
                myState.energyAttachedThisTurn(),
                myState.retreatedThisTurn(),
                canAttackThisTurn,
                myState.setupReady(),
                opponentState.setupReady(),
                state.winner(),
                state.winReason(),
                myHandViews,
                projectPokemon(myState.active()),
                myState.bench().stream().map(PlayerViewProjector::projectPokemon).toList(),
                myState.discardPile().stream().map(c -> toCardView(c, List.of(), false, List.of())).toList(),
                myState.deck().size(),
                myState.prizes().size(),
                opponentView,
                canRetreat
        );
    }

    private static PokemonViewDTO projectPokemon(PokemonInPlay pokemon) {
        if (pokemon == null) {
            return null;
        }
        List<CardInstance> energies = pokemon.attachedEnergies();
        CardViewDTO activeCard = toCardView(pokemon.activeCard(), energies, false, List.of());
        List<StatusCondition> conditions = pokemon.statusConditions().stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        return new PokemonViewDTO(
                pokemon.instanceId(),
                activeCard,
                pokemon.maxHp(),
                pokemon.remainingHp(),
                pokemon.damage(),
                energies.stream().map(e -> toCardView(e, List.of(), false, List.of())).toList(),
                conditions,
                activeCard.attacks(),
                pokemon.turnEnteredPlay(),
                pokemon.turnLastEvolved()
        );
    }

    private static CardViewDTO toHandCardView(GameState state, PlayerId viewer, PlayerState myState, CardInstance card) {
        CardDefinition def = card.definition();
        boolean playable = false;
        List<String> validTargets = List.of();

        if (state.phase() == TurnPhase.SETUP && !myState.setupReady()) {
            playable = def.basicPokemon();
        } else if (state.phase() == TurnPhase.MAIN_PHASE
                && state.activePlayer() == viewer
                && state.awaitingPromotionPlayer() == null) {
            if (def.basicPokemon()) {
                playable = myState.bench().size() < 5;
            } else if (def.energyCard()) {
                if (!myState.energyAttachedThisTurn()) {
                    validTargets = ownPokemonInPlayIds(myState);
                    playable = !validTargets.isEmpty();
                }
            } else if (def.evolutionCard()) {
                if (state.turnNumber() > 2 && def.evolveFrom() != null && !def.evolveFrom().isBlank()) {
                    validTargets = ownPokemonInPlay(myState).stream()
                            .filter(p -> p.activeCard().definition().name().equalsIgnoreCase(def.evolveFrom()))
                            .filter(p -> p.turnEnteredPlay() < state.turnNumber() && p.turnLastEvolved() < state.turnNumber())
                            .map(PokemonInPlay::instanceId)
                            .toList();
                    playable = !validTargets.isEmpty();
                }
            }
        }

        return toCardView(card, List.of(), playable, validTargets);
    }

    private static List<PokemonInPlay> ownPokemonInPlay(PlayerState myState) {
        List<PokemonInPlay> inPlay = new ArrayList<>();
        if (myState.active() != null) {
            inPlay.add(myState.active());
        }
        inPlay.addAll(myState.bench());
        return inPlay;
    }

    private static List<String> ownPokemonInPlayIds(PlayerState myState) {
        return ownPokemonInPlay(myState).stream().map(PokemonInPlay::instanceId).toList();
    }

    private static int totalEnergyUnits(PokemonInPlay pokemon) {
        int total = 0;
        for (CardInstance energy : pokemon.attachedEnergies()) {
            int units = energy.definition().providedEnergyCount();
            total += units > 0 ? units : 1;
        }
        return total;
    }

    private static CardViewDTO toCardView(
            CardInstance card,
            List<CardInstance> attachedEnergies,
            boolean playable,
            List<String> validTargetIds
    ) {
        CardDefinition def = card.definition();
        List<AttackViewDTO> attackViews = new ArrayList<>(def.attacks().size());
        for (int i = 0; i < def.attacks().size(); i++) {
            AttackDefinition attack = def.attacks().get(i);
            boolean canPay = EnergyCostMatcher.canPay(attachedEnergies, attack.cost());
            attackViews.add(new AttackViewDTO(
                    i,
                    attack.name(),
                    attack.cost(),
                    attack.baseDamage(),
                    attack.rawDamage(),
                    attack.text(),
                    canPay
            ));
        }

        return new CardViewDTO(
                card.instanceId(),
                def.id(),
                def.name(),
                def.category(),
                def.stage(),
                def.evolveFrom(),
                def.hp(),
                def.types(),
                attackViews,
                def.weaknesses(),
                def.resistances(),
                def.retreatCost(),
                def.trainerType(),
                def.energyType(),
                def.providedEnergyType(),
                def.providedEnergyCount(),
                def.imageLowUrl(),
                def.imageHighUrl(),
                playable,
                validTargetIds
        );
    }
}
