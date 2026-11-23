import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { DeckApiService } from '../../core/api/deck-api.service';
import { MatchApiService } from '../../core/api/match-api.service';
import { DeckSummary } from '../../core/models/deck.models';
import { BotDifficulty } from '../../core/models/match.models';
import { MatchStore } from './match.store';

@Component({
  selector: 'app-match-setup-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="setup-page">
      <header class="page-head">
        <div>
          <span class="eyebrow">Combate PvE · Base Set</span>
          <h1>Configurar Partida vs. CPU</h1>
          <p>Selecciona tu mazo válido de 60 cartas, el mazo del oponente y la dificultad del Bot.</p>
        </div>
        <a routerLink="/decks" class="secondary-link">Ir a Mis Mazos</a>
      </header>

      @if (error()) {
        <div class="error-banner" role="alert">{{ error() }}</div>
      }

      @if (loading()) {
        <div class="loading-box">Cargando mazos disponibles...</div>
      } @else {
        <div class="setup-grid">
          <section class="panel">
            <div class="panel-head">
              <span class="step-num mono-num">1</span>
              <div>
                <h2>Tu Mazo (60 cartas)</h2>
                <small>{{ playablePlayerDecks().length }} mazos válidos disponibles</small>
              </div>
            </div>

            @if (hasDraftDecks()) {
              <p class="hint-note">Tienes borradores incompletos ocultos aquí. Complétalos a 60 cartas en Mis Mazos.</p>
            }

            <div class="deck-options">
              @for (deck of playablePlayerDecks(); track deck.id) {
                <button
                  type="button"
                  class="deck-option"
                  [class.selected]="selectedPlayerDeckId() === deck.id"
                  (click)="selectedPlayerDeckId.set(deck.id)">
                  <div class="opt-top">
                    <strong>{{ deck.name }}</strong>
                    <span class="badge">{{ deck.starter ? 'Prearmado' : 'Propio' }}</span>
                  </div>
                  @if (deck.description) {
                    <p>{{ deck.description }}</p>
                  }
                </button>
              } @empty {
                <p class="empty-msg">No tienes mazos válidos de 60 cartas ni starters disponibles.</p>
              }
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <span class="step-num mono-num">2</span>
              <div>
                <h2>Mazo de la CPU</h2>
                <small>{{ cpuDecks().length }} mazos oficiales elegibles</small>
              </div>
            </div>

            <div class="deck-options">
              @for (deck of cpuDecks(); track deck.id) {
                <button
                  type="button"
                  class="deck-option"
                  [class.selected]="selectedCpuDeckId() === deck.id"
                  (click)="selectedCpuDeckId.set(deck.id)">
                  <div class="opt-top">
                    <strong>{{ deck.name }}</strong>
                    <span class="badge cpu">CPU</span>
                  </div>
                  @if (deck.description) {
                    <p>{{ deck.description }}</p>
                  }
                </button>
              }
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <span class="step-num mono-num">3</span>
              <div>
                <h2>Dificultad del Bot</h2>
                <small>Motor PvE en Java puro</small>
              </div>
            </div>

            <div class="deck-options">
              <button
                type="button"
                class="deck-option"
                [class.selected]="difficulty() === 'BASIC'"
                (click)="difficulty.set('BASIC')">
                <div class="opt-top">
                  <strong>Bot Básico</strong>
                  <span class="badge ready">Disponible</span>
                </div>
                <p>Baja Básicos a banca, adjunta energía al Activo y prioriza el ataque habilitado de mayor daño.</p>
              </button>

              <div class="deck-option disabled" aria-disabled="true">
                <div class="opt-top">
                  <strong>Bot Táctico</strong>
                  <span class="badge soon">Fase 4</span>
                </div>
                <p>Evoluciones, ventaja de tipo (×2 / -30), retirada defensiva y cartas Entrenador.</p>
              </div>
            </div>

            <div class="launch-area">
              <button
                type="button"
                class="launch-btn"
                [disabled]="!canStart() || starting()"
                (click)="startMatch()">
                {{ starting() ? 'Iniciando partida...' : 'Iniciar Combate →' }}
              </button>
            </div>
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    .setup-page {
      max-width: 1240px;
      margin: 0 auto;
      padding: var(--space-6) var(--space-5);
      display: flex;
      flex-direction: column;
      gap: var(--space-5);
    }
    .page-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: var(--space-4);
      flex-wrap: wrap;
    }
    .eyebrow {
      font-size: var(--text-xs);
      text-transform: uppercase;
      letter-spacing: 0.06em;
      font-weight: 700;
      color: var(--accent-primary);
    }
    .page-head h1 { font-size: var(--text-2xl); margin: 4px 0; }
    .page-head p { margin: 0; color: var(--text-secondary); font-size: var(--text-sm); }
    .secondary-link {
      padding: 9px 15px;
      border-radius: var(--radius-md);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      font-size: var(--text-sm);
      font-weight: 600;
    }
    .error-banner {
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius-md);
      background: rgba(239, 68, 68, 0.14);
      border: 1px solid var(--state-danger);
      color: #fecaca;
      font-size: var(--text-sm);
    }
    .loading-box {
      padding: var(--space-6);
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
    }
    .setup-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--space-4);
      align-items: start;
    }
    @media (max-width: 960px) {
      .setup-grid { grid-template-columns: 1fr; }
    }
    .panel {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-4);
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
    }
    .panel-head {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      padding-bottom: var(--space-2);
      border-bottom: 1px solid var(--border-subtle);
    }
    .step-num {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: rgba(255, 203, 5, 0.16);
      color: var(--accent-primary);
      font-weight: 800;
      font-size: var(--text-sm);
    }
    .panel-head h2 { font-size: var(--text-base); }
    .panel-head small { color: var(--text-muted); font-size: var(--text-xs); }
    .hint-note {
      margin: 0;
      font-size: var(--text-xs);
      color: var(--text-secondary);
      padding: var(--space-2) var(--space-3);
      border-radius: var(--radius-sm);
      background: rgba(59, 130, 246, 0.1);
    }
    .deck-options {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }
    .deck-option {
      width: 100%;
      text-align: left;
      padding: var(--space-3);
      border-radius: var(--radius-md);
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      color: var(--text-primary);
      cursor: pointer;
      transition: border-color 150ms ease, box-shadow 150ms ease;
    }
    .deck-option:hover:not(.disabled) { border-color: rgba(255, 203, 5, 0.5); }
    .deck-option.selected {
      border-color: var(--accent-primary);
      box-shadow: var(--shadow-active-glow);
    }
    .deck-option.disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
    .opt-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-2);
      margin-bottom: 4px;
    }
    .opt-top strong { font-size: var(--text-sm); }
    .deck-option p { margin: 0; font-size: var(--text-xs); color: var(--text-secondary); }
    .badge {
      font-size: 10px;
      font-weight: 700;
      padding: 2px 7px;
      border-radius: 999px;
      background: var(--bg-elevated);
      color: var(--text-secondary);
    }
    .badge.ready { background: rgba(34, 197, 94, 0.18); color: var(--state-success); }
    .badge.cpu { background: rgba(59, 130, 246, 0.18); color: var(--accent-secondary); }
    .badge.soon { background: rgba(245, 158, 11, 0.18); color: var(--state-warning); }
    .launch-area {
      margin-top: var(--space-2);
      padding-top: var(--space-3);
      border-top: 1px solid var(--border-subtle);
    }
    .launch-btn {
      width: 100%;
      padding: 12px 18px;
      border-radius: var(--radius-md);
      border: none;
      background: var(--accent-primary);
      color: #0b1020;
      font-weight: 800;
      font-size: var(--text-sm);
      cursor: pointer;
    }
    .launch-btn:disabled { opacity: 0.45; cursor: not-allowed; }
    .empty-msg { color: var(--text-muted); font-size: var(--text-xs); margin: 0; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatchSetupPageComponent implements OnInit {
  private readonly deckApi = inject(DeckApiService);
  private readonly matchApi = inject(MatchApiService);
  private readonly matchStore = inject(MatchStore);
  private readonly router = inject(Router);

  readonly loading = signal<boolean>(true);
  readonly starting = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly myDecks = signal<DeckSummary[]>([]);
  readonly starters = signal<DeckSummary[]>([]);

  readonly selectedPlayerDeckId = signal<string | null>(null);
  readonly selectedCpuDeckId = signal<string | null>(null);
  readonly difficulty = signal<BotDifficulty>('BASIC');

  readonly playablePlayerDecks = computed(() => [
    ...this.myDecks().filter((d) => d.valid),
    ...this.starters().filter((d) => d.valid)
  ]);

  readonly hasDraftDecks = computed(() => this.myDecks().some((d) => !d.valid));

  readonly cpuDecks = computed(() => this.starters().filter((d) => d.cpuEligible && d.valid));

  readonly canStart = computed(
    () => Boolean(this.selectedPlayerDeckId() && this.selectedCpuDeckId())
  );

  ngOnInit(): void {
    forkJoin({
      myDecks: this.deckApi.listMyDecks(),
      starters: this.deckApi.listStarters()
    })
      .pipe(
        catchError(() => {
          this.error.set('No se pudieron cargar los mazos para configurar la partida.');
          return of({ myDecks: [] as DeckSummary[], starters: [] as DeckSummary[] });
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe(({ myDecks, starters }) => {
        this.myDecks.set(myDecks);
        this.starters.set(starters);

        const playerChoices = [...myDecks.filter((d) => d.valid), ...starters.filter((d) => d.valid)];
        if (playerChoices.length > 0) {
          this.selectedPlayerDeckId.set(playerChoices[0].id);
        }

        const cpuChoices = starters.filter((d) => d.cpuEligible && d.valid);
        if (cpuChoices.length > 1) {
          this.selectedCpuDeckId.set(cpuChoices[1].id);
        } else if (cpuChoices.length === 1) {
          this.selectedCpuDeckId.set(cpuChoices[0].id);
        }
      });
  }

  startMatch(): void {
    const playerDeckId = this.selectedPlayerDeckId();
    const cpuDeckId = this.selectedCpuDeckId();
    if (!playerDeckId || !cpuDeckId || this.starting()) {
      return;
    }

    this.starting.set(true);
    this.error.set(null);

    this.matchApi
      .createMatch({
        playerDeckId,
        cpuDeckId,
        botDifficulty: this.difficulty()
      })
      .pipe(
        catchError(() => {
          this.error.set('No se pudo crear la partida. Verifica que los mazos elegidos sean válidos.');
          return of(null);
        }),
        finalize(() => this.starting.set(false))
      )
      .subscribe((response) => {
        if (response) {
          this.matchStore.primeCreatedMatch(response);
          void this.router.navigate(['/play', response.matchId]);
        }
      });
  }
}
