import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { DeckCardItem } from '../../core/models/deck.models';
import { CardDetailModalComponent } from '../../shared/ui/card-detail-modal/card-detail-modal.component';
import { DeckValidationBarComponent } from '../../shared/ui/deck-validation-bar/deck-validation-bar.component';
import { EnergyBadgeComponent } from '../../shared/ui/energy-badge/energy-badge.component';
import { CatalogStore } from '../catalog/catalog.store';
import { DeckBuilderStore } from './deck-builder.store';

@Component({
  selector: 'app-deck-editor-page',
  standalone: true,
  imports: [RouterLink, DeckValidationBarComponent, EnergyBadgeComponent, CardDetailModalComponent],
  template: `
    <div class="editor-wrap">
      <header class="editor-top">
        <div class="title-row">
          <a routerLink="/decks" class="back-link">← Mis Mazos</a>
          <input
            type="text"
            class="deck-name-input"
            [value]="deckStore.deckName()"
            (input)="deckStore.deckName.set($any($event.target).value)"
            placeholder="Nombre del mazo"
            aria-label="Nombre del mazo" />
        </div>
        <div class="save-actions">
          @if (deckStore.saveMessage(); as msg) {
            <span class="save-msg">{{ msg }}</span>
          }
          <button
            type="button"
            class="save-btn"
            [disabled]="deckStore.saving()"
            (click)="deckStore.saveDeck()">
            @if (deckStore.saving()) { Guardando... } @else { Guardar Mazo }
          </button>
        </div>
      </header>

      <app-deck-validation-bar
        [totalCards]="deckStore.totalCards()"
        [hasBasicPokemon]="deckStore.hasBasicPokemon()"
        [copiesLimitOk]="deckStore.copiesLimitOk()"
        [isValid]="deckStore.isValidClient()"
        [errors]="deckStore.validationErrors()" />

      @if (deckStore.error(); as err) {
        <div class="error-banner" role="alert">{{ err }}</div>
      }

      @if (deckStore.loadingDeck()) {
        <div class="state-box">Cargando mazo...</div>
      } @else {
        <div class="split-layout">
          <section class="panel catalog-panel" aria-label="Buscador de cartas">
            <div class="panel-filters">
              <input
                type="search"
                class="search-input"
                placeholder="Buscar carta..."
                [value]="catalogStore.q()"
                (input)="catalogStore.onSearchInput($any($event.target).value)" />
              <div class="cat-pills">
                @for (cat of categories; track cat.value) {
                  <button
                    type="button"
                    class="pill-btn"
                    [class.is-active]="catalogStore.selectedCategory() === cat.value"
                    (click)="catalogStore.setCategory(cat.value)">
                    {{ cat.label }}
                  </button>
                }
              </div>
              <div class="type-chips">
                @for (t of energyTypes; track t) {
                  <button
                    type="button"
                    class="type-chip"
                    [class.is-active]="catalogStore.selectedType() === t"
                    (click)="catalogStore.setType(t)">
                    <app-energy-badge [type]="t" size="xs" />
                  </button>
                }
              </div>
            </div>

            <div class="catalog-grid">
              @for (card of catalogStore.cards(); track card.id) {
                <div class="mini-card">
                  <button type="button" class="thumb-btn" (click)="catalogStore.openCardDetail(card.id)">
                    @if (card.imageLowUrl) {
                      <img [src]="card.imageLowUrl" [alt]="card.name" loading="lazy" />
                    } @else {
                      <div class="thumb-fallback">{{ card.name }}</div>
                    }
                  </button>
                  <div class="mini-info">
                    <span class="mini-name" [title]="card.name">{{ card.name }}</span>
                    <div class="mini-actions">
                      <span class="qty-badge mono-num">
                        x{{ deckStore.quantitiesByCardId().get(card.id) ?? 0 }}
                      </span>
                      <button
                        type="button"
                        class="add-btn"
                        [disabled]="!deckStore.canAddCard(card)"
                        (click)="deckStore.addCard(card)">
                        + Agregar
                      </button>
                    </div>
                  </div>
                </div>
              }
            </div>

            @if (catalogStore.totalPages() > 1) {
              <div class="pager">
                <button type="button" [disabled]="catalogStore.page() === 0" (click)="catalogStore.goToPage(catalogStore.page() - 1)">←</button>
                <span class="mono-num">{{ catalogStore.page() + 1 }} / {{ catalogStore.totalPages() }}</span>
                <button type="button" [disabled]="catalogStore.page() + 1 >= catalogStore.totalPages()" (click)="catalogStore.goToPage(catalogStore.page() + 1)">→</button>
              </div>
            }
          </section>

          <section class="panel deck-panel" aria-label="Mazo actual">
            <div class="deck-panel-head">
              <h2>Cartas en el Mazo</h2>
              <span class="mono-num">{{ deckStore.totalCards() }}/60</span>
            </div>

            @if (deckStore.entries().length === 0) {
              <div class="empty-deck">Agrega cartas desde el panel izquierdo para construir tu mazo.</div>
            } @else {
              @if (deckStore.pokemonCards().length > 0) {
                <h3>Pokémon ({{ sumQty(deckStore.pokemonCards()) }})</h3>
                @for (item of deckStore.pokemonCards(); track item.cardId) {
                  <div class="deck-row">
                    <button type="button" class="row-name" (click)="catalogStore.openCardDetail(item.cardId)">
                      <span class="mono-num">#{{ item.localId }}</span> {{ item.name }}
                    </button>
                    <div class="qty-controls">
                      <button type="button" (click)="deckStore.decrementCard(item.cardId)">−</button>
                      <span class="mono-num">{{ item.quantity }}</span>
                      <button type="button" [disabled]="!deckStore.canAddCard(item)" (click)="deckStore.incrementCard(item.cardId)">+</button>
                    </div>
                  </div>
                }
              }

              @if (deckStore.trainerCards().length > 0) {
                <h3>Entrenadores ({{ sumQty(deckStore.trainerCards()) }})</h3>
                @for (item of deckStore.trainerCards(); track item.cardId) {
                  <div class="deck-row">
                    <button type="button" class="row-name" (click)="catalogStore.openCardDetail(item.cardId)">
                      <span class="mono-num">#{{ item.localId }}</span> {{ item.name }}
                    </button>
                    <div class="qty-controls">
                      <button type="button" (click)="deckStore.decrementCard(item.cardId)">−</button>
                      <span class="mono-num">{{ item.quantity }}</span>
                      <button type="button" [disabled]="!deckStore.canAddCard(item)" (click)="deckStore.incrementCard(item.cardId)">+</button>
                    </div>
                  </div>
                }
              }

              @if (deckStore.energyCards().length > 0) {
                <h3>Energías ({{ sumQty(deckStore.energyCards()) }})</h3>
                @for (item of deckStore.energyCards(); track item.cardId) {
                  <div class="deck-row">
                    <button type="button" class="row-name" (click)="catalogStore.openCardDetail(item.cardId)">
                      <span class="mono-num">#{{ item.localId }}</span> {{ item.name }}
                    </button>
                    <div class="qty-controls">
                      <button type="button" (click)="deckStore.decrementCard(item.cardId)">−</button>
                      <span class="mono-num">{{ item.quantity }}</span>
                      <button type="button" [disabled]="!deckStore.canAddCard(item)" (click)="deckStore.incrementCard(item.cardId)">+</button>
                    </div>
                  </div>
                }
              }
            }
          </section>
        </div>
      }

      <app-card-detail-modal
        [isOpen]="catalogStore.isModalOpen()"
        [loading]="catalogStore.detailLoading()"
        [card]="catalogStore.selectedCard()"
        [showFavorite]="authStore.isAuthenticated()"
        [isFavorite]="catalogStore.selectedCard() ? authStore.favoriteCardIds().has(catalogStore.selectedCard()!.id) : false"
        (favoriteToggled)="authStore.toggleFavorite($event)"
        (closed)="catalogStore.closeCardDetail()" />
    </div>
  `,
  styles: [`
    .editor-wrap {
      max-width: 1360px;
      margin: 0 auto;
      padding: var(--space-5);
      display: flex;
      flex-direction: column;
      gap: var(--space-4);
    }
    .editor-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-4);
      flex-wrap: wrap;
    }
    .title-row { display: flex; align-items: center; gap: var(--space-3); flex: 1 1 320px; }
    .back-link { font-size: var(--text-sm); color: var(--text-secondary); white-space: nowrap; }
    .deck-name-input {
      flex: 1;
      max-width: 420px;
      padding: 9px 12px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-surface);
      color: var(--text-primary);
      font-family: var(--font-display);
      font-size: var(--text-lg);
      font-weight: 700;
    }
    .save-actions { display: flex; align-items: center; gap: var(--space-3); }
    .save-msg { font-size: var(--text-xs); color: var(--state-success); font-weight: 600; }
    .save-btn {
      padding: 10px 18px;
      border: none;
      border-radius: var(--radius-md);
      background: var(--accent-primary);
      color: #0b1020;
      font-weight: 700;
      cursor: pointer;
    }
    .save-btn:disabled { opacity: 0.55; cursor: wait; }
    .error-banner {
      padding: var(--space-3);
      border-radius: var(--radius-sm);
      background: rgba(239, 68, 68, 0.16);
      border: 1px solid rgba(239, 68, 68, 0.45);
      color: #fecaca;
      font-size: var(--text-sm);
    }
    .split-layout {
      display: grid;
      grid-template-columns: 55% 1fr;
      gap: var(--space-4);
      align-items: start;
    }
    .panel {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-4);
    }
    .panel-filters { display: flex; flex-direction: column; gap: var(--space-2); margin-bottom: var(--space-3); }
    .search-input {
      padding: 8px 12px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-primary);
    }
    .cat-pills, .type-chips { display: flex; gap: 6px; flex-wrap: wrap; }
    .pill-btn {
      padding: 5px 10px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-secondary);
      font-size: var(--text-xs);
      cursor: pointer;
    }
    .pill-btn.is-active { background: var(--accent-primary); color: #0b1020; font-weight: 700; }
    .type-chip { padding: 2px; border-radius: 999px; border: 1px solid transparent; background: transparent; cursor: pointer; opacity: 0.75; }
    .type-chip.is-active { opacity: 1; border-color: var(--accent-primary); }
    .catalog-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
      gap: var(--space-3);
    }
    .mini-card {
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 6px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .thumb-btn { padding: 0; border: none; background: transparent; cursor: pointer; aspect-ratio: 5 / 7; overflow: hidden; border-radius: 4px; }
    .thumb-btn img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .thumb-fallback { width: 100%; height: 100%; display: grid; place-items: center; font-size: 11px; color: var(--text-muted); }
    .mini-name { display: block; font-size: var(--text-xs); font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .mini-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
    .qty-badge { font-size: var(--text-xs); color: var(--text-secondary); }
    .add-btn {
      padding: 4px 8px;
      border-radius: 4px;
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-primary);
      font-size: 11px;
      cursor: pointer;
    }
    .add-btn:disabled { opacity: 0.35; cursor: not-allowed; }
    .pager { display: flex; justify-content: center; align-items: center; gap: var(--space-3); margin-top: var(--space-3); }
    .pager button { padding: 4px 10px; border-radius: 4px; border: 1px solid var(--border-subtle); background: var(--bg-tabletop); color: var(--text-primary); cursor: pointer; }
    .deck-panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-3); }
    .deck-panel-head h2 { font-size: var(--text-lg); }
    .deck-panel h3 { font-size: var(--text-xs); text-transform: uppercase; color: var(--text-muted); margin: var(--space-3) 0 var(--space-2); }
    .deck-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 6px 8px;
      border-radius: var(--radius-sm);
      background: var(--bg-tabletop);
      margin-bottom: 4px;
    }
    .row-name { border: none; background: transparent; color: var(--text-primary); font-size: var(--text-sm); text-align: left; cursor: pointer; }
    .row-name span { color: var(--text-muted); margin-right: 4px; }
    .qty-controls { display: flex; align-items: center; gap: 8px; }
    .qty-controls button {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-primary);
      cursor: pointer;
    }
    .qty-controls button:disabled { opacity: 0.35; cursor: not-allowed; }
    .empty-deck, .state-box { padding: var(--space-6); text-align: center; color: var(--text-secondary); font-size: var(--text-sm); }
    @media (max-width: 960px) {
      .split-layout { grid-template-columns: 1fr; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckEditorPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly deckStore = inject(DeckBuilderStore);
  protected readonly catalogStore = inject(CatalogStore);
  protected readonly authStore = inject(AuthStore);

  protected readonly categories = [
    { label: 'Todas', value: '' },
    { label: 'Pokémon', value: 'Pokemon' },
    { label: 'Entrenador', value: 'Trainer' },
    { label: 'Energía', value: 'Energy' }
  ];

  protected readonly energyTypes = ['Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Colorless'];

  ngOnInit(): void {
    this.catalogStore.initialize();
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.deckStore.loadDeckForEdit(id);
    } else {
      this.deckStore.initNewDeck();
    }
  }

  protected sumQty(items: DeckCardItem[]): number {
    return items.reduce((sum, item) => sum + item.quantity, 0);
  }
}
