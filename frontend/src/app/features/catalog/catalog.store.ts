import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, distinctUntilChanged, finalize, of, switchMap } from 'rxjs';
import { CatalogApiService } from '../../core/api/catalog-api.service';
import { CardDetail, CardSet, CardSummary } from '../../core/models/catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogStore {
  private readonly api = inject(CatalogApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchTrigger$ = new Subject<void>();
  private readonly queryDebounce$ = new Subject<string>();

  readonly sets = signal<CardSet[]>([]);
  readonly cards = signal<CardSummary[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly page = signal<number>(0);
  readonly size = signal<number>(24);

  readonly q = signal<string>('');
  readonly selectedSetId = signal<string>('base1');
  readonly selectedCategory = signal<string>('');
  readonly selectedType = signal<string>('');

  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly isModalOpen = signal<boolean>(false);
  readonly detailLoading = signal<boolean>(false);
  readonly selectedCard = signal<CardDetail | null>(null);

  readonly hasActiveFilters = computed(() =>
    Boolean(this.q().trim() || this.selectedCategory() || this.selectedType())
  );

  constructor() {
    this.queryDebounce$
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((term) => {
        this.q.set(term);
        this.page.set(0);
        this.searchTrigger$.next();
      });

    this.searchTrigger$
      .pipe(
        switchMap(() => {
          this.loading.set(true);
          this.error.set(null);
          return this.api
            .searchCards({
              q: this.q(),
              setId: this.selectedSetId(),
              category: this.selectedCategory(),
              type: this.selectedType(),
              page: this.page(),
              size: this.size()
            })
            .pipe(
              catchError(() => {
                this.error.set('No se pudo cargar el catálogo desde /api/cards.');
                return of(null);
              }),
              finalize(() => this.loading.set(false))
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res) => {
        if (!res) return;
        this.cards.set(res.items);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.page.set(res.page);
      });
  }

  initialize(): void {
    if (this.sets().length === 0) {
      this.api
        .getSets()
        .pipe(
          catchError(() => of([])),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe((sets) => {
          this.sets.set(sets);
          if (sets.length > 0 && !this.selectedSetId()) {
            this.selectedSetId.set(sets[0].id);
          }
        });
    }
    this.searchTrigger$.next();
  }

  onSearchInput(term: string): void {
    this.queryDebounce$.next(term);
  }

  setCategory(category: string): void {
    this.selectedCategory.set(category);
    this.page.set(0);
    this.searchTrigger$.next();
  }

  setType(type: string): void {
    this.selectedType.set(this.selectedType() === type ? '' : type);
    this.page.set(0);
    this.searchTrigger$.next();
  }

  setSetId(setId: string): void {
    this.selectedSetId.set(setId);
    this.page.set(0);
    this.searchTrigger$.next();
  }

  goToPage(nextPage: number): void {
    if (nextPage < 0 || (this.totalPages() > 0 && nextPage >= this.totalPages())) {
      return;
    }
    this.page.set(nextPage);
    this.searchTrigger$.next();
  }

  resetFilters(): void {
    this.q.set('');
    this.selectedCategory.set('');
    this.selectedType.set('');
    this.page.set(0);
    this.searchTrigger$.next();
  }

  openCardDetail(cardId: string): void {
    this.isModalOpen.set(true);
    this.detailLoading.set(true);
    this.selectedCard.set(null);
    this.api
      .getCardById(cardId)
      .pipe(
        catchError(() => of(null)),
        finalize(() => this.detailLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((detail) => this.selectedCard.set(detail));
  }

  closeCardDetail(): void {
    this.isModalOpen.set(false);
    this.selectedCard.set(null);
  }
}
