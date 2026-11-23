import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of, tap } from 'rxjs';
import { MatchApiService } from '../../core/api/match-api.service';
import { AuthStore } from '../../core/auth/auth.store';
import { CardDetail } from '../../core/models/catalog.models';
import {
  AttackViewDTO,
  BotDifficulty,
  CardViewDTO,
  GameEvent,
  MatchActionRequest,
  MatchStatus,
  MatchTurnResponse,
  PlayerViewDTO,
  toCardDetailFromView
} from '../../core/models/match.models';
import { EventAnimatorService } from './event-animator.service';

@Injectable({ providedIn: 'root' })
export class MatchStore {
  private readonly api = inject(MatchApiService);
  private readonly animator = inject(EventAnimatorService);
  private readonly authStore = inject(AuthStore);

  readonly matchId = signal<string | null>(null);
  readonly status = signal<MatchStatus>('IN_PROGRESS');
  readonly botDifficulty = signal<BotDifficulty>('BASIC');
  readonly playerDeckName = signal<string>('');
  readonly cpuDeckName = signal<string>('');
  readonly playerView = signal<PlayerViewDTO | null>(null);
  readonly eventLog = signal<GameEvent[]>([]);
  readonly loading = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly selectedHandInstanceId = signal<string | null>(null);
  readonly setupActiveId = signal<string | null>(null);
  readonly setupBenchIds = signal<string[]>([]);
  readonly inspectCard = signal<CardDetail | null>(null);
  readonly discardModalSide = signal<'PLAYER' | 'CPU' | null>(null);

  readonly isAnimating = this.animator.isAnimating;
  readonly playbackSpeed = this.animator.playbackSpeed;
  readonly activeAnimationEvent = this.animator.activeEvent;

  readonly isInteractiveBlocked = computed(
    () => this.loading() || this.submitting() || this.isAnimating()
  );

  readonly isSetupPhase = computed(() => {
    const view = this.playerView();
    return Boolean(view && view.phase === 'SETUP' && !view.mySetupReady);
  });

  readonly isPromotionRequired = computed(
    () => this.playerView()?.awaitingPromotionPlayer === 'PLAYER'
  );

  readonly isMyMainTurn = computed(() => {
    const view = this.playerView();
    return Boolean(
      view &&
        view.phase === 'MAIN_PHASE' &&
        view.activePlayer === 'PLAYER' &&
        !view.awaitingPromotionPlayer
    );
  });

  readonly selectedHandCard = computed<CardViewDTO | null>(() => {
    const id = this.selectedHandInstanceId();
    const view = this.playerView();
    if (!id || !view) {
      return null;
    }
    return view.myHand.find((card) => card.instanceId === id) ?? null;
  });

  readonly playableHandIds = computed<Set<string>>(() => {
    const view = this.playerView();
    const ids = new Set<string>();
    if (!view || this.isInteractiveBlocked()) {
      return ids;
    }

    if (view.phase === 'SETUP' && !view.mySetupReady) {
      for (const card of view.myHand) {
        if (isBasicPokemon(card)) {
          ids.add(card.instanceId);
        }
      }
      return ids;
    }

    if (view.phase === 'MAIN_PHASE' && view.activePlayer === 'PLAYER' && !view.awaitingPromotionPlayer) {
      const canBench = view.myBench.length < 5;
      const canEnergy = !view.energyAttachedThisTurn;
      for (const card of view.myHand) {
        if ((canBench && isBasicPokemon(card)) || (canEnergy && isEnergyCard(card))) {
          ids.add(card.instanceId);
        }
      }
    }
    return ids;
  });

  readonly canConfirmSetup = computed(() => Boolean(this.isSetupPhase() && this.setupActiveId()));

  primeCreatedMatch(response: MatchTurnResponse): void {
    this.animator.cancel();
    this.resetSelectionState();
    this.eventLog.set([]);
    this.applyTurnResponse(response, true);
  }

  loadMatch(id: string): void {
    if (this.matchId() === id && this.playerView() !== null) {
      return;
    }

    this.animator.cancel();
    this.resetSelectionState();
    this.loading.set(true);
    this.error.set(null);

    this.api
      .getMatch(id)
      .pipe(
        tap((res) => {
          this.eventLog.set([]);
          this.applyTurnResponse(res, false);
        }),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractApiError(err, 'No se pudo cargar la partida solicitada.'));
          return of(null);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe();
  }

  onHandCardClick(card: CardViewDTO): void {
    if (this.isInteractiveBlocked()) {
      return;
    }
    this.error.set(null);

    if (this.isSetupPhase()) {
      if (!isBasicPokemon(card)) {
        this.error.set('En la preparación inicial solo puedes elegir Pokémon Básicos.');
        return;
      }
      const id = card.instanceId;
      if (this.setupActiveId() === id) {
        this.setupActiveId.set(null);
        return;
      }
      if (!this.setupActiveId()) {
        this.setupActiveId.set(id);
        this.setupBenchIds.update((list) => list.filter((x) => x !== id));
        return;
      }
      const currentBench = this.setupBenchIds();
      if (currentBench.includes(id)) {
        this.setupBenchIds.set(currentBench.filter((x) => x !== id));
        return;
      }
      if (currentBench.length >= 5) {
        this.error.set('Puedes seleccionar un máximo de 5 Pokémon en Banca.');
        return;
      }
      this.setupBenchIds.set([...currentBench, id]);
      return;
    }

    if (!this.isMyMainTurn()) {
      return;
    }

    if (this.selectedHandInstanceId() === card.instanceId) {
      this.selectedHandInstanceId.set(null);
      return;
    }
    this.selectedHandInstanceId.set(card.instanceId);
  }

