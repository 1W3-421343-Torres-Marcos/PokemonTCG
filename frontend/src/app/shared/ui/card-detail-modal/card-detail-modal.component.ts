import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { CardDetail } from '../../../core/models/catalog.models';
import { EnergyBadgeComponent } from '../energy-badge/energy-badge.component';

@Component({
  selector: 'app-card-detail-modal',
  standalone: true,
  imports: [EnergyBadgeComponent],
  template: `
    @if (isOpen()) {
      <div class="modal-backdrop" (click)="closed.emit()" role="dialog" aria-modal="true" aria-label="Detalle de carta">
        <div class="modal-panel" (click)="$event.stopPropagation()">
          <button type="button" class="close-btn" (click)="closed.emit()" aria-label="Cerrar">✕</button>

          @if (loading()) {
            <div class="modal-loading">Cargando detalle de carta...</div>
          } @else {
            @if (card(); as c) {
            <div class="modal-grid">
              <div class="art-col">
                @if (c.imageHighUrl || c.imageLowUrl) {
                  <img [src]="c.imageHighUrl || c.imageLowUrl" [alt]="c.name" />
                } @else {
                  <div class="art-fallback">{{ c.name }}</div>
                }
              </div>

              <div class="info-col">
                <div class="header-row">
                  <div>
                    <span class="set-pill">{{ c.setName }} · <span class="mono-num">#{{ c.localId }}</span></span>
                    <h2>{{ c.name }}</h2>
                  </div>
                  <div class="header-actions">
                    @if (c.hp !== null) {
                      <span class="hp-pill mono-num">{{ c.hp }} PS</span>
                    }
                    @if (showFavorite()) {
                      <button
                        type="button"
                        class="fav-btn"
                        [class.is-fav]="isFavorite()"
                        (click)="favoriteToggled.emit(c.id)">
                        @if (isFavorite()) { ★ Favorita } @else { ☆ Favorita }
                      </button>
                    }
                  </div>
                </div>

                <div class="tag-row">
                  <span class="tag">{{ c.category }}</span>
                  @if (c.stage) { <span class="tag">{{ c.stage }}</span> }
                  @if (c.trainerType) { <span class="tag">{{ c.trainerType }}</span> }
                  @if (c.energyType) { <span class="tag">{{ c.energyType }} Energy</span> }
                  @if (c.evolveFrom) { <span class="tag">Evoluciona de: {{ c.evolveFrom }}</span> }
                  @for (t of c.types; track t) {
                    <app-energy-badge [type]="t" size="sm" [showLabel]="true" />
                  }
                </div>

                @if (c.attacks.length > 0) {
                  <section class="section">
                    <h4>Ataques</h4>
                    @for (atk of c.attacks; track atk.name) {
                      <div class="attack-card">
                        <div class="attack-head">
                          <div class="attack-cost">
                            @for (costType of atk.cost; track $index) {
                              <app-energy-badge [type]="costType" size="xs" />
                            }
                            <strong>{{ atk.name }}</strong>
                          </div>
                          @if (atk.damage) {
                            <span class="damage mono-num">{{ atk.damage }}</span>
                          }
                        </div>
                        @if (atk.effect) {
                          <p class="effect">{{ atk.effect }}</p>
                        }
                      </div>
                    }
                  </section>
                }

                @if (c.effectText) {
                  <section class="section">
                    <h4>Efecto</h4>
                    <p class="effect-box">{{ c.effectText }}</p>
                  </section>
                }

                <div class="stats-grid">
                  <div>
                    <span class="stat-label">Debilidad</span>
                    <div class="stat-val">
                      @for (w of c.weaknesses; track w.type) {
                        <app-energy-badge [type]="w.type" size="xs" /> <span class="mono-num">{{ w.value }}</span>
                      } @empty { <span>—</span> }
                    </div>
                  </div>
                  <div>
                    <span class="stat-label">Resistencia</span>
                    <div class="stat-val">
                      @for (r of c.resistances; track r.type) {
                        <app-energy-badge [type]="r.type" size="xs" /> <span class="mono-num">{{ r.value }}</span>
                      } @empty { <span>—</span> }
                    </div>
                  </div>
                  <div>
                    <span class="stat-label">Retirada</span>
                    <div class="stat-val mono-num">{{ c.retreat ?? '—' }}</div>
                  </div>
                </div>

                <div class="footer-meta">
                  <span>Rareza: {{ c.rarity ?? '—' }}</span>
                  <span>Ilustrador: {{ c.illustrator ?? '—' }}</span>
                </div>
              </div>
            </div>
            }
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 50;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--space-4);
      background: rgba(6, 9, 18, 0.78);
      backdrop-filter: blur(6px);
    }
    .modal-panel {
      position: relative;
      width: min(860px, 100%);
      max-height: 90dvh;
      overflow-y: auto;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-6);
      box-shadow: var(--shadow-modal);
    }
    .close-btn {
      position: absolute;
      top: var(--space-4);
      right: var(--space-4);
      width: 34px;
      height: 34px;
      border-radius: 50%;
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-primary);
      cursor: pointer;
    }
    .modal-loading {
      padding: var(--space-8);
      text-align: center;
      color: var(--text-secondary);
    }
    .modal-grid {
      display: grid;
      grid-template-columns: minmax(220px, 290px) 1fr;
      gap: var(--space-6);
      align-items: start;
    }
    .art-col img, .art-fallback {
      width: 100%;
      aspect-ratio: 5 / 7;
      border-radius: var(--radius-card);
      object-fit: cover;
      box-shadow: var(--shadow-card);
    }
    .art-fallback {
      display: grid;
      place-items: center;
      background: var(--bg-tabletop);
      color: var(--text-secondary);
    }
    .header-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: var(--space-3);
      padding-right: var(--space-8);
    }
    .set-pill { font-size: var(--text-xs); color: var(--text-secondary); }
    .header-row h2 { font-size: var(--text-2xl); margin-top: 2px; }
    .hp-pill {
      font-size: var(--text-lg);
      font-weight: 700;
      color: var(--accent-primary);
      background: var(--bg-elevated);
      padding: 4px 10px;
      border-radius: var(--radius-sm);
    }
    .tag-row {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-2);
      margin-top: var(--space-3);
    }
    .tag {
      font-size: var(--text-xs);
      padding: 3px 9px;
      border-radius: 999px;
      background: var(--bg-elevated);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
    }
    .section { margin-top: var(--space-4); }
    .section h4 { font-size: var(--text-sm); color: var(--text-secondary); margin-bottom: var(--space-2); text-transform: uppercase; letter-spacing: 0.04em; }
    .attack-card, .effect-box {
      background: var(--bg-elevated);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: var(--space-3);
      margin-bottom: var(--space-2);
    }
    .attack-head { display: flex; justify-content: space-between; align-items: center; gap: var(--space-2); }
    .attack-cost { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
    .damage { font-weight: 700; color: var(--accent-primary); }
    .effect, .effect-box { margin: var(--space-2) 0 0; font-size: var(--text-sm); color: var(--text-secondary); }
    .effect-box { margin-top: 0; }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: var(--space-3);
      margin-top: var(--space-4);
      padding: var(--space-3);
      background: var(--bg-tabletop);
      border-radius: var(--radius-sm);
    }
    .stat-label { display: block; font-size: var(--text-xs); color: var(--text-muted); margin-bottom: 4px; }
    .stat-val { display: flex; align-items: center; gap: 6px; font-size: var(--text-sm); }
    .footer-meta {
      display: flex;
      justify-content: space-between;
      gap: var(--space-4);
      margin-top: var(--space-4);
      font-size: var(--text-xs);
      color: var(--text-muted);
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: var(--space-2);
    }
    .fav-btn {
      padding: 5px 10px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-secondary);
      font-size: var(--text-xs);
      font-weight: 600;
      cursor: pointer;
    }
    .fav-btn.is-fav {
      border-color: var(--accent-primary);
      color: var(--accent-primary);
      background: rgba(255, 203, 5, 0.14);
    }
    @media (max-width: 720px) {
      .modal-grid { grid-template-columns: 1fr; }
      .art-col { max-width: 240px; margin: 0 auto; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardDetailModalComponent {
  readonly card = input<CardDetail | null>(null);
  readonly loading = input<boolean>(false);
  readonly isOpen = input<boolean>(false);
  readonly showFavorite = input<boolean>(false);
  readonly isFavorite = input<boolean>(false);
  readonly closed = output<void>();
  readonly favoriteToggled = output<string>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen()) {
      this.closed.emit();
    }
  }
}
