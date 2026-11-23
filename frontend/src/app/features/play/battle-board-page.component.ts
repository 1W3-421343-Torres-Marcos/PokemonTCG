import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CardViewDTO, PokemonViewDTO } from '../../core/models/match.models';
import { CardDetailModalComponent } from '../../shared/ui/card-detail-modal/card-detail-modal.component';
import { DiscardPileModalComponent } from '../../shared/ui/discard-pile-modal/discard-pile-modal.component';
import { EnergyBadgeComponent } from '../../shared/ui/energy-badge/energy-badge.component';
import { EventLogFeedComponent } from '../../shared/ui/event-log-feed/event-log-feed.component';
import { HpBarComponent } from '../../shared/ui/hp-bar/hp-bar.component';
import { MatchResultModalComponent } from '../../shared/ui/match-result-modal/match-result-modal.component';
import { MatchStore, isBasicPokemon, isEnergyCard } from './match.store';

@Component({
  selector: 'app-battle-board-page',
  standalone: true,
  imports: [
    EnergyBadgeComponent,
    HpBarComponent,
    EventLogFeedComponent,
    DiscardPileModalComponent,
    CardDetailModalComponent,
    MatchResultModalComponent
  ],
  template: `
    <div class="battle-shell">
      @if (store.loading() && !store.playerView()) {
        <div class="center-state">Cargando tablero de combate...</div>
      } @else {
        @if (store.playerView(); as view) {
        <div class="arena">
          @if (store.error()) {
            <div class="error-toast" role="alert">{{ store.error() }}</div>
          }

          <!-- ZONA RIVAL (SUPERIOR) -->
          <section class="rival-zone" aria-label="Zona del rival CPU">
            <div class="zone-strip">
              <div class="hand-hidden" title="Mano oculta de la CPU">
                <div class="card-backs">
                  @for (dot of cardBackSlots(view.opponent.handCount); track $index) {
                    <span class="mini-back"></span>
                  }
                </div>
                <span class="counter-pill">Mano CPU: <strong class="mono-num">{{ view.opponent.handCount }}</strong></span>
              </div>

              <div class="deck-meta">
                <span><strong>CPU:</strong> {{ store.cpuDeckName() }}</span>
                <span class="counter-pill">Mazo: <strong class="mono-num">{{ view.opponent.deckCount }}</strong></span>
                <span class="counter-pill">Premios: <strong class="mono-num">{{ view.opponent.prizesRemaining }}/6</strong></span>
                <button type="button" class="pile-btn" (click)="store.openDiscard('CPU')">
                  Descarte ({{ view.opponent.discardPile.length }})
                </button>
              </div>
            </div>

            <div class="field-row rival-field">
              <div class="bench-slots">
                @for (slot of benchIndices; track slot) {
                  @if (view.opponent.bench[slot]; as mon) {
                    <div class="bench-card">
                      <button
                        type="button"
                        class="inspect-mini"
                        title="Inspeccionar"
                        (click)="inspectPokemon($event, mon)">🔍</button>
                      @if (mon.activeCard.imageLowUrl) {
                        <img [src]="mon.activeCard.imageLowUrl" [alt]="mon.activeCard.name" />
                      }
                      <div class="bench-meta">
                        <span class="poke-name">{{ mon.activeCard.name }}</span>
                        <app-hp-bar [currentHp]="mon.remainingHp" [maxHp]="mon.maxHp" />
                        <div class="mini-energies">
                          @for (e of mon.attachedEnergies; track e.instanceId) {
                            <app-energy-badge [type]="e.providedEnergyType || 'Colorless'" size="xs" />
                          }
                        </div>
                      </div>
                    </div>
                  } @else {
                    <div class="bench-empty">Banca {{ slot + 1 }}</div>
                  }
                }
              </div>

              <div class="active-slot">
                @if (view.opponent.active; as oppActive) {
                  <div class="active-card rival-active">
                    <button
                      type="button"
                      class="inspect-mini"
                      title="Inspeccionar Activo rival"
                      (click)="inspectPokemon($event, oppActive)">🔍</button>
                    <div class="active-art">
                      @if (oppActive.activeCard.imageLowUrl) {
                        <img [src]="oppActive.activeCard.imageLowUrl" [alt]="oppActive.activeCard.name" />
                      }
                    </div>
                    <div class="active-info">
                      <div class="active-title">
                        <strong>{{ oppActive.activeCard.name }}</strong>
                        @if (oppActive.damage > 0) {
                          <span class="dmg-badge mono-num">-{{ oppActive.damage }}</span>
                        }
                      </div>
                      <app-hp-bar [currentHp]="oppActive.remainingHp" [maxHp]="oppActive.maxHp" />
                      <div class="energy-row">
                        @for (e of oppActive.attachedEnergies; track e.instanceId) {
                          <app-energy-badge [type]="e.providedEnergyType || 'Colorless'" size="xs" />
                        } @empty {
                          <span class="muted-mini">Sin energías</span>
                        }
                      </div>
                    </div>
                  </div>
                } @else {
                  <div class="active-empty">Activo rival vacío</div>
                }
              </div>
            </div>
          </section>

          <!-- FRANJA CENTRAL / HUD DE COMBATE -->
          <section class="hud-bar" aria-label="Controles de turno y ataques">
            <div class="hud-status">
              <span
                class="turn-pill"
                [class.my-turn]="view.activePlayer === 'PLAYER'"
                [class.cpu-turn]="view.activePlayer === 'CPU'">
                @if (view.phase === 'SETUP') {
                  Preparación Inicial
                } @else if (store.isPromotionRequired()) {
                  Elige Nuevo Activo
                } @else if (view.activePlayer === 'PLAYER') {
                  Tu Turno · <span class="mono-num">T{{ view.turnNumber }}</span>
                } @else {
                  Turno CPU · <span class="mono-num">T{{ view.turnNumber }}</span>
                }
              </span>

              @if (store.activeAnimationEvent(); as activeEvt) {
                <span class="live-ticker">{{ activeEvt.message }}</span>
              } @else if (store.isSetupPhase()) {
                <span class="hint-text">Elige 1 Pokémon Básico Activo y hasta 5 en Banca.</span>
              } @else if (store.isPromotionRequired()) {
                <span class="hint-text urgent">Haz clic en un Pokémon de tu Banca para promoverlo al Activo.</span>
              } @else if (selectedHandHint()) {
                <span class="hint-text">{{ selectedHandHint() }}</span>
              } @else if (view.turnNumber === 1 && view.phase === 'MAIN_PHASE') {
                <span class="hint-text">En el Turno 1 el jugador inicial no puede atacar.</span>
              }
            </div>

            <div class="hud-actions">
              @if (store.isSetupPhase()) {
                <button
                  type="button"
                  class="btn-primary"
                  [disabled]="!store.canConfirmSetup() || store.isInteractiveBlocked()"
                  (click)="store.confirmSetup()">
                  Confirmar Setup ({{ store.setupActiveId() ? '1 Activo' : 'Sin Activo' }}, {{ store.setupBenchIds().length }} Banca)
                </button>
              } @else {
                @if (store.isMyMainTurn() && view.myActive; as active) {
                <div class="attack-group">
                  @for (atk of active.attacks; track atk.index) {
                    <button
                      type="button"
                      class="attack-btn"
                      [disabled]="store.isInteractiveBlocked() || !view.canAttackThisTurn || !atk.canPay"
                      [title]="!view.canAttackThisTurn ? 'No disponible en Turno 1' : (!atk.canPay ? 'Energía insuficiente' : atk.text || atk.name)"
                      (click)="store.useAttack(atk)">
                      <span class="atk-costs">
                        @for (c of atk.cost; track $index) {
                          <app-energy-badge [type]="c" size="xs" />
                        }
                      </span>
                      <span class="atk-name">{{ atk.name }}</span>
                      @if (atk.rawDamage) {
                        <span class="atk-dmg mono-num">{{ atk.rawDamage }}</span>
                      }
                    </button>
                  }
                </div>
                }
              }

              <div class="utility-btns">
                @if (selectedBasicPlayable()) {
                  <button
                    type="button"
                    class="btn-accent"
                    [disabled]="store.isInteractiveBlocked()"
                    (click)="store.playSelectedBasicToBench()">
                    Enviar a Banca
                  </button>
                }
                <button
                  type="button"
                  class="btn-ghost"
                  disabled
                  title="Retirada disponible en Fase 4">Retirar (F4)</button>
                <button
                  type="button"
                  class="btn-secondary"
                  [disabled]="!store.isMyMainTurn() || store.isInteractiveBlocked()"
                  (click)="store.endTurn()">Terminar Turno</button>
                @if (confirmingConcede()) {
                  <button
                    type="button"
                    class="btn-danger"
                    [disabled]="store.isInteractiveBlocked()"
                    (click)="confirmConcede()">¿Confirmar Rendirse?</button>
                  <button type="button" class="btn-ghost" (click)="confirmingConcede.set(false)">Cancelar</button>
                } @else {
                  <button
                    type="button"
                    class="btn-danger-outline"
                    [disabled]="view.phase === 'GAME_OVER' || store.isInteractiveBlocked()"
                    (click)="confirmingConcede.set(true)">Rendirse</button>
                }
              </div>
            </div>
          </section>

          <!-- ZONA JUGADOR (INFERIOR) -->
          <section class="player-zone" aria-label="Zona del jugador">
            <div class="field-row player-field">
              <div class="active-slot">
                @if (view.myActive; as myActive) {
                  <div
                    class="active-card my-active"
                    [class.energy-target]="energyTargetMode()"
                    (click)="onTargetPokemonClick(myActive.instanceId)">
                    <button
                      type="button"
                      class="inspect-mini"
                      title="Inspeccionar mi Activo"
                      (click)="inspectPokemon($event, myActive)">🔍</button>
                    <div class="active-art">
                      @if (myActive.activeCard.imageLowUrl) {
                        <img [src]="myActive.activeCard.imageLowUrl" [alt]="myActive.activeCard.name" />
                      }
                    </div>
                    <div class="active-info">
                      <div class="active-title">
                        <strong>{{ myActive.activeCard.name }}</strong>
                        @if (myActive.damage > 0) {
                          <span class="dmg-badge mono-num">-{{ myActive.damage }}</span>
                        }
                      </div>
                      <app-hp-bar [currentHp]="myActive.remainingHp" [maxHp]="myActive.maxHp" />
                      <div class="energy-row">
                        @for (e of myActive.attachedEnergies; track e.instanceId) {
                          <app-energy-badge [type]="e.providedEnergyType || 'Colorless'" size="xs" />
                        } @empty {
                          <span class="muted-mini">Sin energías</span>
                        }
                      </div>
                      @if (energyTargetMode()) {
                        <span class="target-tag">Clic para unir energía</span>
                      }
                    </div>
                  </div>
                } @else {
                  <div class="active-empty">
                    {{ store.isSetupPhase() && store.setupActiveId() ? 'Activo seleccionado en mano' : 'Sin Activo' }}
                  </div>
                }
              </div>

              <div class="bench-slots">
                @for (slot of benchIndices; track slot) {
                  @if (view.myBench[slot]; as mon) {
                    <div
                      class="bench-card interactive"
                      [class.energy-target]="energyTargetMode()"
                      [class.promote-target]="store.isPromotionRequired()"
                      (click)="onMyBenchCardClick(mon)">
                      <button
                        type="button"
                        class="inspect-mini"
                        title="Inspeccionar"
                        (click)="inspectPokemon($event, mon)">🔍</button>
                      @if (mon.activeCard.imageLowUrl) {
                        <img [src]="mon.activeCard.imageLowUrl" [alt]="mon.activeCard.name" />
                      }
                      <div class="bench-meta">
                        <span class="poke-name">{{ mon.activeCard.name }}</span>
                        <app-hp-bar [currentHp]="mon.remainingHp" [maxHp]="mon.maxHp" />
                        <div class="mini-energies">
                          @for (e of mon.attachedEnergies; track e.instanceId) {
                            <app-energy-badge [type]="e.providedEnergyType || 'Colorless'" size="xs" />
                          }
                        </div>
                      </div>
                    </div>
                  } @else {
                    <button
                      type="button"
                      class="bench-empty"
                      [class.bench-drop]="selectedBasicPlayable()"
                      [disabled]="!selectedBasicPlayable() || store.isInteractiveBlocked()"
                      (click)="store.playSelectedBasicToBench()">
                      {{ selectedBasicPlayable() ? '+ Bajar' : 'Banca ' + (slot + 1) }}
                    </button>
                  }
                }
              </div>
            </div>

            <div class="zone-strip player-strip">
              <div class="deck-meta">
                <span><strong>Tú:</strong> {{ store.playerDeckName() }}</span>
                <span class="counter-pill">Mazo: <strong class="mono-num">{{ view.myDeckCount }}</strong></span>
                <span class="counter-pill">Premios: <strong class="mono-num">{{ view.myPrizesRemaining }}/6</strong></span>
                <button type="button" class="pile-btn" (click)="store.openDiscard('PLAYER')">
                  Descarte ({{ view.myDiscard.length }})
                </button>
              </div>
              <span class="counter-pill">Mano: <strong class="mono-num">{{ view.myHand.length }}</strong></span>
            </div>

            <!-- MANO INTERACTIVA -->
            <div class="hand-strip" aria-label="Tu mano">
              @for (card of view.myHand; track card.instanceId) {
                <div
                  class="hand-card"
                  [class.playable]="store.playableHandIds().has(card.instanceId)"
                  [class.selected]="isHandCardSelected(card)"
                  [class.setup-active]="store.setupActiveId() === card.instanceId"
                  [class.setup-bench]="store.setupBenchIds().includes(card.instanceId)"
                  (click)="store.onHandCardClick(card)">
                  <button
                    type="button"
                    class="inspect-mini"
                    title="Inspeccionar carta"
                    (click)="inspectHandCard($event, card)">🔍</button>

                  @if (store.setupActiveId() === card.instanceId) {
                    <span class="role-badge active">ACTIVO</span>
                  } @else if (store.setupBenchIds().includes(card.instanceId)) {
                    <span class="role-badge bench">BANCA</span>
                  } @else if (!isPlayableCategory(card)) {
                    <span class="role-badge f4">F4</span>
                  }

                  @if (card.imageLowUrl) {
                    <img [src]="card.imageLowUrl" [alt]="card.name" />
                  } @else {
                    <div class="hand-fallback">{{ card.name }}</div>
                  }
                  <span class="hand-name">{{ card.name }}</span>
                </div>
              }
            </div>
          </section>
        </div>

        <!-- HISTORIAL DE COMBATE LATERAL -->
        <div class="side-log">
          <app-event-log-feed
            [events]="store.eventLog()"
            [activeEventSeq]="store.activeAnimationEvent()?.sequence ?? null"
            [speed]="store.playbackSpeed()"
            [animating]="store.isAnimating()"
            (speedChanged)="store.setPlaybackSpeed($event)"
            (skipRequested)="store.skipAnimation()" />
        </div>

        <app-discard-pile-modal
          [isOpen]="store.discardModalSide() !== null"
          [ownerLabel]="store.discardModalSide() === 'CPU' ? 'CPU' : 'Jugador'"
          [cards]="store.discardModalSide() === 'CPU' ? view.opponent.discardPile : view.myDiscard"
          (closed)="store.closeDiscard()"
          (inspectCard)="store.openInspectCard($event)" />

        <app-card-detail-modal
          [isOpen]="store.inspectCard() !== null"
          [card]="store.inspectCard()"
          [showFavorite]="false"
          (closed)="store.closeInspectCard()" />

        <app-match-result-modal
          [isOpen]="view.phase === 'GAME_OVER'"
          [winner]="view.winner"
          [reason]="view.winReason"
          [turnCount]="view.turnNumber"
          [myPrizesRemaining]="view.myPrizesRemaining"
          [opponentPrizesRemaining]="view.opponent.prizesRemaining"
          (rematch)="goRematch()"
          (goHome)="goHome()" />
        } @else {
          <div class="center-state">
            <p>{{ store.error() || 'No se encontró la partida.' }}</p>
            <button type="button" class="btn-primary" (click)="goHome()">Volver al Inicio</button>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .battle-shell {
      height: calc(100dvh - 58px);
      min-height: 640px;
      max-width: 1460px;
      margin: 0 auto;
      padding: var(--space-3);
      display: grid;
      grid-template-columns: minmax(0, 1fr) 290px;
      gap: var(--space-3);
      overflow: hidden;
    }
    @media (max-width: 1099px) {
      .battle-shell {
        height: auto;
        min-height: calc(100dvh - 58px);
        grid-template-columns: 1fr;
        overflow: visible;
      }
      .side-log { height: 280px; }
    }
    .center-state {
      grid-column: 1 / -1;
      display: grid;
      place-items: center;
      gap: var(--space-3);
      color: var(--text-secondary);
    }
    .arena {
      display: grid;
      grid-template-rows: minmax(0, 1fr) auto minmax(0, 1.35fr);
      gap: var(--space-2);
      min-height: 0;
      position: relative;
    }
    .error-toast {
      position: absolute;
      top: 8px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 40;
      padding: 6px 14px;
      border-radius: var(--radius-md);
      background: rgba(239, 68, 68, 0.92);
      color: #fff;
      font-size: var(--text-xs);
      font-weight: 600;
    }
    .rival-zone, .player-zone {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-2) var(--space-3);
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
      min-height: 0;
    }
    .zone-strip {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-2);
      flex-wrap: wrap;
      font-size: var(--text-xs);
    }
    .hand-hidden { display: flex; align-items: center; gap: var(--space-2); }
    .card-backs { display: flex; gap: 3px; }
    .mini-back {
      width: 12px;
      height: 17px;
      border-radius: 2px;
      background: linear-gradient(135deg, #1d4ed8, #0f172a);
      border: 1px solid var(--accent-primary);
    }
    .deck-meta { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
    .counter-pill {
      padding: 2px 8px;
      border-radius: 999px;
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
    }
    .counter-pill strong { color: var(--text-primary); }
    .pile-btn {
      padding: 3px 9px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-primary);
      font-size: var(--text-xs);
      cursor: pointer;
    }
    .pile-btn:hover { border-color: var(--accent-primary); }
    .field-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 260px;
      gap: var(--space-3);
      align-items: stretch;
      flex: 1;
      min-height: 0;
    }
    .player-field {
      grid-template-columns: 260px minmax(0, 1fr);
    }
    .bench-slots {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: var(--space-2);
      min-height: 0;
    }
    .bench-empty {
      border: 1px dashed var(--border-subtle);
      border-radius: var(--radius-md);
      background: rgba(11, 16, 32, 0.45);
      color: var(--text-muted);
      font-size: 11px;
      display: grid;
      place-items: center;
      padding: var(--space-1);
    }
    button.bench-empty { cursor: default; }
    button.bench-empty.bench-drop {
      border-color: var(--state-success);
      color: var(--state-success);
      background: rgba(34, 197, 94, 0.12);
      cursor: pointer;
      font-weight: 700;
    }
    .bench-card {
      position: relative;
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 5px;
      display: flex;
      gap: 5px;
      align-items: center;
      min-width: 0;
    }
    .bench-card.interactive { cursor: pointer; }
    .bench-card img {
      width: 36px;
      aspect-ratio: 5 / 7;
      object-fit: cover;
      border-radius: 4px;
      flex-shrink: 0;
    }
    .bench-meta {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .poke-name {
      font-size: 11px;
      font-weight: 700;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .mini-energies { display: flex; flex-wrap: wrap; gap: 2px; }
    .active-slot { min-height: 0; display: flex; }
    .active-card {
      width: 100%;
      position: relative;
      background: var(--bg-tabletop);
      border: 1px solid rgba(255, 255, 255, 0.16);
      border-radius: var(--radius-md);
      padding: var(--space-2);
      display: flex;
      gap: var(--space-2);
      align-items: center;
    }
    .active-card.my-active { border-color: rgba(255, 203, 5, 0.35); cursor: pointer; }
    .active-card.rival-active { border-color: rgba(59, 130, 246, 0.4); }
    .energy-target, .promote-target {
      border-color: var(--accent-primary) !important;
      box-shadow: var(--shadow-active-glow);
    }
    .active-art img {
      width: 68px;
      aspect-ratio: 5 / 7;
      object-fit: cover;
      border-radius: 5px;
      display: block;
    }
    .active-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 5px; }
    .active-title { display: flex; justify-content: space-between; align-items: center; gap: 4px; padding-right: 18px; }
    .active-title strong { font-size: var(--text-sm); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .dmg-badge {
      padding: 1px 6px;
      border-radius: 999px;
      background: rgba(239, 68, 68, 0.25);
      color: #fca5a5;
      font-size: 11px;
      font-weight: 700;
    }
    .energy-row { display: flex; flex-wrap: wrap; gap: 3px; }
    .muted-mini { font-size: 11px; color: var(--text-muted); }
    .target-tag { font-size: 10px; font-weight: 700; color: var(--accent-primary); }
    .active-empty {
      width: 100%;
      border: 1px dashed var(--border-subtle);
      border-radius: var(--radius-md);
      display: grid;
      place-items: center;
      font-size: var(--text-xs);
      color: var(--text-muted);
    }
    .inspect-mini {
      position: absolute;
      top: 4px;
      right: 4px;
      z-index: 3;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 1px solid var(--border-subtle);
      background: rgba(11, 16, 32, 0.85);
      color: var(--text-primary);
      font-size: 10px;
      cursor: pointer;
      display: grid;
      place-items: center;
      padding: 0;
    }
    .hud-bar {
      background: var(--bg-elevated);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-2) var(--space-3);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-3);
      flex-wrap: wrap;
    }
    .hud-status { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
    .turn-pill {
      padding: 4px 10px;
      border-radius: 999px;
      font-size: var(--text-xs);
      font-weight: 800;
      background: var(--bg-surface);
    }
    .turn-pill.my-turn { background: rgba(255, 203, 5, 0.2); color: var(--accent-primary); }
    .turn-pill.cpu-turn { background: rgba(59, 130, 246, 0.22); color: var(--accent-secondary); }
    .live-ticker {
      font-size: var(--text-xs);
      font-weight: 600;
      color: var(--accent-primary);
    }
    .hint-text { font-size: var(--text-xs); color: var(--text-secondary); }
    .hint-text.urgent { color: var(--accent-primary); font-weight: 700; }
    .hud-actions { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
    .attack-group { display: flex; gap: var(--space-2); flex-wrap: wrap; }
    .attack-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 11px;
      border-radius: var(--radius-md);
      border: 1px solid rgba(255, 203, 5, 0.5);
      background: rgba(255, 203, 5, 0.14);
      color: var(--text-primary);
      font-size: var(--text-xs);
      font-weight: 700;
      cursor: pointer;
    }
    .attack-btn:hover:not(:disabled) { background: rgba(255, 203, 5, 0.24); }
    .attack-btn:disabled {
      opacity: 0.42;
      border-color: var(--border-subtle);
      background: var(--bg-surface);
      cursor: not-allowed;
    }
    .atk-costs { display: inline-flex; gap: 2px; }
    .atk-dmg { color: var(--accent-primary); }
    .utility-btns { display: flex; gap: 6px; flex-wrap: wrap; }
    .btn-primary, .btn-secondary, .btn-accent, .btn-ghost, .btn-danger, .btn-danger-outline {
      padding: 6px 12px;
      border-radius: var(--radius-sm);
      font-size: var(--text-xs);
      font-weight: 700;
      cursor: pointer;
    }
    .btn-primary { background: var(--accent-primary); color: #0b1020; border: none; }
    .btn-accent { background: rgba(34, 197, 94, 0.22); color: var(--state-success); border: 1px solid var(--state-success); }
    .btn-secondary { background: var(--accent-secondary); color: #fff; border: none; }
    .btn-ghost { background: var(--bg-surface); color: var(--text-muted); border: 1px solid var(--border-subtle); }
    .btn-danger { background: var(--state-danger); color: #fff; border: none; }
    .btn-danger-outline { background: transparent; color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.5); }
    button:disabled { opacity: 0.45; cursor: not-allowed; }

    .hand-strip {
      display: flex;
      gap: var(--space-2);
      overflow-x: auto;
      padding: 4px 2px;
      min-height: 126px;
      align-items: stretch;
    }
    .hand-card {
      position: relative;
      flex: 0 0 84px;
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-card);
      padding: 4px;
      display: flex;
      flex-direction: column;
      gap: 3px;
      cursor: pointer;
      transition: transform 150ms ease, border-color 150ms ease, box-shadow 150ms ease;
    }
    .hand-card.playable { border-color: rgba(34, 197, 94, 0.65); }
    .hand-card:hover { transform: translateY(-4px); }
    .hand-card.selected { box-shadow: var(--shadow-active-glow); transform: translateY(-6px); }
    .hand-card.setup-active { border-color: var(--accent-primary); box-shadow: var(--shadow-active-glow); }
    .hand-card.setup-bench { border-color: var(--accent-secondary); }
    .hand-card img, .hand-fallback {
      width: 100%;
      aspect-ratio: 5 / 7;
      object-fit: cover;
      border-radius: 4px;
      background: var(--bg-elevated);
    }
    .hand-fallback { display: grid; place-items: center; font-size: 10px; text-align: center; padding: 4px; }
    .hand-name {
      font-size: 10px;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .role-badge {
      position: absolute;
      top: 4px;
      left: 4px;
      z-index: 3;
      padding: 1px 5px;
      border-radius: 4px;
      font-size: 9px;
      font-weight: 800;
    }
    .role-badge.active { background: var(--accent-primary); color: #0b1020; }
    .role-badge.bench { background: var(--accent-secondary); color: #fff; }
    .role-badge.f4 { background: rgba(11, 16, 32, 0.8); color: var(--text-muted); border: 1px solid var(--border-subtle); }
    .side-log { min-height: 0; height: 100%; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BattleBoardPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly store = inject(MatchStore);

  protected readonly benchIndices = [0, 1, 2, 3, 4];
  protected readonly confirmingConcede = signal<boolean>(false);

  protected readonly selectedBasicPlayable = computed(() => {
    const card = this.store.selectedHandCard();
    const view = this.store.playerView();
    return Boolean(
      card &&
        view &&
        this.store.isMyMainTurn() &&
        isBasicPokemon(card) &&
        view.myBench.length < 5
    );
  });

  protected readonly energyTargetMode = computed(() => {
    const card = this.store.selectedHandCard();
    const view = this.store.playerView();
    return Boolean(
      card &&
        view &&
        this.store.isMyMainTurn() &&
        isEnergyCard(card) &&
        !view.energyAttachedThisTurn
    );
  });

  protected readonly selectedHandHint = computed(() => {
    const card = this.store.selectedHandCard();
    if (!card || !this.store.isMyMainTurn()) {
      return null;
    }
    if (isBasicPokemon(card)) {
      return (this.store.playerView()?.myBench.length ?? 0) < 5
        ? `Seleccionado ${card.name}: haz clic en un slot libre de Banca o "Enviar a Banca".`
        : 'Tu Banca ya tiene el máximo de 5 Pokémon.';
    }
    if (isEnergyCard(card)) {
      return !this.store.playerView()?.energyAttachedThisTurn
        ? `Seleccionada ${card.name}: haz clic en tu Activo o un Pokémon de Banca para unirla.`
        : 'Ya adjuntaste una energía este turno.';
    }
    return `${card.name}: Entrenadores y Evoluciones estarán disponibles en la Fase 4.`;
  });

  ngOnInit(): void {
    const matchId = this.route.snapshot.paramMap.get('matchId');
    if (matchId) {
      this.store.loadMatch(matchId);
    }
  }

  ngOnDestroy(): void {
    this.store.clearBoardState();
  }

  protected cardBackSlots(count: number): number[] {
    const capped = Math.max(0, Math.min(count, 10));
    return Array.from({ length: capped }, (_, idx) => idx);
  }

  protected isHandCardSelected(card: CardViewDTO): boolean {
    return this.store.selectedHandInstanceId() === card.instanceId;
  }

  protected isPlayableCategory(card: CardViewDTO): boolean {
    return isBasicPokemon(card) || isEnergyCard(card);
  }

  protected onTargetPokemonClick(pokemonInstanceId: string): void {
    if (this.energyTargetMode()) {
      this.store.attachSelectedEnergyTo(pokemonInstanceId);
    }
  }

  protected onMyBenchCardClick(mon: PokemonViewDTO): void {
    if (this.store.isPromotionRequired()) {
      this.store.promoteBenchPokemon(mon.instanceId);
      return;
    }
    if (this.energyTargetMode()) {
      this.store.attachSelectedEnergyTo(mon.instanceId);
    }
  }

  protected inspectPokemon(event: Event, mon: PokemonViewDTO): void {
    event.stopPropagation();
    this.store.openInspectCard(mon.activeCard);
  }

  protected inspectHandCard(event: Event, card: CardViewDTO): void {
    event.stopPropagation();
    this.store.openInspectCard(card);
  }

  protected confirmConcede(): void {
    this.confirmingConcede.set(false);
    this.store.concede();
  }

  protected goRematch(): void {
    void this.router.navigate(['/play/setup']);
  }

  protected goHome(): void {
    void this.router.navigate(['/']);
  }
}