  confirmSetup(): void {
    const activeId = this.setupActiveId();
    if (!this.isSetupPhase() || !activeId || this.isInteractiveBlocked()) {
      return;
    }
    this.dispatchAction({
      type: 'SELECT_SETUP',
      activeInstanceId: activeId,
      benchInstanceIds: this.setupBenchIds()
    });
  }

  playSelectedBasicToBench(): void {
    const card = this.selectedHandCard();
    if (!card || !isBasicPokemon(card) || !this.isMyMainTurn() || this.isInteractiveBlocked()) {
      return;
    }
    this.dispatchAction({
      type: 'PLAY_BASIC',
      handInstanceId: card.instanceId
    });
  }

  attachSelectedEnergyTo(targetPokemonInstanceId: string): void {
    const card = this.selectedHandCard();
    if (!card || !isEnergyCard(card) || !this.isMyMainTurn() || this.isInteractiveBlocked()) {
      return;
    }
    this.dispatchAction({
      type: 'ATTACH_ENERGY',
      energyInstanceId: card.instanceId,
      targetPokemonInstanceId
    });
  }

  promoteBenchPokemon(benchInstanceId: string): void {
    if (!this.isPromotionRequired() || this.isInteractiveBlocked()) {
      return;
    }
    this.dispatchAction({
      type: 'PROMOTE_ACTIVE',
      benchInstanceId
    });
  }

  useAttack(attack: AttackViewDTO): void {
    const view = this.playerView();
    if (
      !view ||
      !this.isMyMainTurn() ||
      !view.canAttackThisTurn ||
      !attack.canPay ||
      this.isInteractiveBlocked()
    ) {
      return;
    }
    this.dispatchAction({
      type: 'ATTACK',
      attackIndex: attack.index
    });
  }

  endTurn(): void {
    if (!this.isMyMainTurn() || this.isInteractiveBlocked()) {
      return;
    }
    this.dispatchAction({ type: 'END_TURN' });
  }

  concede(): void {
    const id = this.matchId();
    if (!id || this.playerView()?.phase === 'GAME_OVER' || this.isInteractiveBlocked()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.api
      .concedeMatch(id)
      .pipe(
        tap((res) => this.applyTurnResponse(res, true)),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractApiError(err, 'No se pudo procesar la rendición.'));
          return of(null);
        }),
        finalize(() => this.submitting.set(false))
      )
      .subscribe();
  }

  setPlaybackSpeed(speed: 1 | 2): void {
    this.animator.setSpeed(speed);
  }

  skipAnimation(): void {
    this.animator.skip();
  }

  openInspectCard(card: CardViewDTO): void {
    this.inspectCard.set(toCardDetailFromView(card));
  }

  closeInspectCard(): void {
    this.inspectCard.set(null);
  }

  openDiscard(side: 'PLAYER' | 'CPU'): void {
    this.discardModalSide.set(side);
  }

  closeDiscard(): void {
    this.discardModalSide.set(null);
  }

  clearBoardState(): void {
    this.animator.cancel();
    this.matchId.set(null);
    this.playerView.set(null);
    this.eventLog.set([]);
    this.resetSelectionState();
  }

  private dispatchAction(action: MatchActionRequest): void {
    const id = this.matchId();
    if (!id) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.selectedHandInstanceId.set(null);

    this.api
      .sendAction(id, action)
      .pipe(
        tap((res) => this.applyTurnResponse(res, true)),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractApiError(err, 'Acción inválida en este estado del combate.'));
          return of(null);
        }),
        finalize(() => this.submitting.set(false))
      )
      .subscribe();
  }

  private applyTurnResponse(res: MatchTurnResponse, animateEvents: boolean): void {
    this.matchId.set(res.matchId);
    this.status.set(res.status);
    this.botDifficulty.set(res.botDifficulty);
    this.playerDeckName.set(res.playerDeckName);
    this.cpuDeckName.set(res.cpuDeckName);

    const applyFinalView = () => {
      this.playerView.set(res.playerView);
      this.resetSelectionState();
      if (res.playerView.phase === 'GAME_OVER') {
        this.authStore.hydrateProfile();
      }
    };

    if (!animateEvents || res.events.length === 0) {
      applyFinalView();
      return;
    }

    // Show immediate baseline view if we don't have one yet (e.g., first load right after createMatch),
    // while stepping events chronologically and applying authoritative final PlayerViewDTO on completion.
    if (this.playerView() === null) {
      this.playerView.set(res.playerView);
    }

    this.animator.play(
      res.events,
      (evt) => {
        this.eventLog.update((existing) => [...existing, evt]);
      },
      applyFinalView
    );
  }

  private resetSelectionState(): void {
    this.selectedHandInstanceId.set(null);
    this.setupActiveId.set(null);
    this.setupBenchIds.set([]);
  }
}

export function isBasicPokemon(card: CardViewDTO): boolean {
  return card.category.toLowerCase() === 'pokemon' && (card.stage ?? '').toLowerCase() === 'basic';
}

export function isEnergyCard(card: CardViewDTO): boolean {
  return card.category.toLowerCase() === 'energy';
}

function extractApiError(err: HttpErrorResponse, fallback: string): string {
  const apiMessage = err.error?.message;
  return typeof apiMessage === 'string' && apiMessage.trim().length > 0 ? apiMessage : fallback;
}
