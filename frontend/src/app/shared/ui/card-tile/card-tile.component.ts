import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { CardSummary } from '../../../core/models/catalog.models';
import { EnergyBadgeComponent } from '../energy-badge/energy-badge.component';

@Component({
  selector: 'app-card-tile',
  standalone: true,
  imports: [EnergyBadgeComponent],
  template: `
    <button
      type="button"
      class="card-tile"
      [class.is-selected]="selected()"
      [class.is-playable]="playable()"
      (click)="cardSelect.emit(card())">
      <div class="card-frame">
        @if (card().imageLowUrl && !imageFailed()) {
          <img
            [src]="card().imageLowUrl"
            [alt]="card().name"
            loading="lazy"
            (error)="imageFailed.set(true)" />
        } @else {
          <div class="card-back-fallback" aria-label="Dorso de carta">
            <div class="pokeball-ring"></div>
            <span>{{ card().name }}</span>
          </div>
        }
      </div>

      <div class="card-meta">
        <div class="meta-top">
          <span class="card-num mono-num">#{{ card().localId }}</span>
          @if (card().hp !== null) {
            <span class="card-hp mono-num">{{ card().hp }} PS</span>
          } @else {
            <span class="card-cat">{{ card().category }}</span>
          }
        </div>
        <h3 class="card-name" [title]="card().name">{{ card().name }}</h3>
        @if (card().types.length > 0) {
          <div class="card-types">
            @for (type of card().types; track type) {
              <app-energy-badge [type]="type" size="xs" />
            }
          </div>
        }
      </div>
    </button>
  `,
  styles: [`
    .card-tile {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
      padding: var(--space-2);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      color: var(--text-primary);
      text-align: left;
      cursor: pointer;
      box-shadow: var(--shadow-card);
      transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
    }
    .card-tile:hover {
      transform: translateY(-4px);
      border-color: rgba(255, 203, 5, 0.45);
      background: var(--bg-elevated);
    }
    .card-tile.is-selected,
    .card-tile.is-playable {
      box-shadow: var(--shadow-active-glow);
      border-color: var(--accent-primary);
    }
    .card-frame {
      width: 100%;
      aspect-ratio: 5 / 7;
      border-radius: var(--radius-card);
      overflow: hidden;
      background: var(--bg-tabletop);
    }
    .card-frame img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .card-back-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      padding: var(--space-3);
      text-align: center;
      background: radial-gradient(circle at 50% 42%, #1d356b, #0b1020);
      color: var(--text-secondary);
      font-size: var(--text-xs);
    }
    .pokeball-ring {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      border: 3px solid var(--accent-primary);
      opacity: 0.7;
    }
    .card-meta {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 0 2px;
    }
    .meta-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: var(--text-xs);
      color: var(--text-secondary);
    }
    .card-hp {
      color: var(--accent-primary);
      font-weight: 700;
    }
    .card-name {
      font-size: var(--text-sm);
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .card-types {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-top: 2px;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardTileComponent {
  readonly card = input.required<CardSummary>();
  readonly selected = input<boolean>(false);
  readonly playable = input<boolean>(false);
  readonly cardSelect = output<CardSummary>();

  protected readonly imageFailed = signal(false);
}
