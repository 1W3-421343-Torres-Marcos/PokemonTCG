import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { AuthStore } from '../../core/auth/auth.store';
import { CardDetailModalComponent } from '../../shared/ui/card-detail-modal/card-detail-modal.component';
import { CardTileComponent } from '../../shared/ui/card-tile/card-tile.component';
import { EnergyBadgeComponent } from '../../shared/ui/energy-badge/energy-badge.component';
import { CatalogStore } from './catalog.store';

@Component({
  selector: 'app-catalog-page',
  standalone: true,
  imports: [CardTileComponent, CardDetailModalComponent, EnergyBadgeComponent],
  template: `
    <div class="catalog-wrap">
      <header class="catalog-header">
        <div>
          <h1>Catálogo de Cartas</h1>
          <p class="subtitle">
            <span class="mono-num">{{ store.totalElements() }}</span> cartas encontradas en el catálogo local
          </p>
        </div>
        @if (store.hasActiveFilters()) {
          <button type="button" class="clear-btn" (click)="store.resetFilters()">Limpiar filtros</button>
        }
      </header>

      <section class="filters-bar" aria-label="Filtros de búsqueda">
        <div class="filters-top">
          <input
            type="search"
            class="search-input"
            placeholder="Buscar carta por nombre (ej. Charizard, Professor Oak)..."
            [value]="store.q()"
            (input)="store.onSearchInput($any($event.target).value)"
            aria-label="Buscar por nombre" />

          <div class="category-pills" role="group" aria-label="Filtrar por categoría">
            @for (cat of categories; track cat.value) {
              <button
                type="button"
                class="pill-btn"
                [class.is-active]="store.selectedCategory() === cat.value"
                (click)="store.setCategory(cat.value)">
                {{ cat.label }}
              </button>
            }
          </div>
        </div>

        <div class="type-row" role="group" aria-label="Filtrar por tipo de energía">
          <span class="filter-label">Tipo:</span>
          @for (energyType of energyTypes; track energyType) {
            <button
              type="button"
              class="type-chip"
              [class.is-active]="store.selectedType() === energyType"
              (click)="store.setType(energyType)">
              <app-energy-badge [type]="energyType" size="xs" [showLabel]="true" />
            </button>
          }
        </div>
      </section>

      @if (store.error(); as err) {
        <div class="state-box is-error">{{ err }}</div>
      } @else if (store.loading()) {
        <div class="state-box">Cargando cartas...</div>
      } @else if (store.cards().length === 0) {
        <div class="state-box">No hay cartas que coincidan con los filtros seleccionados.</div>
      } @else {
        <div class="card-grid">
          @for (card of store.cards(); track card.id) {
            <app-card-tile [card]="card" (cardSelect)="store.openCardDetail($event.id)" />
          }
        </div>

        @if (store.totalPages() > 1) {
          <nav class="pagination" aria-label="Paginación">
            <button
              type="button"
              class="page-btn"
              [disabled]="store.page() === 0"
              (click)="store.goToPage(store.page() - 1)">
              ← Anterior
            </button>
            <span class="page-info mono-num">
              Página {{ store.page() + 1 }} de {{ store.totalPages() }}
            </span>
            <button
              type="button"
              class="page-btn"
              [disabled]="store.page() + 1 >= store.totalPages()"
              (click)="store.goToPage(store.page() + 1)">
              Siguiente →
            </button>
          </nav>
        }
      }

      <app-card-detail-modal
        [isOpen]="store.isModalOpen()"
        [loading]="store.detailLoading()"
        [card]="store.selectedCard()"
        [showFavorite]="authStore.isAuthenticated()"
        [isFavorite]="store.selectedCard() ? authStore.favoriteCardIds().has(store.selectedCard()!.id) : false"
        (favoriteToggled)="authStore.toggleFavorite($event)"
        (closed)="store.closeCardDetail()" />
    </div>
  `,
  styles: [`
    .catalog-wrap {
      max-width: 1320px;
      margin: 0 auto;
      padding: var(--space-6) var(--space-5);
      display: flex;
      flex-direction: column;
      gap: var(--space-5);
    }
    .catalog-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: var(--space-4);
      flex-wrap: wrap;
    }
    .catalog-header h1 { font-size: var(--text-2xl); }
    .subtitle { margin: 4px 0 0; font-size: var(--text-sm); color: var(--text-secondary); }
    .clear-btn, .page-btn {
      padding: 8px 14px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-surface);
      color: var(--text-primary);
      font-size: var(--text-sm);
      cursor: pointer;
    }
    .clear-btn:hover, .page-btn:not(:disabled):hover { border-color: var(--accent-primary); }
    .page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .filters-bar {
      position: sticky;
      top: 58px;
      z-index: 20;
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
      padding: var(--space-4);
      border-radius: var(--radius-lg);
      background: rgba(19, 27, 46, 0.95);
      backdrop-filter: blur(8px);
      border: 1px solid var(--border-subtle);
      box-shadow: var(--shadow-card);
    }
    .filters-top {
      display: flex;
      gap: var(--space-3);
      align-items: center;
      flex-wrap: wrap;
    }
    .search-input {
      flex: 1 1 280px;
      padding: 10px 14px;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-primary);
      font-size: var(--text-sm);
    }
    .search-input:focus { outline: 2px solid var(--accent-primary); border-color: transparent; }
    .category-pills, .type-row {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      flex-wrap: wrap;
    }
    .filter-label { font-size: var(--text-xs); color: var(--text-muted); margin-right: 4px; }
    .pill-btn {
      padding: 8px 12px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-secondary);
      font-size: var(--text-xs);
      font-weight: 600;
      cursor: pointer;
    }
    .pill-btn.is-active {
      background: var(--accent-primary);
      color: #0b1020;
      border-color: var(--accent-primary);
    }
    .type-chip {
      padding: 2px;
      border: 1px solid transparent;
      border-radius: 999px;
      background: transparent;
      cursor: pointer;
      opacity: 0.78;
    }
    .type-chip.is-active {
      opacity: 1;
      border-color: var(--accent-primary);
      box-shadow: 0 0 10px rgba(255, 203, 5, 0.25);
    }
    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(175px, 1fr));
      gap: var(--space-4);
    }
    .state-box {
      padding: var(--space-8);
      text-align: center;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      color: var(--text-secondary);
    }
    .state-box.is-error { color: var(--state-danger); border-color: rgba(239, 68, 68, 0.4); }
    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: var(--space-4);
      padding-top: var(--space-2);
    }
    .page-info { font-size: var(--text-sm); color: var(--text-secondary); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogPageComponent implements OnInit {
  protected readonly store = inject(CatalogStore);
  protected readonly authStore = inject(AuthStore);

  protected readonly categories = [
    { label: 'Todas', value: '' },
    { label: 'Pokémon', value: 'Pokemon' },
    { label: 'Entrenador', value: 'Trainer' },
    { label: 'Energía', value: 'Energy' }
  ];

  protected readonly energyTypes = [
    'Grass',
    'Fire',
    'Water',
    'Lightning',
    'Psychic',
    'Fighting',
    'Colorless'
  ];

  ngOnInit(): void {
    this.store.initialize();
  }
}
