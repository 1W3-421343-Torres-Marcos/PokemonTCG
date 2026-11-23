import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, forkJoin, of, tap } from 'rxjs';
import { DeckApiService } from '../../core/api/deck-api.service';
import { CardSummary } from '../../core/models/catalog.models';
import { DeckCardItem, DeckSummary } from '../../core/models/deck.models';

@Injectable({ providedIn: 'root' })
export class DeckBuilderStore {
  private readonly api = inject(DeckApiService);
  private readonly router = inject(Router);

  readonly myDecks = signal<DeckSummary[]>([]);
  readonly starterDecks = signal<DeckSummary[]>([]);
  readonly loadingList = signal<boolean>(false);
  readonly loadingDeck = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly saveMessage = signal<string | null>(null);

  readonly activeDeckId = signal<string | null>(null);
  readonly deckName = signal<string>('Nuevo Mazo');
  readonly deckDescription = signal<string>('');
  readonly entries = signal<DeckCardItem[]>([]);

  readonly totalCards = computed(() =>
    this.entries().reduce((sum, item) => sum + item.quantity, 0)
  );

  readonly copiesByName = computed(() => {
    const map = new Map<string, number>();
    for (const item of this.entries()) {
      const key = item.name.trim().toLowerCase();
      map.set(key, (map.get(key) ?? 0) + item.quantity);
    }
    return map;
  });

  readonly quantitiesByCardId = computed(() => {
    const map = new Map<string, number>();
    for (const item of this.entries()) {
      map.set(item.cardId, item.quantity);
    }
    return map;
  });

  readonly hasBasicPokemon = computed(() =>
    this.entries().some(
      (item) =>
        item.category.toLowerCase() === 'pokemon' &&
        item.stage?.toLowerCase() === 'basic'
    )
  );

  readonly copiesLimitOk = computed(() => {
    for (const item of this.entries()) {
      if (!isBasicEnergyItem(item)) {
        const count = this.copiesByName().get(item.name.trim().toLowerCase()) ?? 0;
        if (count > 4) {
          return false;
        }
      }
    }
    return true;
  });

  readonly validationErrors = computed(() => {
    const errors: string[] = [];
    const total = this.totalCards();
    if (total !== 60) {
      errors.push(
        total < 60
          ? `Faltan ${60 - total} cartas para completar las 60 obligatorias.`
          : `Sobran ${total - 60} cartas (el máximo exacto es 60).`
      );
    }
    if (!this.hasBasicPokemon()) {
      errors.push('Incluye al menos 1 Pokémon Básico.');
    }
    const reportedNames = new Set<string>();
    for (const item of this.entries()) {
      if (!isBasicEnergyItem(item)) {
        const key = item.name.trim().toLowerCase();
        const count = this.copiesByName().get(key) ?? 0;
        if (count > 4 && !reportedNames.has(key)) {
          reportedNames.add(key);
          errors.push(`Máximo 4 copias de "${item.name}" (tienes ${count}).`);
        }
      }
    }
    return errors;
  });

  readonly isValidClient = computed(() => this.validationErrors().length === 0);

  readonly pokemonCards = computed(() =>
    this.entries().filter((e) => e.category.toLowerCase() === 'pokemon')
  );
  readonly trainerCards = computed(() =>
    this.entries().filter((e) => e.category.toLowerCase() === 'trainer')
  );
  readonly energyCards = computed(() =>
    this.entries().filter((e) => e.category.toLowerCase() === 'energy')
  );

  loadLists(): void {
    this.loadingList.set(true);
    this.error.set(null);
    forkJoin({
      mine: this.api.listMyDecks(),
      starters: this.api.listStarters()
    })
      .pipe(
        catchError(() => {
          this.error.set('No se pudieron cargar los mazos.');
          return of(null);
        }),
        finalize(() => this.loadingList.set(false))
      )
      .subscribe((res) => {
        if (!res) return;
        this.myDecks.set(res.mine);
        this.starterDecks.set(res.starters);
      });
  }

  cloneStarter(starterId: string): void {
    this.saving.set(true);
    this.error.set(null);
    this.api
      .cloneStarter(starterId)
      .pipe(
        tap((cloned) => {
          void this.router.navigate(['/decks', cloned.id]);
        }),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractErrorMessage(err, 'No se pudo clonar el starter deck.'));
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe();
  }

