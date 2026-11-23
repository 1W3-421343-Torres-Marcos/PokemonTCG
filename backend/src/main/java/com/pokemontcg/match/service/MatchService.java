package com.pokemontcg.match.service;

import com.pokemontcg.auth.service.UserStatsService;
import com.pokemontcg.bot.BasicBotStrategy;
import com.pokemontcg.bot.BotDifficulty;
import com.pokemontcg.bot.BotStrategy;
import com.pokemontcg.bot.BotTurnRunner;
import com.pokemontcg.common.exception.ForbiddenException;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import com.pokemontcg.deck.domain.DeckEntity;
import com.pokemontcg.deck.repository.DeckRepository;
import com.pokemontcg.engine.action.ConcedeAction;
import com.pokemontcg.engine.action.GameAction;
import com.pokemontcg.engine.dto.PlayerViewProjector;
import com.pokemontcg.engine.event.GameEvent;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.random.RandomProvider;
import com.pokemontcg.engine.service.EngineStepResult;
import com.pokemontcg.engine.service.GameEngine;
import com.pokemontcg.match.domain.MatchEntity;
import com.pokemontcg.match.domain.MatchStatus;
import com.pokemontcg.match.dto.ActiveMatchSummaryDTO;
import com.pokemontcg.match.dto.CreateMatchRequest;
import com.pokemontcg.match.dto.MatchActionRequest;
import com.pokemontcg.match.dto.MatchTurnResponse;
import com.pokemontcg.match.mapper.CatalogCardMapper;
import com.pokemontcg.match.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final DeckRepository deckRepository;
    private final CatalogCardMapper cardMapper;
    private final UserStatsService userStatsService;
    private final GameEngine gameEngine;
    private final BotTurnRunner botTurnRunner;
    private final RandomProvider randomProvider;

    public MatchService(
            MatchRepository matchRepository,
            DeckRepository deckRepository,
            CatalogCardMapper cardMapper,
            UserStatsService userStatsService,
            GameEngine gameEngine,
            BotTurnRunner botTurnRunner,
            RandomProvider randomProvider
    ) {
        this.matchRepository = matchRepository;
        this.deckRepository = deckRepository;
        this.cardMapper = cardMapper;
        this.userStatsService = userStatsService;
        this.gameEngine = gameEngine;
        this.botTurnRunner = botTurnRunner;
        this.randomProvider = randomProvider;
    }

    @Transactional
    public MatchTurnResponse createMatch(UUID userId, CreateMatchRequest request) {
        DeckEntity playerDeck = deckRepository.findWithCardsById(request.playerDeckId())
                .orElseThrow(() -> new ResourceNotFoundException("Player deck not found: " + request.playerDeckId()));
        if (!playerDeck.isStarter() && !userId.equals(playerDeck.getOwnerId())) {
            throw new ForbiddenException("You do not have permission to use this deck");
        }
        if (!playerDeck.isValid()) {
            throw new IllegalArgumentException("El mazo seleccionado no cumple las reglas de 60 cartas");
        }

        DeckEntity cpuDeck = deckRepository.findWithCardsById(request.cpuDeckId())
                .orElseThrow(() -> new ResourceNotFoundException("CPU deck not found: " + request.cpuDeckId()));
        if (!cpuDeck.isCpuEligible() || !cpuDeck.isValid()) {
            throw new IllegalArgumentException("El mazo seleccionado para la CPU no es válido o elegible");
        }

        BotDifficulty difficulty = request.botDifficulty() != null ? request.botDifficulty() : BotDifficulty.BASIC;
        List<CardDefinition> playerCards = cardMapper.toDeckDefinitions(playerDeck);
        List<CardDefinition> cpuCards = cardMapper.toDeckDefinitions(cpuDeck);

        EngineStepResult init = gameEngine.initializeMatch(playerCards, cpuCards, randomProvider);
        List<GameEvent> allEvents = new ArrayList<>(init.events());

        EngineStepResult cpuSetup = botTurnRunner.runUntilHumanTurn(
                init.state(), PlayerId.CPU, resolveStrategy(difficulty)
        );
        GameState currentState = cpuSetup.state();
        allEvents.addAll(cpuSetup.events());

        Instant now = Instant.now();
        MatchEntity entity = new MatchEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setPlayerDeckId(playerDeck.getId());
        entity.setCpuDeckId(cpuDeck.getId());
        entity.setPlayerDeckName(playerDeck.getName());
        entity.setCpuDeckName(cpuDeck.getName());
        entity.setBotDifficulty(difficulty);
        entity.setStatus(currentState.phase() == TurnPhase.GAME_OVER ? MatchStatus.FINISHED : MatchStatus.IN_PROGRESS);
        entity.setWinner(currentState.winner() != null ? currentState.winner().name() : null);
        entity.setWinReason(currentState.winReason() != null ? currentState.winReason().name() : null);
        entity.setTurnNumber(currentState.turnNumber());
        entity.setStateJson(currentState);
        entity.setEventSeq(currentState.nextEventSequence());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        MatchEntity saved = matchRepository.saveAndFlush(entity);
        return toResponse(saved, allEvents);
    }

    @Transactional(readOnly = true)
    public Optional<ActiveMatchSummaryDTO> getActiveMatch(UUID userId) {
        return matchRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, MatchStatus.IN_PROGRESS)
                .map(match -> new ActiveMatchSummaryDTO(
                        match.getId(),
                        match.getPlayerDeckName(),
                        match.getCpuDeckName(),
                        match.getBotDifficulty(),
                        match.getTurnNumber(),
                        match.getStateJson().phase(),
                        match.getUpdatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public MatchTurnResponse getMatch(UUID matchId, UUID userId) {
        MatchEntity match = loadOwnedMatch(matchId, userId);
        return toResponse(match, List.of());
    }

    @Transactional
    public MatchTurnResponse submitAction(UUID matchId, UUID userId, MatchActionRequest request) {
        MatchEntity match = loadOwnedMatch(matchId, userId);
        ensureInProgress(match);

        GameAction action = request.toGameAction();
        return applyAndPersist(match, userId, action, true);
    }

    @Transactional
    public MatchTurnResponse concedeMatch(UUID matchId, UUID userId) {
        MatchEntity match = loadOwnedMatch(matchId, userId);
        ensureInProgress(match);
        return applyAndPersist(match, userId, new ConcedeAction(), false);
    }

    private MatchTurnResponse applyAndPersist(
            MatchEntity match,
            UUID userId,
            GameAction action,
            boolean runBotAfter
    ) {
        boolean wasInProgress = match.getStatus() == MatchStatus.IN_PROGRESS;
        EngineStepResult humanStep = gameEngine.applyAction(match.getStateJson(), PlayerId.PLAYER, action);

        List<GameEvent> allEvents = new ArrayList<>(humanStep.events());
        GameState currentState = humanStep.state();

        if (runBotAfter && currentState.phase() != TurnPhase.GAME_OVER) {
            EngineStepResult botStep = botTurnRunner.runUntilHumanTurn(
                    currentState,
                    PlayerId.CPU,
                    resolveStrategy(match.getBotDifficulty())
            );
            currentState = botStep.state();
            allEvents.addAll(botStep.events());
        }

        match.setStateJson(currentState);
        match.setTurnNumber(currentState.turnNumber());
        match.setEventSeq(currentState.nextEventSequence());
        match.setUpdatedAt(Instant.now());

        if (currentState.phase() == TurnPhase.GAME_OVER) {
            match.setStatus(MatchStatus.FINISHED);
            match.setWinner(currentState.winner() != null ? currentState.winner().name() : null);
            match.setWinReason(currentState.winReason() != null ? currentState.winReason().name() : null);
        }

        MatchEntity saved = matchRepository.saveAndFlush(match);

        if (wasInProgress && saved.getStatus() == MatchStatus.FINISHED) {
            boolean playerWon = currentState.winner() == PlayerId.PLAYER;
            userStatsService.recordMatchResult(userId, playerWon);
        }

        return toResponse(saved, allEvents);
    }

    private MatchEntity loadOwnedMatch(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));
        if (!userId.equals(match.getUserId())) {
            throw new ForbiddenException("You do not have permission to access this match");
        }
        return match;
    }

    private void ensureInProgress(MatchEntity match) {
        if (match.getStatus() == MatchStatus.FINISHED || match.getStateJson().phase() == TurnPhase.GAME_OVER) {
            throw new IllegalGameActionException("La partida ya finalizó");
        }
    }

    private BotStrategy resolveStrategy(BotDifficulty difficulty) {
        // Tactical strategy arrives in F4; delegate safely to BasicBotStrategy in F3.
        return new BasicBotStrategy();
    }

    private MatchTurnResponse toResponse(MatchEntity match, List<GameEvent> events) {
        return new MatchTurnResponse(
                match.getId(),
                match.getStatus(),
                match.getBotDifficulty(),
                match.getPlayerDeckName(),
                match.getCpuDeckName(),
                PlayerViewProjector.project(match.getStateJson(), PlayerId.PLAYER),
                events
        );
    }
}
