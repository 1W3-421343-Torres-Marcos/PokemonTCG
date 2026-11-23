import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PlayerId, WinReason } from '../../../core/models/match.models';

@Component({
  selector: 'app-match-result-modal',
  standalone: true,
  template: `
    @if (isOpen()) {
      <div class="modal-backdrop" role="dialog" aria-modal="true" aria-label="Resultado de la partida">
        <div class="result-card" [class.victory]="winner() === 'PLAYER'" [class.defeat]="winner() === 'CPU'">
          <span class="result-pill">{{ winner() === 'PLAYER' ? 'VICTORIA' : 'DERROTA' }}</span>
          <h2>{{ winner() === 'PLAYER' ? '¡Has ganado el combate!' : 'La CPU se llevó la victoria' }}</h2>
          <p class="reason">{{ reasonLabel() }}</p>

          <div class="summary-stats">
            <div>
              <strong class="mono-num">{{ turnCount() }}</strong>
              <span>Turnos</span>
            </div>
            <div>
              <strong class="mono-num">{{ 6 - myPrizesRemaining() }}/6</strong>
              <span>Tus Premios</span>
            </div>
            <div>
              <strong class="mono-num">{{ 6 - opponentPrizesRemaining() }}/6</strong>
              <span>Premios CPU</span>
            </div>
          </div>

          <div class="actions">
            <button type="button" class="btn-primary" (click)="rematch.emit()">Nueva Partida</button>
            <button type="button" class="btn-secondary" (click)="goHome.emit()">Volver al Inicio</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 80;
      background: rgba(5, 8, 18, 0.82);
      backdrop-filter: blur(8px);
      display: grid;
      place-items: center;
      padding: var(--space-4);
    }
    .result-card {
      width: min(440px, 100%);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-modal);
      padding: var(--space-6);
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: var(--space-4);
    }
    .result-card.victory { border-color: rgba(34, 197, 94, 0.55); }
    .result-card.defeat { border-color: rgba(239, 68, 68, 0.55); }
    .result-pill {
      align-self: center;
      padding: 4px 14px;
      border-radius: 999px;
      font-size: var(--text-xs);
      font-weight: 800;
      letter-spacing: 0.08em;
    }
    .victory .result-pill { background: rgba(34, 197, 94, 0.2); color: var(--state-success); }
    .defeat .result-pill { background: rgba(239, 68, 68, 0.2); color: var(--state-danger); }
    h2 { font-size: var(--text-2xl); }
    .reason { margin: 0; color: var(--text-secondary); font-size: var(--text-sm); }
    .summary-stats {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: var(--space-2);
      padding: var(--space-3);
      border-radius: var(--radius-md);
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
    }
    .summary-stats strong { display: block; font-size: var(--text-lg); color: var(--accent-primary); }
    .summary-stats span { font-size: var(--text-xs); color: var(--text-muted); }
    .actions { display: flex; gap: var(--space-3); justify-content: center; flex-wrap: wrap; }
    .btn-primary, .btn-secondary {
      padding: 10px 18px;
      border-radius: var(--radius-md);
      font-weight: 700;
      font-size: var(--text-sm);
      cursor: pointer;
    }
    .btn-primary { background: var(--accent-primary); color: #0b1020; border: none; }
    .btn-secondary { background: var(--bg-elevated); color: var(--text-primary); border: 1px solid var(--border-subtle); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatchResultModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly winner = input<PlayerId | null>(null);
  readonly reason = input<WinReason | null>(null);
  readonly turnCount = input<number>(0);
  readonly myPrizesRemaining = input<number>(6);
  readonly opponentPrizesRemaining = input<number>(6);

  readonly rematch = output<void>();
  readonly goHome = output<void>();

  protected readonly reasonLabel = computed(() => {
    switch (this.reason()) {
      case 'ALL_PRIZES_TAKEN':
        return 'Se reclamaron las 6 cartas de Premio.';
      case 'NO_POKEMON_LEFT':
        return 'El perdedor se quedó sin Pokémon en juego.';
      case 'DECK_OUT':
        return 'El perdedor no tuvo cartas para robar al inicio del turno.';
      case 'CONCEDED':
        return 'La partida finalizó por rendición.';
      default:
        return 'Fin de la partida.';
    }
  });
}
