export interface CardSet {
  id: string;
  name: string;
  series: string | null;
  releaseDate: string | null;
  totalCards: number | null;
  logoUrl: string | null;
  syncedAt: string;
}

export interface CardAttack {
  cost: string[];
  name: string;
  effect: string | null;
  damage: string | null;
}

export interface CardTypeModifier {
  type: string;
  value: string;
}

export interface CardSummary {
  id: string;
  setId: string;
  localId: string;
  sortOrder: number;
  name: string;
  category: 'Pokemon' | 'Trainer' | 'Energy' | string;
  hp: number | null;
  stage: string | null;
  trainerType: string | null;
  energyType: string | null;
  rarity: string | null;
  imageLowUrl: string | null;
  types: string[];
}

export interface CardDetail {
  id: string;
  setId: string;
  setName: string;
  localId: string;
  sortOrder: number;
  name: string;
  category: string;
  hp: number | null;
  stage: string | null;
  evolveFrom: string | null;
  retreat: number | null;
  trainerType: string | null;
  energyType: string | null;
  rarity: string | null;
  illustrator: string | null;
  imageLowUrl: string | null;
  imageHighUrl: string | null;
  types: string[];
  attacks: CardAttack[];
  weaknesses: CardTypeModifier[];
  resistances: CardTypeModifier[];
  effectText: string | null;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CardSearchFilters {
  q?: string;
  setId?: string;
  category?: string;
  type?: string;
  stage?: string;
  page?: number;
  size?: number;
}
