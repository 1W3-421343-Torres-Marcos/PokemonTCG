import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CardViewDTO, GameEvent, MatchTurnResponse, PlayerViewDTO } from '../../core/models/match.models';
import { MatchStore } from './match.store';

describe('MatchStore & EventAnimatorService', () => {
  let store: MatchStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }])
      ]
    });

    store = TestBed.inject(MatchStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    store.clearBoardState();
    httpMock.verify();
  });

  it('plays GameEvent entries sequentially, locks interaction during playback, and applies final PlayerViewDTO on skip', () => {
    const basicCard = makeCard('p-1', 'Charmander', 'Pokemon', 'Basic');
    const energyCard = makeCard('p-2', 'Fire Energy', 'Energy', 'Basic');

    const initialView = makeView('SETUP', false, [basicCard, energyCard], []);
    store.primeCreatedMatch({
      matchId: 'm-1',
      status: 'IN_PROGRESS',
      botDifficulty: 'BASIC',
      playerDeckName: 'Brushfire',
      cpuDeckName: 'Blackout',
      playerView: initialView,
      events: []
    });

    // Rejects selecting non-Basic during SETUP
    store.onHandCardClick(energyCard);
    expect(store.setupActiveId()).toBeNull();
    expect(store.error()).toContain('Básicos');

    // Select Charmander as Active in SETUP and confirm
    store.onHandCardClick(basicCard);
    expect(store.setupActiveId()).toBe('p-1');
    store.confirmSetup();

    const req = httpMock.expectOne('/api/matches/m-1/actions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.type).toBe('SELECT_SETUP');

    const events: GameEvent[] = [
      { sequence: 1, type: 'SETUP_COMPLETED', actor: 'PLAYER', message: 'Jugador listo', metadata: {} },
      { sequence: 2, type: 'TURN_STARTED', actor: 'PLAYER', message: 'Turno 1', metadata: {} },
      { sequence: 3, type: 'CARD_DRAWN', actor: 'PLAYER', message: 'Roba 1 carta', metadata: {} }
    ];
    const finalMainView = makeView('MAIN_PHASE', true, [energyCard], []);
    const turnResponse: MatchTurnResponse = {
      matchId: 'm-1',
      status: 'IN_PROGRESS',
      botDifficulty: 'BASIC',
      playerDeckName: 'Brushfire',
      cpuDeckName: 'Blackout',
      playerView: finalMainView,
      events
    };

    req.flush(turnResponse);

    // During sequential playback: isAnimating is true, controls blocked, first event added,
    // and authoritative final view is NOT prematurely mutated until playback finishes or skips.
    expect(store.isAnimating()).toBeTrue();
    expect(store.isInteractiveBlocked()).toBeTrue();
    expect(store.eventLog().length).toBe(1);
    expect(store.playerView()?.phase).toBe('SETUP');

    // Attempt to end turn while animating is ignored cleanly
    store.endTurn();
    httpMock.expectNone('/api/matches/m-1/actions');

    // Skip immediately flushes remaining events to log and applies final authoritative PlayerViewDTO
    store.skipAnimation();
    expect(store.isAnimating()).toBeFalse();
    expect(store.isInteractiveBlocked()).toBeFalse();
    expect(store.eventLog().length).toBe(3);
    expect(store.playerView()?.phase).toBe('MAIN_PHASE');
    expect(store.playableHandIds().has('p-2')).toBeTrue();
  });

  it('prevents attack dispatch unless both attack.canPay and canAttackThisTurn are true', () => {
    const attackDisabled: CardViewDTO['attacks'][number] = {
      index: 0,
      name: 'Scratch',
      cost: ['Colorless'],
      baseDamage: 10,
      rawDamage: '10',
      text: null,
      canPay: false
    };
    const attackEnabled = { ...attackDisabled, canPay: true };

    const viewTurn1: PlayerViewDTO = {
      ...makeView('MAIN_PHASE', true, [], []),
      turnNumber: 1,
      canAttackThisTurn: false
    };
    store.primeCreatedMatch({
      matchId: 'm-2',
      status: 'IN_PROGRESS',
      botDifficulty: 'BASIC',
      playerDeckName: 'Brushfire',
      cpuDeckName: 'Blackout',
      playerView: viewTurn1,
      events: []
    });

    // Neither unpaid nor Turn 1 locked attacks dispatch HTTP actions
    store.useAttack(attackDisabled);
    store.useAttack(attackEnabled);
    expect(store.submitting()).toBeFalse();
    httpMock.expectNone('/api/matches/m-2/actions');

    // On Turn 2 (canAttackThisTurn = true) + canPay = true, ATTACK is dispatched
    const viewTurn2 = makeView('MAIN_PHASE', true, [], []);
    store.primeCreatedMatch({
      matchId: 'm-2',
      status: 'IN_PROGRESS',
      botDifficulty: 'BASIC',
      playerDeckName: 'Brushfire',
      cpuDeckName: 'Blackout',
      playerView: viewTurn2,
      events: []
    });
    store.useAttack(attackEnabled);
    const req = httpMock.expectOne('/api/matches/m-2/actions');
    expect(req.request.body.type).toBe('ATTACK');
    expect(req.request.body.attackIndex).toBe(0);
    req.flush({
      matchId: 'm-2',
      status: 'IN_PROGRESS',
      botDifficulty: 'BASIC',
      playerDeckName: 'Brushfire',
      cpuDeckName: 'Blackout',
      playerView: viewTurn2,
      events: []
    });
  });

  function makeCard(instanceId: string, name: string, category: string, stage: string): CardViewDTO {
    return {
      instanceId,
      cardId: 'base1-46',
      name,
      category,
      stage,
      evolveFrom: null,
      hp: category === 'Pokemon' ? 50 : null,
      types: category === 'Pokemon' ? ['Fire'] : [],
      attacks: [],
      weaknesses: [],
      resistances: [],
      retreatCost: 1,
      trainerType: null,
      energyType: category === 'Energy' ? 'Basic' : null,
      providedEnergyType: category === 'Energy' ? 'Fire' : null,
      providedEnergyCount: category === 'Energy' ? 1 : 0,
      imageLowUrl: null,
      imageHighUrl: null
    };
  }

  function makeView(
    phase: PlayerViewDTO['phase'],
    mySetupReady: boolean,
    hand: CardViewDTO[],
    bench: PlayerViewDTO['myBench']
  ): PlayerViewDTO {
    return {
      viewer: 'PLAYER',
      turnNumber: phase === 'SETUP' ? 0 : 2,
      activePlayer: 'PLAYER',
      startingPlayer: 'PLAYER',
      phase,
      awaitingPromotionPlayer: null,
      energyAttachedThisTurn: false,
      retreatedThisTurn: false,
      canAttackThisTurn: phase === 'MAIN_PHASE',
      mySetupReady,
      opponentSetupReady: true,
      winner: null,
      winReason: null,
      myHand: hand,
      myActive: null,
      myBench: bench,
      myDiscard: [],
      myDeckCount: 47,
      myPrizesRemaining: 6,
      opponent: {
        playerId: 'CPU',
        handCount: 6,
        deckCount: 47,
        prizesRemaining: 6,
        active: null,
        bench: [],
        discardPile: []
      }
    };
  }
});
