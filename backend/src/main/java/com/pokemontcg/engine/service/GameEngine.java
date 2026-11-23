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
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.rules.EnergyCostMatcher;

import java.util.ArrayList;
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
            case EvolveAction ignored ->
                    throw new IllegalGameActionException("Acción disponible a partir de la Fase 4");
            case PlayTrainerAction ignored ->
                    throw new IllegalGameActionException("Acción disponible a partir de la Fase 4");
            case RetreatAction ignored ->
                    throw new IllegalGameActionException("Acción disponible a partir de la Fase 4");
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
                actorState.mulliganCount()
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
                actorState.mulliganCount()
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

        events.emit(
                GameEventType.ATTACK_DECLARED,
                actor,
                attacker.activeCard().definition().name() + " usó " + chosenAttack.name() + ".",
                Map.of("attackName", chosenAttack.name(), "attackerName", attacker.activeCard().definition().name())
        );

        int damageDealt = chosenAttack.baseDamage();
        PokemonInPlay damagedDefender = new PokemonInPlay(
                defender.instanceId(),
                defender.activeCard(),
                defender.evolutionChain(),
                defender.attachedEnergies(),
                defender.damage() + damageDealt,
                defender.statusConditions(),
                defender.turnEnteredPlay(),
                defender.turnLastEvolved()
        );

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

        if (!damagedDefender.knockedOut()) {
            GameState afterAttack = state.withPlayer(defenderId, replaceActive(defenderState, damagedDefender));
            GameState nextTurnState = advanceToNextTurn(afterAttack, actor, events);
            return new EngineStepResult(nextTurnState, events.events());
        }

        // Knock Out resolution
        List<CardInstance> nextDefenderDiscard = new ArrayList<>(defenderState.discardPile());
        nextDefenderDiscard.addAll(damagedDefender.allCards());
        PlayerState koDefenderState = new PlayerState(
                defenderState.deck(),
                defenderState.hand(),
                null,
                defenderState.bench(),
                defenderState.prizes(),
                nextDefenderDiscard,
                defenderState.energyAttachedThisTurn(),
                defenderState.retreatedThisTurn(),
                defenderState.setupReady(),
                defenderState.mulliganCount()
        );

        events.emit(
                GameEventType.POKEMON_KNOCKED_OUT,
                defenderId,
                damagedDefender.activeCard().definition().name() + " quedó Fuera de Combate.",
                Map.of("pokemonName", damagedDefender.activeCard().definition().name())
        );

        PlayerState nextAttackerState = attackerState;
        if (!attackerState.prizes().isEmpty()) {
            List<CardInstance> nextPrizes = new ArrayList<>(attackerState.prizes());
            CardInstance prizeCard = nextPrizes.remove(0);
            List<CardInstance> nextAttackerHand = new ArrayList<>(attackerState.hand());
            nextAttackerHand.add(prizeCard);

            nextAttackerState = new PlayerState(
                    attackerState.deck(),
                    nextAttackerHand,
                    attackerState.active(),
                    attackerState.bench(),
                    nextPrizes,
                    attackerState.discardPile(),
                    attackerState.energyAttachedThisTurn(),
                    attackerState.retreatedThisTurn(),
                    attackerState.setupReady(),
                    attackerState.mulliganCount()
            );

            events.emit(
                    GameEventType.PRIZE_TAKEN,
                    actor,
                    displayPlayer(actor) + " tomó 1 carta de Premio (" + nextPrizes.size() + " restantes).",
                    Map.of("prizesRemaining", nextPrizes.size())
            );
        }

        GameState afterKoState = state
                .withPlayer(actor, nextAttackerState)
                .withPlayer(defenderId, koDefenderState);

        if (nextAttackerState.prizes().isEmpty()) {
            return finishMatch(afterKoState, actor, WinReason.ALL_PRIZES_TAKEN, events);
        }

        if (koDefenderState.bench().isEmpty()) {
            return finishMatch(afterKoState, actor, WinReason.NO_POKEMON_LEFT, events);
        }

        GameState awaitingPromotion = new GameState(
                afterKoState.turnNumber(),
                afterKoState.activePlayer(),
                afterKoState.startingPlayer(),
                TurnPhase.POKEMON_CHECKUP,
                defenderId,
                afterKoState.players(),
                null,
                null,
                events.nextSequence()
        );
        return new EngineStepResult(awaitingPromotion, events.events());
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
                actorState.mulliganCount()
        );

        events.emit(
                GameEventType.ACTIVE_PROMOTED,
                actor,
                displayPlayer(actor) + " promovió a " + promoted.activeCard().definition().name() + " al puesto Activo.",
                Map.of("pokemonName", promoted.activeCard().definition().name())
        );

        GameState updated = new GameState(
                state.turnNumber(),
                state.activePlayer(),
                state.startingPlayer(),
                state.phase(),
                null,
                state.withPlayer(actor, updatedActorState).players(),
                null,
                null,
                events.nextSequence()
        );

        GameState nextTurnState = advanceToNextTurn(updated, state.activePlayer(), events);
        return new EngineStepResult(nextTurnState, events.events());
    }

    private EngineStepResult handleEndTurn(GameState state, PlayerId actor, EventCollector events) {
        requireMainPhaseAndTurn(state, actor);
        GameState nextTurnState = advanceToNextTurn(state, actor, events);
        return new EngineStepResult(nextTurnState, events.events());
    }

    private GameState advanceToNextTurn(GameState state, PlayerId endingPlayer, EventCollector events) {
        PlayerState resetEnding = resetTurnFlags(state.player(endingPlayer));
        GameState afterReset = state.withPlayer(endingPlayer, resetEnding);

        events.emit(
                GameEventType.TURN_ENDED,
                endingPlayer,
                "Finalizó el turno " + state.turnNumber() + " de " + displayPlayer(endingPlayer) + ".",
                Map.of("turnNumber", state.turnNumber())
        );

        return startTurn(afterReset, endingPlayer.opponent(), state.turnNumber() + 1, events);
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
        return new PokemonInPlay(
                target.instanceId(),
                target.activeCard(),
                target.evolutionChain(),
                nextEnergies,
                target.damage(),
                target.statusConditions(),
                target.turnEnteredPlay(),
                target.turnLastEvolved()
        );
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
                state.mulliganCount()
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
                state.mulliganCount()
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