  deleteDeck(deckId: string): void {
    this.error.set(null);
    this.api
      .deleteDeck(deckId)
      .pipe(
        tap(() => {
          this.myDecks.update((list) => list.filter((d) => d.id !== deckId));
        }),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractErrorMessage(err, 'No se pudo eliminar el mazo.'));
          return of(null);
        })
      )
      .subscribe();
  }

  initNewDeck(): void {
    this.activeDeckId.set(null);
    this.deckName.set('Nuevo Mazo');
    this.deckDescription.set('');
    this.entries.set([]);
    this.error.set(null);
    this.saveMessage.set(null);
  }

  loadDeckForEdit(deckId: string): void {
    this.loadingDeck.set(true);
    this.error.set(null);
    this.saveMessage.set(null);
    this.api
      .getDeck(deckId)
      .pipe(
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractErrorMessage(err, 'No se pudo cargar el mazo solicitado.'));
          return of(null);
        }),
        finalize(() => this.loadingDeck.set(false))
      )
      .subscribe((detail) => {
        if (!detail) return;
        this.activeDeckId.set(detail.id);
        this.deckName.set(detail.name);
        this.deckDescription.set(detail.description ?? '');
        this.entries.set(detail.cards);
      });
  }

  canAddCard(card: Pick<CardSummary, 'name' | 'category' | 'energyType'>): boolean {
    if (isBasicEnergyItem(card)) {
      return true;
    }
    const count = this.copiesByName().get(card.name.trim().toLowerCase()) ?? 0;
    return count < 4;
  }

  addCard(card: CardSummary): void {
    if (!this.canAddCard(card)) {
      return;
    }
    this.saveMessage.set(null);
    this.entries.update((current) => {
      const existing = current.find((e) => e.cardId === card.id);
      if (existing) {
        return current.map((e) => (e.cardId === card.id ? { ...e, quantity: e.quantity + 1 } : e));
      }
      const added: DeckCardItem = {
        cardId: card.id,
        localId: card.localId,
        sortOrder: card.sortOrder,
        name: card.name,
        category: card.category,
        stage: card.stage,
        trainerType: card.trainerType,
        energyType: card.energyType,
        imageLowUrl: card.imageLowUrl,
        types: card.types,
        quantity: 1
      };
      return [...current, added].sort((a, b) => a.sortOrder - b.sortOrder || a.cardId.localeCompare(b.cardId));
    });
  }

  incrementCard(cardId: string): void {
    const target = this.entries().find((e) => e.cardId === cardId);
    if (!target || !this.canAddCard(target)) {
      return;
    }
    this.saveMessage.set(null);
    this.entries.update((current) =>
      current.map((e) => (e.cardId === cardId ? { ...e, quantity: e.quantity + 1 } : e))
    );
  }

  decrementCard(cardId: string): void {
    this.saveMessage.set(null);
    this.entries.update((current) =>
      current
        .map((e) => (e.cardId === cardId ? { ...e, quantity: e.quantity - 1 } : e))
        .filter((e) => e.quantity > 0)
    );
  }

  saveDeck(): void {
    const trimmedName = this.deckName().trim();
    if (!trimmedName) {
      this.error.set('El mazo debe tener un nombre.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.saveMessage.set(null);

    const payload = {
      name: trimmedName,
      description: this.deckDescription().trim() || null,
      cards: this.entries().map((e) => ({ cardId: e.cardId, quantity: e.quantity }))
    };

    const existingId = this.activeDeckId();
    const request$ = existingId
      ? this.api.updateDeck(existingId, payload)
      : this.api.createDeck(payload);

    request$
      .pipe(
        tap((saved) => {
          this.activeDeckId.set(saved.id);
          this.deckName.set(saved.name);
          this.deckDescription.set(saved.description ?? '');
          this.entries.set(saved.cards);
          this.saveMessage.set(
            saved.valid ? 'Mazo guardado y validado (60/60).' : 'Borrador guardado.'
          );
          if (!existingId) {
            void this.router.navigate(['/decks', saved.id], { replaceUrl: true });
          }
        }),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractErrorMessage(err, 'No se pudo guardar el mazo.'));
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe();
  }
}

function isBasicEnergyItem(card: { category: string; energyType: string | null }): boolean {
  if (card.category.trim().toLowerCase() !== 'energy') {
    return false;
  }
  const et = card.energyType?.trim().toLowerCase() ?? '';
  return et === 'basic' || et === 'normal';
}

function extractErrorMessage(err: HttpErrorResponse, fallback: string): string {
  if (err.error && typeof err.error === 'object' && 'message' in err.error && typeof err.error.message === 'string') {
    return err.error.message;
  }
  return fallback;
}
