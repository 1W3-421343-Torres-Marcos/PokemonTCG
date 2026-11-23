package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.random.RandomProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Mutable execution context passed to Trainer and Attack effects within a single engine step.
 */
public final class EffectContext {

    @FunctionalInterface
    public interface EventSink {
        void emit(GameEventType type, PlayerId actor, String message, Map<String, Object> metadata);
    }

    private GameState state;
    private final PlayerId actor;
    private final RandomProvider randomProvider;
    private final EventSink eventSink;

    public EffectContext(
            GameState state,
            PlayerId actor,
            RandomProvider randomProvider,
            EventSink eventSink
    ) {
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
        this.randomProvider = Objects.requireNonNull(randomProvider, "randomProvider must not be null");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink must not be null");
    }

    public GameState state() {
        return state;
    }

    public void updateState(GameState nextState) {
        this.state = Objects.requireNonNull(nextState, "nextState must not be null");
    }

    public PlayerId actor() {
        return actor;
    }

    public int turnNumber() {
        return state.turnNumber();
    }

    public void emit(GameEventType type, PlayerId owner, String message, Map<String, Object> metadata) {
        eventSink.emit(type, owner, message, metadata == null ? Map.of() : metadata);
    }

    public boolean flipCoin(PlayerId owner, String reason, String descriptionPrefix) {
        boolean heads = randomProvider.flipCoin();
        emit(
                GameEventType.COIN_FLIPPED,
                owner,
                descriptionPrefix + (heads ? "CARA." : "CRUZ."),
                Map.of("reason", reason, "result", heads ? "HEADS" : "TAILS", "heads", heads)
        );
        return heads;
    }

    public void updatePlayer(PlayerId owner, UnaryOperator<PlayerState> mutator) {
        PlayerState current = state.player(owner);
        PlayerState updated = Objects.requireNonNull(mutator.apply(current), "updated PlayerState must not be null");
        state = state.withPlayer(owner, updated);
    }

    public Optional<PokemonInPlay> findPokemon(PlayerId owner, String instanceId) {
        if (instanceId == null) {
            return Optional.empty();
        }
        PlayerState ps = state.player(owner);
        if (ps.active() != null && ps.active().instanceId().equals(instanceId)) {
            return Optional.of(ps.active());
        }
        for (PokemonInPlay benched : ps.bench()) {
            if (benched.instanceId().equals(instanceId)) {
                return Optional.of(benched);
            }
        }
        return Optional.empty();
    }

    public void replacePokemon(PlayerId owner, PokemonInPlay updated) {
        updatePlayer(owner, ps -> {
            if (ps.active() != null && ps.active().instanceId().equals(updated.instanceId())) {
                return withActive(ps, updated);
            }
            List<PokemonInPlay> nextBench = new ArrayList<>(ps.bench());
            for (int i = 0; i < nextBench.size(); i++) {
                if (nextBench.get(i).instanceId().equals(updated.instanceId())) {
                    nextBench.set(i, updated);
                    return withBench(ps, nextBench);
                }
            }
            throw new IllegalStateException("Pokemon not in play: " + updated.instanceId());
        });
    }

    public String displayPlayer(PlayerId playerId) {
        return playerId == PlayerId.PLAYER ? "Jugador" : "CPU";
    }

    public static List<PokemonInPlay> allPokemonInPlay(PlayerState ps) {
        List<PokemonInPlay> list = new ArrayList<>();
        if (ps.active() != null) {
            list.add(ps.active());
        }
        list.addAll(ps.bench());
        return list;
    }

    public static PlayerState withActive(PlayerState ps, PokemonInPlay active) {
        return new PlayerState(
                ps.deck(), ps.hand(), active, ps.bench(), ps.prizes(), ps.discardPile(),
                ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(),
                ps.mulliganCount(), ps.plusPowerBonus()
        );
    }

    public static PlayerState withBench(PlayerState ps, List<PokemonInPlay> bench) {
        return new PlayerState(
                ps.deck(), ps.hand(), ps.active(), bench, ps.prizes(), ps.discardPile(),
                ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(),
                ps.mulliganCount(), ps.plusPowerBonus()
        );
    }
}
