import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CardSummary } from '../../core/models/catalog.models';
import { DeckBuilderStore } from './deck-builder.store';

describe('DeckBuilderStore', () => {
  let store: DeckBuilderStore;
  let httpMock: HttpTestingController;

  const charmander: CardSummary = {
    id: 'base1-46',
    setId: 'base1',
    localId: '46',
    sortOrder: 46,
    name: 'Charmander',
    category: 'Pokemon',
    hp: 50,
    stage: 'Basic',
    trainerType: null,
    energyType: null,
    rarity: 'Common',
    imageLowUrl: null,
    types: ['Fire']
  };

  const doubleColorless: CardSummary = {
    id: 'base1-96',
    setId: 'base1',
    localId: '96',
    sortOrder: 96,
    name: 'Double Colorless Energy',
    category: 'Energy',
    hp: null,
    stage: null,
    trainerType: null,
    energyType: 'Special',
    rarity: 'Uncommon',
    imageLowUrl: null,
    types: []
  };

  const fireEnergy: CardSummary = {
    id: 'base1-98',
    setId: 'base1',
    localId: '98',
    sortOrder: 98,
    name: 'Fire Energy',
    category: 'Energy',
    hp: null,
    stage: 'Basic',
    trainerType: null,
    energyType: 'Basic',
    rarity: 'Common',
    imageLowUrl: null,
    types: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: '**', children: [] }]), provideHttpClient(), provideHttpClientTesting()]
    });
    store = TestBed.inject(DeckBuilderStore);
    httpMock = TestBed.inject(HttpTestingController);
    store.initNewDeck();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('enforces max 4 copies for Pokemon and Special Energy while allowing unlimited Basic Energy', () => {
    for (let i = 0; i < 5; i++) {
      store.addCard(charmander);
      store.addCard(doubleColorless);
      store.addCard(fireEnergy);
    }

    expect(store.quantitiesByCardId().get('base1-46')).toBe(4);
    expect(store.quantitiesByCardId().get('base1-96')).toBe(4);
    expect(store.quantitiesByCardId().get('base1-98')).toBe(5);
    expect(store.canAddCard(charmander)).toBeFalse();
    expect(store.canAddCard(doubleColorless)).toBeFalse();
    expect(store.canAddCard(fireEnergy)).toBeTrue();
    expect(store.hasBasicPokemon()).toBeTrue();
    expect(store.isValidClient()).toBeFalse();
  });

  it('validates 60/60 deck and saves to /api/decks', () => {
    for (let i = 0; i < 4; i++) {
      store.addCard(charmander);
    }
    for (let i = 0; i < 56; i++) {
      store.addCard(fireEnergy);
    }

    expect(store.totalCards()).toBe(60);
    expect(store.hasBasicPokemon()).toBeTrue();
    expect(store.copiesLimitOk()).toBeTrue();
    expect(store.isValidClient()).toBeTrue();

    store.deckName.set('Char Test');
    store.saveDeck();

    const req = httpMock.expectOne('/api/decks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.name).toBe('Char Test');
    expect(req.request.body.cards.length).toBe(2);
    req.flush({
      id: 'd-1',
      ownerId: 'u-1',
      name: 'Char Test',
      description: null,
      starter: false,
      cpuEligible: false,
      valid: true,
      totalCards: 60,
      validation: { valid: true, errors: [] },
      cards: store.entries(),
      createdAt: '2026-11-23T00:00:00Z',
      updatedAt: '2026-11-23T00:00:00Z'
    });

    expect(store.activeDeckId()).toBe('d-1');
    expect(store.saveMessage()).toContain('60/60');
  });
});
