import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { CardViewDTO } from '../../../core/models/match.models';

@Component({
  selector: 'app-discard-pile-modal',
  standalone: true,
  template: `
    @if (isOpen()) {
      <div class="modal-backdrop" (click)="closed.emit()" role="dialog" aria-modal="true" aria-label="Pila de descarte">
        <div class="modal-panel" (click)="$event.stopPropagation()">
          <header class="modal-head">
            <div>
              <h3>Descarte · {{ ownerLabel() }}</h3>
              <span class="mono-num">{{ cards().length }} cartas</span>
            </div>
            <button type="button" class="close-btn" (click)="closed.emit()" aria-label="Cerrar">✕</button>
          </header>

          @if (cards().length === 0) {
            <p class="empty-msg">La pila de descarte está vacía.</p>
          } @else {
            <div class="discard-grid">
              @for (card of cards(); track card.instanceId) {
                <button type="button" class="discard-card" (click)="inspectCard.emit(card)">
                  @if (card.imageLowUrl) {
                    <img [src]="card.imageLowUrl" [alt]="card.name" loading="lazy" />
                  } @else {
                    <div class="card-fallback">{{ card.name }}</div>
                  }
                  <span>{{ card.name }}</span>
                </button>
              }
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 70;
      background: rgba(5, 8, 18, 0.78);
      backdrop-filter: blur(6px);
      display: grid;
      place-items: center;
      padding: var(--space-4);
    }
    .modal-panel {
      width: min(760px, 100%);
      max-height: 82dvh;
      overflow-y: auto;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-modal);
      padding: var(--space-5);
      display: flex;
      flex-direction: column;
      gap: var(--space-4);
    }
    .modal-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-subtle);
      padding-bottom: var(--space-3);
    }
    .modal-head h3 { font-size: var(--text-lg); }
    .modal-head span { font-size: var(--text-xs); color: var(--text-secondary); }
    .close-btn {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-primary);
      cursor: pointer;
    }
    .empty-msg { color: var(--text-secondary); font-size: var(--text-sm); margin: 0; }
    .discard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
      gap: var(--space-3);
    }
    .discard-card {
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-card);
      padding: var(--space-2);
      color: var(--text-primary);
      display: flex;
      flex-direction: column;
      gap: 6px;
      cursor: pointer;
      text-align: left;
    }
    .discard-card:hover { border-color: var(--accent-primary); }
    .discard-card img, .card-fallback {
      width: 100%;
      aspect-ratio: 5 / 7;
      object-fit: cover;
      border-radius: 4px;
      background: var(--bg-elevated);
    }
    .card-fallback {
      display: grid;
      place-items: center;
      font-size: var(--text-xs);
      text-align: center;
      padding: var(--space-2);
    }
    .discard-card span {
      font-size: 11px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DiscardPileModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly ownerLabel = input<string>('Jugador');
  readonly cards = input<CardViewDTO[]>([]);

  readonly closed = output<void>();
  readonly inspectCard = output<CardViewDTO>();

  @HostListener('document:keydown.escape')
  protected onEsc(): void {
    if (this.isOpen()) {
      this.closed.emit();
    }
  }
}
