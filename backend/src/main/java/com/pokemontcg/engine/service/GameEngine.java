package com.pokemontcg.engine.service;

import com.pokemontcg.engine.action.AttachEnergyAction;
import com.pokemontcg.engine.action.AttackAction;
import com.pokemontcg.engine.action.ConcedeAction;
import com.pokemontcg.engine.action.EndTurnAction;
import com.pokemontcg.engine.action.EvolveAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.action.PlayBasicAction;
import com.pokemontcg.engine.action.PlayTrainerAction;
import com.pokemontcg.engine.action.PromoteActiveAction;
import com.pokemontcg.engine.action.RetreatAction;
import com.pokemontcg.engine.action.SelectSetupAction;
import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.ActiveModifier;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;
import com.pokemontcg.engine.random.DefaultRandomProvider;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.rules.DamageCalculator;
import com.pokemontcg.engine.rules.EnergyCostMatcher;
import com.pokemontcg.engine.rules.StatusRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GameEngine {

    public static final int DECK_SIZE = 60;
    public static final int INITIAL_HAND_SIZE = 7;
    public static final int PRIZE_COUNT = 6;
    public static final int MAX_BENCH_SIZE = 5;
    private static final int MAX_MULLIGANS = 50;

    private final RandomProvider randomProvider;

    public GameEngine() {
        this(new DefaultRandomProvider());
    }

    public GameEngine(RandomProvider randomProvider) {
        this.randomProvider = Objects.requireNonNull(randomProvider, "randomProvider must not be null");
    }

    public EngineStepResult initializeMatch(
            List<CardDefinition> playerDeck,
            List<CardDefinition> cpuDeck,
            RandomProvider random
    ) {
        Objects.requireNonNull(random, "random provider must not be null");
        PlayerId startingPlayer = random.flipCoin() ? PlayerId.PLAYER : PlayerId.CPU;
        return initializeMatch(playerDeck, cpuDeck, startingPlayer, random);
    }

    public EngineStepResult initializeMatch(
            List<CardDefinition> playerDeck,
            List<CardDefinition> cpuDeck,
            PlayerId startingPlayer,
            RandomProvider random
    ) {
        Objects.requireNonNull(startingPlayer, "startingPlayer must not be null");
        Objects.requireNonNull(random, "random provider must not be null");
        validateDeck(playerDeck, "Jugador");
        validateDeck(cpuDeck, "CPU");

        List<CardInstance> playerInstances = instantiateDeck("p", playerDeck);
        List<CardInstance> cpuInstances = instantiateDeck("c", cpuDeck);

        EventCollector events = new EventCollector(1L);
        PlayerState playerState = drawOpeningHand(PlayerId.PLAYER, playerInstances, random, events);
        PlayerState cpuState = drawOpeningHand(PlayerId.CPU, cpuInstances, random, events);

        events.emit(
                GameEventType.GAME_STARTED,
                startingPlayer,
                "Partida iniciada. Comienza el turno 1: " + displayPlayer(startingPlayer) + ".",
                Map.of(
                        "startingPlayer", startingPlayer.name(),
                        "playerMulligans", playerState.mulliganCount(),
                        "cpuMulligans", cpuState.mulliganCount()
                )
        );

        EnumMap<PlayerId, PlayerState> players = new EnumMap<>(PlayerId.class);
        players.put(PlayerId.PLAYER, playerState);
        players.put(PlayerId.CPU, cpuState);

        GameState initialState = new GameState(
                0,
                startingPlayer,
                startingPlayer,
                TurnPhase.SETUP,
                null,
                players,
                null,
                null,
                events.nextSequence()
        );

        return new EngineStepResult(initialState, events.events());
    }

    public EngineStepResult applyAction(GameState state, PlayerId actor, GameAction action) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(action, "action must not be null");

        if (state.phase() == TurnPhase.GAME_OVER) {
            throw new IllegalGameActionException("La partida ya ha finalizado");
        }

        EventCollector events = new EventCollector(state.nextEventSequence());

        if (action instanceof ConcedeAction) {
            return handleConcede(state, actor, events);
        }

        if (state.awaitingPromotionPlayer() != null) {
            if (action instanceof PromoteActiveAction promote) {
                return handlePromoteActive(state, actor, promote, events);
            }
            throw new IllegalGameActionException(
                    "Se requiere que " + displayPlayer(state.awaitingPromotionPlayer()) + " promueva un Pokémon de la Banca"
            );
        }

        return switch (action) {
            case SelectSetupAction setup -> handleSelectSetup(state, actor, setup, events);
            case PlayBasicAction playBasic -> handlePlayBasic(state, actor, playBasic, events);
            case AttachEnergyAction attachEnergy -> handleAttachEnergy(state, actor, attachEnergy, events);
            case AttackAction attack -> handleAttack(state, actor, attack, events);
            case EndTurnAction ignored -> handleEndTurn(state, actor, events);
            case PromoteActiveAction ignored ->
                    throw new IllegalGameActionException("No hay ninguna promoción pendiente en este momento");
            case EvolveAction evolve -> handleEvolve(state, actor, evolve, events);
            case PlayTrainerAction ignored ->
                    throw new IllegalGameActionException("Acción de Entrenador disponible en F4.3");
            case RetreatAction retreat -> handleRetreat(state, actor, retreat, events);
            case ConcedeAction ignored -> throw new IllegalStateException("Handled earlier");
        };
    }

    private EngineStepResult handleConcede(GameState state, PlayerId actor, EventCollector events) {
        PlayerId winner = actor.opponent();
        events.emit(
                GameEventType.GAME_ENDED,
                winner,
                displayPlayer(actor) + " se rindió. Victoria para " + displayPlayer(winner) + ".",
                Map.of("winner", winner.name(), "reason", WinReason.CONCEDED.name())
        );

        GameState nextState = new GameState(
                state.turnNumber(),
                state.activePlayer(),
                state.startingPlayer(),
                TurnPhase.GAME_OVER,
                null,
                state.players(),
                winner,
                WinReason.CONCEDED,
                events.nextSequence()
        );
        return new EngineStepResult(nextState, events.events());
    }

    private EngineStepResult handleSelectSetup(
            GameState state,
            PlayerId actor,
            SelectSetupAction setup,
            EventCollector events
    ) {
        if (state.phase() != TurnPhase.SETUP) {
            throw new IllegalGameActionException("La fase de preparación inicial ya finalizó");
        }
        PlayerState actorState = state.player(actor);
        if (actorState.setupReady()) {
            throw new IllegalGameActionException("El setup de " + displayPlayer(actor) + " ya fue confirmado");
        }
        if (setup.activeInstanceId() == null || setup.activeInstanceId().isBlank()) {
            throw new IllegalGameActionException("Debes elegir un Pokémon Básico para el puesto Activo");
        }

        List<String> benchIds = setup.benchInstanceIds();
        if (benchIds.size() > MAX_BENCH_SIZE) {
            throw new IllegalGameActionException("No puedes colocar más de 5 Pokémon en Banca");
        }

        Set<String> chosenIds = new HashSet<>();
        chosenIds.add(setup.activeInstanceId());
        for (String benchId : benchIds) {
            if (!chosenIds.add(benchId)) {
                throw new IllegalGameActionException("Cartas duplicadas en la selección inicial");
            }
        }

        List<CardInstance> handCopy = new ArrayList<>(actorState.hand());
        CardInstance activeCard = removeHandCard(handCopy, setup.activeInstanceId(), "La carta activa elegida no está en tu mano");
        if (!activeCard.definition().basicPokemon()) {
            throw new IllegalGameActionException("El Pokémon Activo inicial debe ser un Pokémon Básico");
        }

        List<PokemonInPlay> bench = new ArrayList<>(benchIds.size());
        for (String benchId : benchIds) {
            CardInstance benchCard = removeHandCard(handCopy, benchId, "La carta de banca elegida no está en tu mano");
            if (!benchCard.definition().basicPokemon()) {
                throw new IllegalGameActionException("Solo puedes colocar Pokémon Básicos en Banca en el setup");
            }
            bench.add(PokemonInPlay.fromBasic(benchCard, 0));
        }

        List<CardInstance> deckCopy = new ArrayList<>(actorState.deck());
        int prizeCount = Math.min(PRIZE_COUNT, deckCopy.size());
        List<CardInstance> prizes = new ArrayList<>(deckCopy.subList(0, prizeCount));
        List<CardInstance> remainingDeck = new ArrayList<>(deckCopy.subList(prizeCount, deckCopy.size()));

        PlayerState readyState = new PlayerState(
                remainingDeck,
                handCopy,
                PokemonInPlay.fromBasic(activeCard, 0),
                bench,
                prizes,
                actorState.discardPile(),
                false,
                false,
                true,
                actorState.mulliganCount()
        );

        events.emit(
                GameEventType.SETUP_COMPLETED,
                actor,
                displayPlayer(actor) + " colocó a " + activeCard.definition().name()
                        + " como Activo" + (bench.isEmpty() ? "." : " y " + bench.size() + " en Banca."),
                Map.of("activeName", activeCard.definition().name(), "benchCount", bench.size())
        );

        GameState updated = state.withPlayer(actor, readyState);
        if (!updated.player(PlayerId.PLAYER).setupReady() || !updated.player(PlayerId.CPU).setupReady()) {
            GameState waitingState = withSequence(updated, events.nextSequence());
            return new EngineStepResult(waitingState, events.events());
        }

        updated = applyNetMulliganDraws(updated, events);
        GameState turn1State = startTurn(updated, updated.startingPlayer(), 1, events);
        return new EngineStepResult(turn1State, events.events());
    }

    private EngineStepResult handlePlayBasic(
            GameState state,
            PlayerId actor,
            PlayBasicAction action,
            EventCollector events
    ) {
        requireMainPhaseAndTurn(state, actor);
        PlayerState actorState = state.player(actor);
        if (actorState.bench().size() >= MAX_BENCH_SIZE) {
            throw new IllegalGameActionException("La Banca ya alcanzó el máximo de 5 Pokémon");
        }
        if (action.handInstanceId() == null || action.handInstanceId().isBlank()) {
            throw new IllegalGameActionException("Debes indicar qué Pokémon Básico quieres bajar a Banca");
        }

        List<CardInstance> nextHand = new ArrayList<>(actorState.hand());
        CardInstance chosen = removeHandCard(nextHand, action.handInstanceId(), "La carta no se encuentra en tu mano");
        if (!chosen.definition().basicPokemon()) {
            throw new IllegalGameActionException("Solo los Pokémon Básicos pueden bajarse directamente a Banca");
        }

        List<PokemonInPlay> nextBench = new ArrayList<>(actorState.bench());
        nextBench.add(PokemonInPlay.fromBasic(chosen, state.turnNumber()));

        PlayerState updatedPlayer = new PlayerState(
                actorState.deck(),
                nextHand,
                actorState.active(),
                nextBench,
                actorState.prizes(),
                actorState.discardPile(),
                actorState.energyAttachedThisTurn(),
                actorState.retreatedThisTurn(),
                actorState.setupReady(),
                actorState.mulliganCount(),
                actorState.plusPowerBonus()
        );

        events.emit(
                GameEventType.POKEMON_BENCHED,
                actor,
                displayPlayer(actor) + " bajó a " + chosen.definition().name() + " a la Banca.",
                Map.of("cardName", chosen.definition().name(), "cardId", chosen.definition().id())
        );

        GameState nextState = withSequence(state.withPlayer(actor, updatedPlayer), events.nextSequence());
        return new EngineStepResult(nextState, events.events());
    }

    private EngineStepResult handleAttachEnergy(
            GameState state,
            PlayerId actor,
            AttachEnergyAction action,
            EventCollector events
    ) {
        requireMainPhaseAndTurn(state, actor);
        PlayerState actorState = state.player(actor);
        if (actorState.energyAttachedThisTurn()) {
            throw new IllegalGameActionException("Solo puedes unir una carta de Energía por turno");
        }

        List<CardInstance> nextHand = new ArrayList<>(actorState.hand());
        CardInstance energyCard = removeHandCard(nextHand, action.energyInstanceId(), "La energía seleccionada no está en tu mano");
        if (!energyCard.definition().energyCard()) {
            throw new IllegalGameActionException("La carta seleccionada no es una Energía");
        }

        PokemonInPlay nextActive = actorState.active();
        List<PokemonInPlay> nextBench = new ArrayList<>(actorState.bench());
        String targetName = null;

        if (nextActive != null && nextActive.instanceId().equals(action.targetPokemonInstanceId())) {
            nextActive = attachEnergyToPokemon(nextActive, energyCard);
            targetName = nextActive.activeCard().definition().name();
        } else {
            boolean found = false;
            for (int i = 0; i < nextBench.size(); i++) {
                PokemonInPlay benchMon = nextBench.get(i);
                if (benchMon.instanceId().equals(action.targetPokemonInstanceId())) {
                    PokemonInPlay updatedBenchMon = attachEnergyToPokemon(benchMon, energyCard);
                    nextBench.set(i, updatedBenchMon);
                    targetName = updatedBenchMon.activeCard().definition().name();
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalGameActionException("El Pokémon objetivo no está en juego");
            }
        }

        PlayerState updatedPlayer = new PlayerState(
                actorState.deck(),
                nextHand,
                nextActive,
                nextBench,
                actorState.prizes(),
                actorState.discardPile(),
                true,
                actorState.retreatedThisTurn(),
                actorState.setupReady(),
                actorState.mulliganCount(),
                actorState.plusPowerBonus()
        );

        events.emit(
                GameEventType.ENERGY_ATTACHED,
                actor,
                displayPlayer(actor) + " unió " + energyCard.definition().name() + " a " + targetName + ".",
                Map.of("energyName", energyCard.definition().name(), "targetName", targetName)
        );

        GameState nextState = withSequence(state.withPlayer(actor, updatedPlayer), events.nextSequence());
        return new EngineStepResult(nextState, events.events());
    }

    private EngineStepResult handleAttack(
            GameState state,
            PlayerId actor,
            AttackAction action,
            EventCollector events
    ) {
        requireMainPhaseAndTurn(state, actor);
        if (state.turnNumber() <= 1) {
            throw new IllegalGameActionException("El jugador que comienza no puede atacar en el Turno 1");
        }

        PlayerState attackerState = state.player(actor);
        PokemonInPlay attacker = attackerState.active();
        if (attacker == null) {
            throw new IllegalGameActionException("No tienes ningún Pokémon Activo para atacar");
        }
        if (StatusRules.blocksAttackOrRetreat(attacker)) {
            throw new IllegalGameActionException("Un Pokémon Dormido o Paralizado no puede atacar");
        }

        List<AttackDefinition> attacks = attacker.activeCard().definition().attacks();
        if (action.attackIndex() < 0 || action.attackIndex() >= attacks.size()) {
            throw new IllegalGameActionException("Índice de ataque inválido: " + action.attackIndex());
        }

        AttackDefinition chosenAttack = attacks.get(action.attackIndex());
        if (!EnergyCostMatcher.canPay(attacker, chosenAttack.cost())) {
            throw new IllegalGameActionException("Energía insuficiente para usar " + chosenAttack.name());
        }

        PlayerId defenderId = actor.opponent();
        PlayerState defenderState = state.player(defenderId);
        PokemonInPlay defender = defenderState.active();
        if (defender == null) {
            throw new IllegalGameActionException("El rival no tiene Pokémon Activo");
        }

        if (attacker.statusConditions().contains(StatusCondition.CONFUSED)) {
            boolean heads = randomProvider.flipCoin();
            events.emit(
                    GameEventType.COIN_FLIPPED,
                    actor,
                    attacker.activeCard().definition().name() + " está Confundido: lanzó moneda y salió "
                            + (heads ? "CARA." : "CRUZ (30 de daño propio)."),
                    Map.of("reason", "CONFUSION", "result", heads ? "HEADS" : "TAILS", "heads", heads)
            );
            if (!heads) {
                PokemonInPlay selfDamaged = attacker.withDamage(attacker.damage() + 30);
                events.emit(
                        GameEventType.DAMAGE_DEALT,
                        actor,
                        selfDamaged.activeCard().definition().name() + " se hirió a sí mismo con 30 de daño ("
                                + selfDamaged.remainingHp() + "/" + selfDamaged.maxHp() + " PS).",
                        Map.of(
                                "targetName", selfDamaged.activeCard().definition().name(),
                                "damage", 30,
                                "remainingHp", selfDamaged.remainingHp(),
                                "maxHp", selfDamaged.maxHp(),
                                "selfDamage", true
                        )
                );
                GameState afterConfusion = state.withPlayer(actor, replaceActive(attackerState, selfDamaged));
                return resolveKnockOutsAndProceed(afterConfusion, actor, TurnPhase.ATTACK_PHASE, events);
            }
        }

        events.emit(
                GameEventType.ATTACK_DECLARED,
                actor,
                attacker.activeCard().definition().name() + " usó " + chosenAttack.name() + ".",
                Map.of("attackName", chosenAttack.name(), "attackerName", attacker.activeCard().definition().name())
        );

        int damageDealt = DamageCalculator.calculateDamage(
                attacker,
                defender,
                chosenAttack.baseDamage(),
                attackerState.plusPowerBonus()
        );
        PokemonInPlay damagedDefender = defender.withDamage(defender.damage() + damageDealt);

        events.emit(
                GameEventType.DAMAGE_DEALT,
                actor,
                damagedDefender.activeCard().definition().name() + " recibió " + damageDealt + " de daño ("
                        + damagedDefender.remainingHp() + "/" + damagedDefender.maxHp() + " PS).",
                Map.of(
                        "targetName", damagedDefender.activeCard().definition().name(),
                        "damage", damageDealt,
                        "remainingHp", damagedDefender.remainingHp(),
                        "maxHp", damagedDefender.maxHp()
                )
        );

        GameState afterAttack = state.withPlayer(defenderId, replaceActive(defenderState, damagedDefender));
        return resolveKnockOutsAndProceed(afterAttack, actor, TurnPhase.ATTACK_PHASE, events);
    }

    private EngineStepResult handlePromoteActive(
            GameState state,
            PlayerId actor,
            PromoteActiveAction action,
            EventCollector events
    ) {
        if (state.awaitingPromotionPlayer() != actor) {
            throw new IllegalGameActionException("No es tu turno de promover un Pokémon desde Banca");
        }

        PlayerState actorState = state.player(actor);
        List<PokemonInPlay> nextBench = new ArrayList<>(actorState.bench());
        PokemonInPlay promoted = null;
        for (int i = 0; i < nextBench.size(); i++) {
            if (nextBench.get(i).instanceId().equals(action.benchInstanceId())) {
                promoted = nextBench.remove(i);
                break;
            }
        }
        if (promoted == null) {
            throw new IllegalGameActionException("El Pokémon elegido no se encuentra en tu Banca");
        }

        PlayerState updatedActorState = new PlayerState(
                actorState.deck(),
                actorState.hand(),
                promoted,
                nextBench,
                actorState.prizes(),
                actorState.discardPile(),
                actorState.energyAttachedThisTurn(),
                actorState.retreatedThisTurn(),
                actorState.setupReady(),
                actorState.mulliganCount(),
                actorState.plusPowerBonus()
        );

        events.emit(
                GameEventType.ACTIVE_PROMOTED,
                actor,
                displayPlayer(actor) + " promovió a " + promoted.activeCard().definition().name() + " al puesto Activo.",
                Map.of("pokemonName", promoted.activeCard().definition().name())
        );

        GameState updated = state.withPlayer(actor, updatedActorState);
        PlayerId secondPlayer = actor.opponent();
        if (updated.player(secondPlayer).active() == null && !updated.player(secondPlayer).bench().isEmpty()) {
            GameState nextPromo = new GameState(
                    updated.turnNumber(),
                    updated.activePlayer(),
                    updated.startingPlayer(),
                    state.phase(),
                    secondPlayer,
                    updated.players(),
                    null,
                    null,
                    events.nextSequence()
            );
            return new EngineStepResult(nextPromo, events.events());
        }

        GameState clearedPromo = new GameState(
                updated.turnNumber(),
                updated.activePlayer(),
                updated.startingPlayer(),
                state.phase(),
                null,
                updated.players(),
                null,
                null,
                events.nextSequence()
        );

        if (state.phase() == TurnPhase.POKEMON_CHECKUP) {
            return new EngineStepResult(
                    completeTurnTransitionAfterCheckup(clearedPromo, state.activePlayer(), events),
                    events.events()
            );
        }
        return executePokemonCheckupAndAdvance(clearedPromo, state.activePlayer(), events);
    }

    private EngineStepResult handleEndTurn(GameState state, PlayerId actor, EventCollector events) {
        requireMainPhaseAndTurn(state, actor);
        return executePokemonCheckupAndAdvance(state, actor, events);
    }

    private EngineStepResult handleEvolve(
            GameState state,
            PlayerId actor,
            EvolveAction action,
            EventCollector events
    ) {
        requireMainPhaseAndTurn(state, actor);
        if (state.turnNumber() <= 2) {
            throw new IllegalGameActionException("No se puede evolucionar en el primer turno de ningún jugador");
        }
        if (action.evolutionCardInstanceId() == null || action.evolutionCardInstanceId().isBlank()
                || action.targetPokemonInstanceId() == null || action.targetPokemonInstanceId().isBlank()) {
            throw new IllegalGameActionException("Debes indicar carta de evolución y Pokémon objetivo");
        }

        PlayerState actorState = state.player(actor);
        List<CardInstance> nextHand = new ArrayList<>(actorState.hand());
        CardInstance evoCard = removeHandCard(
                nextHand, action.evolutionCardInstanceId(), "La carta de evolución no está en tu mano"
        );
        if (!evoCard.definition().evolutionCard()) {
            throw new IllegalGameActionException("La carta seleccionada no es una Evolución (Stage 1 / Stage 2)");
        }
        String requiredPreEvo = evoCard.definition().evolveFrom();
        if (requiredPreEvo == null || requiredPreEvo.isBlank()) {
            throw new IllegalGameActionException("La carta de evolución no especifica preevolución");
        }

        PokemonInPlay nextActive = actorState.active();
        List<PokemonInPlay> nextBench = new ArrayList<>(actorState.bench());
        String preName;

        if (nextActive != null && nextActive.instanceId().equals(action.targetPokemonInstanceId())) {
            validateEvolutionTarget(nextActive, requiredPreEvo, state.turnNumber());
            preName = nextActive.activeCard().definition().name();
            nextActive = buildEvolvedPokemon(nextActive, evoCard, state.turnNumber());
        } else {
            int benchIndex = -1;
            for (int i = 0; i < nextBench.size(); i++) {
                if (nextBench.get(i).instanceId().equals(action.targetPokemonInstanceId())) {
                    benchIndex = i;
                    break;
                }
            }
            if (benchIndex < 0) {
                throw new IllegalGameActionException("El Pokémon objetivo para evolucionar no está en juego");
            }
            PokemonInPlay target = nextBench.get(benchIndex);
            validateEvolutionTarget(target, requiredPreEvo, state.turnNumber());
            preName = target.activeCard().definition().name();
            nextBench.set(benchIndex, buildEvolvedPokemon(target, evoCard, state.turnNumber()));
        }

        PlayerState updatedPlayer = new PlayerState(
                actorState.deck(),
                nextHand,
                nextActive,
                nextBench,
                actorState.prizes(),
                actorState.discardPile(),
                actorState.energyAttachedThisTurn(),
                actorState.retreatedThisTurn(),
                actorState.setupReady(),
                actorState.mulliganCount(),
                actorState.plusPowerBonus()
        );

        events.emit(
                GameEventType.POKEMON_EVOLVED,
                actor,
                displayPlayer(actor) + " evolucionó a " + preName + " en " + evoCard.definition().name() + ".",
                Map.of("fromName", preName, "toName", evoCard.definition().name())
        );

        GameState nextState = withSequence(state.withPlayer(actor, updatedPlayer), events.nextSequence());
        return new EngineStepResult(nextState, events.events());
    }

    private void validateEvolutionTarget(PokemonInPlay target, String requiredPreEvo, int currentTurn) {
        if (!target.activeCard().definition().name().equalsIgnoreCase(requiredPreEvo)) {
            throw new IllegalGameActionException(
                    target.activeCard().definition().name() + " no puede evolucionar desde " + requiredPreEvo
            );
        }
        if (target.turnEnteredPlay() >= currentTurn || target.turnLastEvolved() >= currentTurn) {
            throw new IllegalGameActionException(
                    "No puedes evolucionar un Pokémon en el mismo turno en que entró en juego o evolucionó"
            );
        }
    }

    private PokemonInPlay buildEvolvedPokemon(PokemonInPlay target, CardInstance evoCard, int currentTurn) {
        List<CardInstance> nextChain = new ArrayList<>(target.evolutionChain());
        nextChain.add(evoCard);
        return new PokemonInPlay(
                target.instanceId(),
                evoCard,
                nextChain,
                target.attachedEnergies(),
                target.damage(),
                Set.of(),
                target.turnEnteredPlay(),
                currentTurn,
                List.of()
        );
    }

    private EngineStepResult handleRetreat(
            GameState state,
            PlayerId actor,
            RetreatAction action,
            EventCollector events
    ) {
        requireMainPhaseAndTurn(state, actor);
        PlayerState actorState = state.player(actor);
        if (actorState.retreatedThisTurn()) {
            throw new IllegalGameActionException("Solo puedes retirarte una vez por turno");
        }
        PokemonInPlay currentActive = actorState.active();
        if (currentActive == null) {
            throw new IllegalGameActionException("No tienes ningún Pokémon Activo para retirar");
        }
        if (actorState.bench().isEmpty()) {
            throw new IllegalGameActionException("No tienes Pokémon en Banca para relevar al Activo");
        }
        if (currentActive.statusConditions().contains(StatusCondition.ASLEEP)
                || currentActive.statusConditions().contains(StatusCondition.PARALYZED)) {
            throw new IllegalGameActionException("No puedes retirar un Pokémon Dormido o Paralizado");
        }
        if (action.benchInstanceId() == null || action.benchInstanceId().isBlank()) {
            throw new IllegalGameActionException("Debes seleccionar un Pokémon de la Banca como relevo");
        }

        List<PokemonInPlay> nextBench = new ArrayList<>(actorState.bench());
        int benchIndex = -1;
        for (int i = 0; i < nextBench.size(); i++) {
            if (nextBench.get(i).instanceId().equals(action.benchInstanceId())) {
                benchIndex = i;
                break;
            }
        }
        if (benchIndex < 0) {
            throw new IllegalGameActionException("El Pokémon elegido no se encuentra en tu Banca");
        }

        int retreatCost = currentActive.activeCard().definition().retreatCost();
        RetreatPayment payment = payRetreatCost(
                currentActive.attachedEnergies(), retreatCost, action.discardEnergyInstanceIds()
        );

        PokemonInPlay retreatedToBench = currentActive
                .withAttachedEnergies(payment.remaining())
                .withClearedCombatState();
        PokemonInPlay promotedActive = nextBench.get(benchIndex);
        nextBench.set(benchIndex, retreatedToBench);

        List<CardInstance> nextDiscard = new ArrayList<>(actorState.discardPile());
        nextDiscard.addAll(payment.paid());

        PlayerState updatedPlayer = new PlayerState(
                actorState.deck(),
                actorState.hand(),
                promotedActive,
                nextBench,
                actorState.prizes(),
                nextDiscard,
                actorState.energyAttachedThisTurn(),
                true,
                actorState.setupReady(),
                actorState.mulliganCount(),
                actorState.plusPowerBonus()
        );

        events.emit(
                GameEventType.POKEMON_RETREATED,
                actor,
                displayPlayer(actor) + " retiró a " + currentActive.activeCard().definition().name()
                        + " por " + promotedActive.activeCard().definition().name() + ".",
                Map.of(
                        "retreatedName", currentActive.activeCard().definition().name(),
                        "promotedName", promotedActive.activeCard().definition().name(),
                        "energiesDiscarded", payment.paid().size()
                )
        );

        GameState nextState = withSequence(state.withPlayer(actor, updatedPlayer), events.nextSequence());
        return new EngineStepResult(nextState, events.events());
    }

    private RetreatPayment payRetreatCost(
            List<CardInstance> attachedEnergies,
            int retreatCost,
            List<String> requestedIds
    ) {
        if (retreatCost <= 0) {
            return new RetreatPayment(List.of(), attachedEnergies);
        }

        List<CardInstance> remaining = new ArrayList<>(attachedEnergies);
        List<CardInstance> paid = new ArrayList<>();
        int paidUnits = 0;

        if (requestedIds != null && !requestedIds.isEmpty()) {
            Set<String> seen = new HashSet<>();
            for (String reqId : requestedIds) {
                if (!seen.add(reqId)) {
                    throw new IllegalGameActionException("Energía duplicada en pago de retirada");
                }
                int idx = -1;
                for (int i = 0; i < remaining.size(); i++) {
                    if (remaining.get(i).instanceId().equals(reqId)) {
                        idx = i;
                        break;
                    }
                }
                if (idx < 0) {
                    throw new IllegalGameActionException("Energía de retirada no está adherida al Pokémon Activo");
                }
                CardInstance removed = remaining.remove(idx);
                paid.add(removed);
                paidUnits += Math.max(1, removed.definition().providedEnergyCount());
            }
            if (paidUnits < retreatCost) {
                throw new IllegalGameActionException("Las energías seleccionadas no cubren el costo de retirada (" + retreatCost + ")");
            }
            return new RetreatPayment(paid, remaining);
        }

        int totalAvailable = 0;
        for (CardInstance energy : attachedEnergies) {
            totalAvailable += Math.max(1, energy.definition().providedEnergyCount());
        }
        if (totalAvailable < retreatCost) {
            throw new IllegalGameActionException("Energía insuficiente para retirarse (costo: " + retreatCost + ")");
        }

        List<CardInstance> orderedCandidates = new ArrayList<>(attachedEnergies);
        orderedCandidates.sort(Comparator.comparingInt(c -> Math.max(1, c.definition().providedEnergyCount())));
        Set<String> chosenIds = new HashSet<>();
        for (CardInstance candidate : orderedCandidates) {
            if (paidUnits >= retreatCost) {
                break;
            }
            chosenIds.add(candidate.instanceId());
            paid.add(candidate);
            paidUnits += Math.max(1, candidate.definition().providedEnergyCount());
        }
        remaining.removeIf(card -> chosenIds.contains(card.instanceId()));
        return new RetreatPayment(paid, remaining);
    }

    private record RetreatPayment(List<CardInstance> paid, List<CardInstance> remaining) {
    }

    private EngineStepResult resolveKnockOutsAndProceed(
            GameState state,
            PlayerId turnOwner,
            TurnPhase originPhase,
            EventCollector events
    ) {
        PlayerId opponent = turnOwner.opponent();
        GameState current = state;
        boolean opponentKo = isActiveKnockedOut(current, opponent);
        boolean ownerKo = isActiveKnockedOut(current, turnOwner);

        if (!opponentKo && !ownerKo) {
            if (originPhase == TurnPhase.ATTACK_PHASE) {
                return executePokemonCheckupAndAdvance(current, turnOwner, events);
            }
            return new EngineStepResult(completeTurnTransitionAfterCheckup(current, turnOwner, events), events.events());
        }

        if (opponentKo) {
            current = discardKnockedOutAndAwardPrize(current, opponent, turnOwner, events);
        }
        if (ownerKo) {
            current = discardKnockedOutAndAwardPrize(current, turnOwner, opponent, events);
        }

        PlayerState pState = current.player(PlayerId.PLAYER);
        PlayerState cState = current.player(PlayerId.CPU);
        int playerWinConditions = (pState.prizes().isEmpty() ? 1 : 0)
                + (cState.active() == null && cState.bench().isEmpty() ? 1 : 0);
        int cpuWinConditions = (cState.prizes().isEmpty() ? 1 : 0)
                + (pState.active() == null && pState.bench().isEmpty() ? 1 : 0);

        if (playerWinConditions > 0 || cpuWinConditions > 0) {
            PlayerId winner;
            if (playerWinConditions > cpuWinConditions) {
                winner = PlayerId.PLAYER;
            } else if (cpuWinConditions > playerWinConditions) {
                winner = PlayerId.CPU;
            } else {
                winner = turnOwner;
            }
            WinReason reason = current.player(winner).prizes().isEmpty()
                    ? WinReason.ALL_PRIZES_TAKEN
                    : WinReason.NO_POKEMON_LEFT;
            return finishMatch(current, winner, reason, events);
        }

        PlayerId firstToPromote = null;
        if (current.player(opponent).active() == null) {
            firstToPromote = opponent;
        } else if (current.player(turnOwner).active() == null) {
            firstToPromote = turnOwner;
        }

        if (firstToPromote != null) {
            GameState awaitingPromotion = new GameState(
                    current.turnNumber(),
                    current.activePlayer(),
                    current.startingPlayer(),
                    originPhase,
                    firstToPromote,
                    current.players(),
                    null,
                    null,
                    events.nextSequence()
            );
            return new EngineStepResult(awaitingPromotion, events.events());
        }

        if (originPhase == TurnPhase.ATTACK_PHASE) {
            return executePokemonCheckupAndAdvance(current, turnOwner, events);
        }
        return new EngineStepResult(completeTurnTransitionAfterCheckup(current, turnOwner, events), events.events());
    }

    private boolean isActiveKnockedOut(GameState state, PlayerId owner) {
        PokemonInPlay active = state.player(owner).active();
        return active != null && active.knockedOut();
    }

    private GameState discardKnockedOutAndAwardPrize(
            GameState state,
            PlayerId koOwner,
            PlayerId prizeTaker,
            EventCollector events
    ) {
        PlayerState koState = state.player(koOwner);
        PokemonInPlay koPokemon = koState.active();
        if (koPokemon == null) {
            return state;
        }

        List<CardInstance> nextDiscard = new ArrayList<>(koState.discardPile());
        nextDiscard.addAll(koPokemon.allCards());
        PlayerState updatedKoOwner = new PlayerState(
                koState.deck(),
                koState.hand(),
                null,
                koState.bench(),
                koState.prizes(),
                nextDiscard,
                koState.energyAttachedThisTurn(),
                koState.retreatedThisTurn(),
                koState.setupReady(),
                koState.mulliganCount(),
                koState.plusPowerBonus()
        );

        events.emit(
                GameEventType.POKEMON_KNOCKED_OUT,
                koOwner,
                koPokemon.activeCard().definition().name() + " quedó Fuera de Combate.",
                Map.of("pokemonName", koPokemon.activeCard().definition().name())
        );

        GameState current = state.withPlayer(koOwner, updatedKoOwner);
        PlayerState takerState = current.player(prizeTaker);
        if (!takerState.prizes().isEmpty()) {
            List<CardInstance> nextPrizes = new ArrayList<>(takerState.prizes());
            CardInstance prizeCard = nextPrizes.remove(0);
            List<CardInstance> nextHand = new ArrayList<>(takerState.hand());
            nextHand.add(prizeCard);
            PlayerState updatedTaker = new PlayerState(
                    takerState.deck(),
                    nextHand,
                    takerState.active(),
                    takerState.bench(),
                    nextPrizes,
                    takerState.discardPile(),
                    takerState.energyAttachedThisTurn(),
                    takerState.retreatedThisTurn(),
                    takerState.setupReady(),
                    takerState.mulliganCount(),
                    takerState.plusPowerBonus()
            );
            current = current.withPlayer(prizeTaker, updatedTaker);

            events.emit(
                    GameEventType.PRIZE_TAKEN,
                    prizeTaker,
                    displayPlayer(prizeTaker) + " tomó 1 carta de Premio (" + nextPrizes.size() + " restantes).",
                    Map.of("prizesRemaining", nextPrizes.size())
            );
        }
        return current;
    }

    private EngineStepResult executePokemonCheckupAndAdvance(
            GameState state,
            PlayerId endingPlayer,
            EventCollector events
    ) {
        GameState current = state;
        for (PlayerId pid : List.of(endingPlayer, endingPlayer.opponent())) {
            PlayerState ps = current.player(pid);
            PokemonInPlay active = ps.active();
            if (active == null || active.statusConditions().isEmpty()) {
                continue;
            }

            PokemonInPlay updatedActive = active;
            if (updatedActive.statusConditions().contains(StatusCondition.POISONED)) {
                updatedActive = updatedActive.withDamage(updatedActive.damage() + 10);
                events.emit(
                        GameEventType.CHECKUP_TICK,
                        pid,
                        updatedActive.activeCard().definition().name() + " sufrió 10 de daño por Veneno ("
                                + updatedActive.remainingHp() + "/" + updatedActive.maxHp() + " PS).",
                        Map.of("status", StatusCondition.POISONED.name(), "damage", 10)
                );
            }

            if (updatedActive.statusConditions().contains(StatusCondition.BURNED)) {
                updatedActive = updatedActive.withDamage(updatedActive.damage() + 20);
                events.emit(
                        GameEventType.CHECKUP_TICK,
                        pid,
                        updatedActive.activeCard().definition().name() + " sufrió 20 de daño por Quemadura ("
                                + updatedActive.remainingHp() + "/" + updatedActive.maxHp() + " PS).",
                        Map.of("status", StatusCondition.BURNED.name(), "damage", 20)
                );
                boolean heads = randomProvider.flipCoin();
                events.emit(
                        GameEventType.COIN_FLIPPED,
                        pid,
                        "Checkup Quemadura de " + updatedActive.activeCard().definition().name() + ": "
                                + (heads ? "CARA (se recupera)." : "CRUZ (continúa quemado)."),
                        Map.of("reason", "BURN", "result", heads ? "HEADS" : "TAILS", "heads", heads)
                );
                if (heads) {
                    updatedActive = StatusRules.removeStatus(updatedActive, StatusCondition.BURNED);
                }
            }

            if (updatedActive.statusConditions().contains(StatusCondition.ASLEEP)) {
                boolean heads = randomProvider.flipCoin();
                events.emit(
                        GameEventType.COIN_FLIPPED,
                        pid,
                        "Checkup Sueño de " + updatedActive.activeCard().definition().name() + ": "
                                + (heads ? "CARA (despierta)." : "CRUZ (sigue dormido)."),
                        Map.of("reason", "SLEEP", "result", heads ? "HEADS" : "TAILS", "heads", heads)
                );
                if (heads) {
                    updatedActive = StatusRules.removeStatus(updatedActive, StatusCondition.ASLEEP);
                }
            }

            if (pid == endingPlayer && updatedActive.statusConditions().contains(StatusCondition.PARALYZED)) {
                updatedActive = StatusRules.removeStatus(updatedActive, StatusCondition.PARALYZED);
                events.emit(
                        GameEventType.CHECKUP_TICK,
                        pid,
                        updatedActive.activeCard().definition().name() + " se recuperó de la Parálisis.",
                        Map.of("statusCleared", StatusCondition.PARALYZED.name())
                );
            }

            current = current.withPlayer(pid, replaceActive(ps, updatedActive));
        }

        current = purgeExpiredModifiers(current, current.turnNumber());
        return resolveKnockOutsAndProceed(current, endingPlayer, TurnPhase.POKEMON_CHECKUP, events);
    }

    private GameState completeTurnTransitionAfterCheckup(
            GameState state,
            PlayerId endingPlayer,
            EventCollector events
    ) {
        GameState cleaned = purgeExpiredModifiers(state, state.turnNumber());
        PlayerState resetEnding = resetTurnFlags(cleaned.player(endingPlayer));
        GameState afterReset = cleaned.withPlayer(endingPlayer, resetEnding);

        events.emit(
                GameEventType.TURN_ENDED,
                endingPlayer,
                "Finalizó el turno " + state.turnNumber() + " de " + displayPlayer(endingPlayer) + ".",
                Map.of("turnNumber", state.turnNumber())
        );

        return startTurn(afterReset, endingPlayer.opponent(), state.turnNumber() + 1, events);
    }

    private GameState purgeExpiredModifiers(GameState state, int currentTurn) {
        GameState updated = state;
        for (PlayerId pid : List.of(PlayerId.PLAYER, PlayerId.CPU)) {
            PlayerState ps = updated.player(pid);
            PokemonInPlay nextActive = purgePokemonModifiers(ps.active(), currentTurn);
            List<PokemonInPlay> nextBench = ps.bench().stream()
                    .map(p -> purgePokemonModifiers(p, currentTurn))
                    .toList();
            updated = updated.withPlayer(pid, new PlayerState(
                    ps.deck(), ps.hand(), nextActive, nextBench, ps.prizes(), ps.discardPile(),
                    ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(),
                    ps.mulliganCount(), ps.plusPowerBonus()
            ));
        }
        return updated;
    }

    private PokemonInPlay purgePokemonModifiers(PokemonInPlay pokemon, int currentTurn) {
        if (pokemon == null || pokemon.modifiers().isEmpty()) {
            return pokemon;
        }
        List<ActiveModifier> surviving = pokemon.modifiers().stream()
                .filter(m -> m.expiresAfterTurn() > currentTurn)
                .toList();
        return surviving.size() == pokemon.modifiers().size()
                ? pokemon
                : pokemon.withModifiers(surviving);
    }

    private GameState startTurn(GameState state, PlayerId nextPlayer, int nextTurnNumber, EventCollector events) {
        events.emit(
                GameEventType.TURN_STARTED,
                nextPlayer,
                "Comienza el turno " + nextTurnNumber + " (" + displayPlayer(nextPlayer) + ").",
                Map.of("turnNumber", nextTurnNumber)
        );

        PlayerState playerState = resetTurnFlags(state.player(nextPlayer));
        if (playerState.deck().isEmpty()) {
            PlayerId winner = nextPlayer.opponent();
            events.emit(
                    GameEventType.GAME_ENDED,
                    winner,
                    displayPlayer(nextPlayer) + " no tiene cartas para robar. Victoria para " + displayPlayer(winner) + ".",
                    Map.of("winner", winner.name(), "reason", WinReason.DECK_OUT.name())
            );
            return new GameState(
                    nextTurnNumber,
                    nextPlayer,
                    state.startingPlayer(),
                    TurnPhase.GAME_OVER,
                    null,
                    state.withPlayer(nextPlayer, playerState).players(),
                    winner,
                    WinReason.DECK_OUT,
                    events.nextSequence()
            );
        }

        List<CardInstance> nextDeck = new ArrayList<>(playerState.deck());
        CardInstance drawn = nextDeck.remove(0);
        List<CardInstance> nextHand = new ArrayList<>(playerState.hand());
        nextHand.add(drawn);

        PlayerState drawnState = new PlayerState(
                nextDeck,
                nextHand,
                playerState.active(),
                playerState.bench(),
                playerState.prizes(),
                playerState.discardPile(),
                false,
                false,
                playerState.setupReady(),
                playerState.mulliganCount()
        );

        events.emit(
                GameEventType.CARD_DRAWN,
                nextPlayer,
                displayPlayer(nextPlayer) + " robó 1 carta al iniciar su turno.",
                Map.of("reason", "TURN_DRAW", "deckRemaining", nextDeck.size())
        );

        return new GameState(
                nextTurnNumber,
                nextPlayer,
                state.startingPlayer(),
                TurnPhase.MAIN_PHASE,
                null,
                state.withPlayer(nextPlayer, drawnState).players(),
                null,
                null,
                events.nextSequence()
        );
    }

    private GameState applyNetMulliganDraws(GameState state, EventCollector events) {
        GameState current = state;
        for (PlayerId receiver : List.of(state.startingPlayer(), state.startingPlayer().opponent())) {
            PlayerState myState = current.player(receiver);
            PlayerState oppState = current.player(receiver.opponent());
            int netExtra = Math.max(0, oppState.mulliganCount() - myState.mulliganCount());
            int actualDraws = Math.min(netExtra, myState.deck().size());
            if (actualDraws <= 0) {
                continue;
            }

            List<CardInstance> nextDeck = new ArrayList<>(myState.deck());
            List<CardInstance> nextHand = new ArrayList<>(myState.hand());
            for (int i = 0; i < actualDraws; i++) {
                nextHand.add(nextDeck.remove(0));
            }

            PlayerState updated = new PlayerState(
                    nextDeck,
                    nextHand,
                    myState.active(),
                    myState.bench(),
                    myState.prizes(),
                    myState.discardPile(),
                    myState.energyAttachedThisTurn(),
                    myState.retreatedThisTurn(),
                    myState.setupReady(),
                    myState.mulliganCount()
            );
            current = current.withPlayer(receiver, updated);

            events.emit(
                    GameEventType.CARD_DRAWN,
                    receiver,
                    displayPlayer(receiver) + " robó " + actualDraws + " carta(s) extra por mulligan rival.",
                    Map.of("reason", "MULLIGAN_BONUS", "count", actualDraws)
            );
        }
        return current;
    }

    private EngineStepResult finishMatch(
            GameState state,
            PlayerId winner,
            WinReason reason,
            EventCollector events
    ) {
        String reasonLabel = switch (reason) {
            case ALL_PRIZES_TAKEN -> "tomó los 6 premios";
            case NO_POKEMON_LEFT -> "dejó al rival sin Pokémon en juego";
            case DECK_OUT -> "el rival se quedó sin cartas en el mazo";
            case CONCEDED -> "el rival se rindió";
        };
        events.emit(
                GameEventType.GAME_ENDED,
                winner,
                "¡Victoria de " + displayPlayer(winner) + " (" + reasonLabel + ")!",
                Map.of("winner", winner.name(), "reason", reason.name())
        );

        GameState finished = new GameState(
                state.turnNumber(),
                state.activePlayer(),
                state.startingPlayer(),
                TurnPhase.GAME_OVER,
                null,
                state.players(),
                winner,
                reason,
                events.nextSequence()
        );
        return new EngineStepResult(finished, events.events());
    }

    private PlayerState drawOpeningHand(
            PlayerId owner,
            List<CardInstance> instances,
            RandomProvider random,
            EventCollector events
    ) {
        int mulligans = 0;
        while (true) {
            List<CardInstance> shuffled = new ArrayList<>(instances);
            random.shuffle(shuffled);
            List<CardInstance> hand = new ArrayList<>(shuffled.subList(0, INITIAL_HAND_SIZE));
            List<CardInstance> deck = new ArrayList<>(shuffled.subList(INITIAL_HAND_SIZE, shuffled.size()));

            boolean hasBasic = hand.stream().anyMatch(card -> card.definition().basicPokemon());
            if (hasBasic) {
                return PlayerState.initial(deck, hand, mulligans);
            }

            mulligans++;
            events.emit(
                    GameEventType.MULLIGAN,
                    owner,
                    displayPlayer(owner) + " no tuvo Pokémon Básico y realizó mulligan (#" + mulligans + ").",
                    Map.of("mulliganCount", mulligans)
            );

            if (mulligans >= MAX_MULLIGANS) {
                return forceBasicIntoOpeningHand(shuffled, mulligans);
            }
        }
    }

    private PlayerState forceBasicIntoOpeningHand(List<CardInstance> shuffled, int mulligans) {
        List<CardInstance> copy = new ArrayList<>(shuffled);
        for (int i = INITIAL_HAND_SIZE; i < copy.size(); i++) {
            if (copy.get(i).definition().basicPokemon()) {
                CardInstance basic = copy.get(i);
                copy.set(i, copy.get(0));
                copy.set(0, basic);
                break;
            }
        }
        return PlayerState.initial(
                copy.subList(INITIAL_HAND_SIZE, copy.size()),
                copy.subList(0, INITIAL_HAND_SIZE),
                mulligans
        );
    }

    private void validateDeck(List<CardDefinition> deck, String label) {
        if (deck == null || deck.size() != DECK_SIZE) {
            int size = deck == null ? 0 : deck.size();
            throw new IllegalGameActionException("El mazo de " + label + " debe tener exactamente 60 cartas (actual: " + size + ")");
        }
        boolean hasBasic = deck.stream().anyMatch(CardDefinition::basicPokemon);
        if (!hasBasic) {
            throw new IllegalGameActionException("El mazo de " + label + " debe incluir al menos 1 Pokémon Básico");
        }
    }

    private List<CardInstance> instantiateDeck(String prefix, List<CardDefinition> deck) {
        List<CardInstance> instances = new ArrayList<>(deck.size());
        for (int i = 0; i < deck.size(); i++) {
            instances.add(new CardInstance(prefix + "-" + (i + 1), deck.get(i)));
        }
        return instances;
    }

    private void requireMainPhaseAndTurn(GameState state, PlayerId actor) {
        if (state.phase() != TurnPhase.MAIN_PHASE) {
            throw new IllegalGameActionException("Esta acción solo puede ejecutarse en MAIN_PHASE (actual: " + state.phase() + ")");
        }
        if (state.activePlayer() != actor) {
            throw new IllegalGameActionException("No es el turno de " + displayPlayer(actor));
        }
    }

    private CardInstance removeHandCard(List<CardInstance> hand, String instanceId, String errorMessage) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).instanceId().equals(instanceId)) {
                return hand.remove(i);
            }
        }
        throw new IllegalGameActionException(errorMessage);
    }

    private PokemonInPlay attachEnergyToPokemon(PokemonInPlay target, CardInstance energyCard) {
        List<CardInstance> nextEnergies = new ArrayList<>(target.attachedEnergies());
        nextEnergies.add(energyCard);
        return target.withAttachedEnergies(nextEnergies);
    }

    private PlayerState replaceActive(PlayerState state, PokemonInPlay active) {
        return new PlayerState(
                state.deck(),
                state.hand(),
                active,
                state.bench(),
                state.prizes(),
                state.discardPile(),
                state.energyAttachedThisTurn(),
                state.retreatedThisTurn(),
                state.setupReady(),
                state.mulliganCount(),
                state.plusPowerBonus()
        );
    }

    private PlayerState resetTurnFlags(PlayerState state) {
        return new PlayerState(
                state.deck(),
                state.hand(),
                state.active(),
                state.bench(),
                state.prizes(),
                state.discardPile(),
                false,
                false,
                state.setupReady(),
                state.mulliganCount(),
                0
        );
    }

    private GameState withSequence(GameState state, long nextEventSequence) {
        return new GameState(
                state.turnNumber(),
                state.activePlayer(),
                state.startingPlayer(),
                state.phase(),
                state.awaitingPromotionPlayer(),
                state.players(),
                state.winner(),
                state.winReason(),
                nextEventSequence
        );
    }

    private String displayPlayer(PlayerId playerId) {
        return playerId == PlayerId.PLAYER ? "Jugador" : "CPU";
    }

    private static final class EventCollector {
        private long nextSeq;
        private final List<GameEvent> collected = new ArrayList<>();

        private EventCollector(long initialSeq) {
            this.nextSeq = initialSeq;
        }

        private void emit(GameEventType type, PlayerId actor, String message, Map<String, Object> metadata) {
            collected.add(new GameEvent(nextSeq++, type, actor, message, metadata));
        }

        private long nextSequence() {
            return nextSeq;
        }

        private List<GameEvent> events() {
            return List.copyOf(collected);
        }
    }
}
