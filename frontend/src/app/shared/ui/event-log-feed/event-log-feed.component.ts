import { ChangeDetectionStrategy, Component, ElementRef, effect, input, output, viewChild } from '@angular/core';
import { GameEvent } from '../../../core/models/match.models';

@Component({
  selector: 'app-event-log-feed',
  standalone: true,
  template: `
    <aside class="log-panel" aria-label="Historial de combate">
      <header class="log-head">
        <h3>Historial</h3>
        <div class="log-controls">
          <button
            type="button"
            class="ctrl-btn"
            [class.active]="speed() === 1"
            (click)="speedChanged.emit(1)">1x</button>
          <button
            type="button"
            class="ctrl-btn"
            [class.active]="speed() === 2"
            (click)="speedChanged.emit(2)">2x</button>
          @if (animating()) {
            <button type="button" class="ctrl-btn skip" (click)="skipRequested.emit()">Saltar</button>
          }
        </div>
      </header>

      <div class="log-list" #scrollBox>
        @for (evt of events(); track evt.sequence) {
          <div
            class="log-item"
            [class.is-cpu]="evt.actor === 'CPU'"
            [class.is-active]="activeEventSeq() === evt.sequence">
            <div class="item-meta">
              <span class="actor-tag">{{ evt.actor === 'PLAYER' ? 'Tú' : 'CPU' }}</span>
              <span class="seq mono-num">#{{ evt.sequence }}</span>
            </div>
            <p>{{ evt.message }}</p>
          </div>
        } @empty {
          <p class="empty">Los eventos de la partida aparecerán aquí en orden cronológico.</p>
        }
      </div>
    </aside>
  `,
  styles: [`
    .log-panel {
      height: 100%;
      display: flex;
      flex-direction: column;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      overflow: hidden;
    }
    .log-head {
      padding: var(--space-3);
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-2);
    }
    .log-head h3 { font-size: var(--text-sm); }
    .log-controls { display: flex; gap: 4px; }
    .ctrl-btn {
      padding: 3px 8px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-secondary);
      font-size: 11px;
      font-weight: 700;
      cursor: pointer;
    }
    .ctrl-btn.active {
      border-color: var(--accent-primary);
      color: var(--accent-primary);
    }
    .ctrl-btn.skip {
      border-color: var(--accent-secondary);
      color: var(--text-primary);
      background: rgba(59, 130, 246, 0.25);
    }
    .log-list {
      flex: 1;
      overflow-y: auto;
      padding: var(--space-3);
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }
    .log-item {
      padding: 7px 9px;
      border-radius: var(--radius-sm);
      background: rgba(255, 255, 255, 0.03);
      border-left: 3px solid var(--accent-primary);
      transition: background-color 200ms ease, transform 200ms ease;
    }
    .log-item.is-cpu { border-left-color: var(--accent-secondary); }
    .log-item.is-active {
      background: rgba(255, 203, 5, 0.16);
      transform: translateX(2px);
    }
    .item-meta {
      display: flex;
      justify-content: space-between;
      font-size: 10px;
      color: var(--text-muted);
      margin-bottom: 2px;
    }
    .actor-tag { font-weight: 700; text-transform: uppercase; color: var(--text-secondary); }
    .log-item p { margin: 0; font-size: var(--text-xs); line-height: 1.35; color: var(--text-primary); }
    .empty { font-size: var(--text-xs); color: var(--text-muted); margin: 0; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EventLogFeedComponent {
  readonly events = input<GameEvent[]>([]);
  readonly activeEventSeq = input<number | null>(null);
  readonly speed = input<1 | 2>(1);
  readonly animating = input<boolean>(false);

  readonly speedChanged = output<1 | 2>();
  readonly skipRequested = output<void>();

  private readonly scrollBox = viewChild<ElementRef<HTMLElement>>('scrollBox');

  constructor() {
    effect(() => {
      this.events();
      this.activeEventSeq();
      const el = this.scrollBox()?.nativeElement;
      if (el) {
        queueMicrotask(() => {
          el.scrollTop = el.scrollHeight;
        });
      }
    });
  }
}
