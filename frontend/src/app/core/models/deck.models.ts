export interface DeckValidationResult {
  valid: boolean;
  errors: string[];
}

export interface DeckSummary {
  id: string;
  name: string;
  description: string | null;
  starter: boolean;
  cpuEligible: boolean;
  valid: boolean;
  totalCards: number;
  createdAt: string;
  updatedAt: string;
}

export interface DeckCardItem {
  cardId: string;
  localId: string;
  sortOrder: number;
  name: string;
  category: string;
  stage: string | null;
  trainerType: string | null;
  energyType: string | null;
  imageLowUrl: string | null;
  types: string[];
  quantity: number;
}

export interface DeckDetail {
  id: string;
  ownerId: string | null;
  name: string;
  description: string | null;
  starter: boolean;
  cpuEligible: boolean;
  valid: boolean;
  totalCards: number;
  validation: DeckValidationResult;
  cards: DeckCardItem[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveDeckCardRequest {
  cardId: string;
  quantity: number;
}

export interface SaveDeckRequest {
  name: string;
  description: string | null;
  cards: SaveDeckCardRequest[];
}
