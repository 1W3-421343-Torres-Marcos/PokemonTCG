import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DeckBuilderStore } from './deck-builder.store';

@Component({
  selector: 'app-deck-list-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="decks-wrap">
      <header class="page-header">
        <div>
          <h1>Mis Mazos</h1>
          <p class="subtitle">Administra tus mazos de 60 cartas o clona un Starter Deck oficial de Base Set.</p>
        </div>
        <a routerLink="/decks/new" class="new-btn">+ Nuevo Mazo</a>
      </header>

      @if (store.error(); as err) {
        <div class="error-banner" role="alert">{{ err }}</div>
      }

      @if (store.loadingList()) {
        <div class="state-box">Cargando mazos...</div>
      } @else {
        <section class="deck-section">
          <h2>Mazos del Entrenador ({{ store.myDecks().length }})</h2>
          @if (store.myDecks().length === 0) {
            <div class="state-box">
              Todavía no tienes mazos propios. Crea uno nuevo o clona un Starter Deck abajo.
            </div>
          } @else {
            <div class="deck-grid">
              @for (deck of store.myDecks(); track deck.id) {
                <article class="deck-card">
                  <div class="card-top">
                    <span class="status-pill" [class.valid]="deck.valid" [class.draft]="!deck.valid">
                      @if (deck.valid) { Válido } @else { Borrador }
                    </span>
                    <span class="count-tag mono-num">{{ deck.totalCards }}/60</span>
                  </div>
                  <h3>{{ deck.name }}</h3>
                  <p>{{ deck.description || 'Sin descripción.' }}</p>
                  <div class="card-actions">
                    <a [routerLink]="['/decks', deck.id]" class="edit-btn">Editar</a>
                    <button type="button" class="del-btn" (click)="store.deleteDeck(deck.id)">Eliminar</button>
                  </div>
                </article>
              }
            </div>
          }
        </section>

        <section class="deck-section">
          <h2>Mazos Prearmados (Starter Decks)</h2>
          <div class="deck-grid">
            @for (starter of store.starterDecks(); track starter.id) {
              <article class="deck-card starter">
                <div class="card-top">
                  <span class="status-pill starter-pill">Starter Oficial</span>
                  <span class="count-tag mono-num">{{ starter.totalCards }}/60</span>
                </div>
                <h3>{{ starter.name }}</h3>
                <p>{{ starter.description }}</p>
                <div class="card-actions">
                  <button
                    type="button"
                    class="clone-btn"
                    [disabled]="store.saving()"
                    (click)="store.cloneStarter(starter.id)">
                    Clonar a Mis Mazos
                  </button>
                </div>
              </article>
            }
          </div>
        </section>
      }
    </div>
  `,
  styles: [`
    .decks-wrap {
      max-width: 1200px;
      margin: 0 auto;
      padding: var(--space-6) var(--space-5);
      display: flex;
      flex-direction: column;
      gap: var(--space-6);
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: var(--space-4);
      flex-wrap: wrap;
    }
    .page-header h1 { font-size: var(--text-2xl); }
    .subtitle { margin: 4px 0 0; font-size: var(--text-sm); color: var(--text-secondary); }
    .new-btn, .clone-btn {
      padding: 10px 16px;
      border-radius: var(--radius-md);
      background: var(--accent-primary);
      color: #0b1020;
      font-weight: 700;
      font-size: var(--text-sm);
      border: none;
      cursor: pointer;
    }
    .clone-btn:disabled { opacity: 0.55; cursor: wait; }
    .error-banner {
      padding: var(--space-3);
      border-radius: var(--radius-sm);
      background: rgba(239, 68, 68, 0.16);
      border: 1px solid rgba(239, 68, 68, 0.45);
      color: #fecaca;
      font-size: var(--text-sm);
    }
    .deck-section h2 { font-size: var(--text-lg); margin-bottom: var(--space-3); color: var(--text-secondary); }
    .deck-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: var(--space-4);
    }
    .deck-card {
      padding: var(--space-5);
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
      box-shadow: var(--shadow-card);
    }
    .deck-card.starter { border-color: rgba(59, 130, 246, 0.35); }
    .card-top { display: flex; justify-content: space-between; align-items: center; }
    .status-pill {
      font-size: var(--text-xs);
      font-weight: 700;
      padding: 3px 9px;
      border-radius: 999px;
    }
    .status-pill.valid { background: rgba(34, 197, 94, 0.16); color: var(--state-success); }
    .status-pill.draft { background: rgba(245, 158, 11, 0.16); color: var(--state-warning); }
    .status-pill.starter-pill { background: rgba(59, 130, 246, 0.2); color: var(--accent-secondary); }
    .count-tag { font-size: var(--text-sm); color: var(--text-secondary); font-weight: 700; }
    .deck-card h3 { font-size: var(--text-lg); }
    .deck-card p { margin: 0; font-size: var(--text-sm); color: var(--text-secondary); flex: 1; }
    .card-actions { display: flex; gap: var(--space-2); }
    .edit-btn, .del-btn {
      padding: 8px 14px;
      border-radius: var(--radius-sm);
      font-size: var(--text-xs);
      font-weight: 600;
      cursor: pointer;
    }
    .edit-btn { background: var(--bg-elevated); border: 1px solid var(--border-subtle); color: var(--text-primary); }
    .del-btn { background: transparent; border: 1px solid rgba(239, 68, 68, 0.45); color: #fca5a5; }
    .state-box {
      padding: var(--space-6);
      text-align: center;
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckListPageComponent implements OnInit {
  protected readonly store = inject(DeckBuilderStore);

  ngOnInit(): void {
    this.store.loadLists();
  }
}
