import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogStore } from './catalog.store';

describe('CatalogStore', () => {
  let store: CatalogStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    store = TestBed.inject(CatalogStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads sets and paginated cards using /api endpoints only', () => {
    store.initialize();

    const setsReq = httpMock.expectOne('/api/sets');
    expect(setsReq.request.method).toBe('GET');
    setsReq.flush([
      {
        id: 'base1',
        name: 'Base Set',
        series: 'Base',
        releaseDate: '1999-01-09',
        totalCards: 102,
        logoUrl: null,
        syncedAt: '2026-11-23T00:00:00Z'
      }
    ]);

    const cardsReq = httpMock.expectOne('/api/cards?setId=base1&page=0&size=24');
    expect(cardsReq.request.method).toBe('GET');
    cardsReq.flush({
      items: [
        {
          id: 'base1-4',
          setId: 'base1',
          localId: '4',
          sortOrder: 4,
          name: 'Charizard',
          category: 'Pokemon',
          hp: 120,
          stage: 'Stage2',
          trainerType: null,
          energyType: null,
          rarity: 'Rare Holo',
          imageLowUrl: 'https://assets.tcgdex.net/en/base/base1/4/low.webp',
          types: ['Fire']
        }
      ],
      page: 0,
      size: 24,
      totalElements: 102,
      totalPages: 5
    });

    expect(store.sets().length).toBe(1);
    expect(store.cards().length).toBe(1);
    expect(store.cards()[0].name).toBe('Charizard');
    expect(store.totalElements()).toBe(102);
  });

  it('updates category filter and requests page 0', () => {
    store.setCategory('Energy');

    const req = httpMock.expectOne('/api/cards?setId=base1&category=Energy&page=0&size=24');
    expect(req.request.method).toBe('GET');
    req.flush({ items: [], page: 0, size: 24, totalElements: 7, totalPages: 1 });

    expect(store.selectedCategory()).toBe('Energy');
    expect(store.totalElements()).toBe(7);
  });
});
