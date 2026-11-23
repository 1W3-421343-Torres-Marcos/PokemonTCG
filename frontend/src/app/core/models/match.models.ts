import { CardDetail } from './catalog.models';

export type PlayerId = 'PLAYER' | 'CPU';

export type TurnPhase =
  | 'SETUP'
  | 'DRAW_PHASE'
  | 'MAIN_PHASE'
  | 'ATTACK_PHASE'
  | 'POKEMON_CHECKUP'
  | 'GAME_OVER';

export type WinReason = 'ALL_PRIZES_TAKEN' | 'NO_POKEMON_LEFT' | 'DECK_OUT' | 'CONCEDED';

export type BotDifficulty = 'BASIC' | 'TACTICAL';

export type MatchStatus = 'IN_PROGRESS' | 'FINISHED';

export type GameEventType =
  | 'GAME_STARTED'
  | 'MULLIGAN'
  | 'SETUP_COMPLETED'
  | 'TURN_STARTED'
  | 'CARD_DRAWN'
  | 'POKEMON_BENCHED'
  | 'ENERGY_ATTACHED'
  | 'POKEMON_EVOLVED'
  | 'TRAINER_PLAYED'
  | 'ATTACK_DECLARED'
  | 'COIN_FLIPPED'
  | 'DAMAGE_DEALT'
  | 'STATUS_APPLIED'
  | 'CHECKUP_TICK'
  | 'POKEMON_KNOCKED_OUT'
  | 'PRIZE_TAKEN'
  | 'POKEMON_RETREATED'
  | 'ACTIVE_PROMOTED'
  | 'TURN_ENDED'
  | 'GAME_ENDED';

export interface AttackViewDTO {
  index: number;
  name: string;
  cost: string[];
  baseDamage: number;
  rawDamage: string | null;
  text: string | null;
  canPay: boolean;
}

export interface TypeModifierDTO {
  type: string;
  value: string;
}

export interface CardViewDTO {
  instanceId: string;
  cardId: string;
  name: string;
  category: string;
  stage: string | null;
  evolveFrom: string | null;
  hp: number | null;
  types: string[];
  attacks: AttackViewDTO[];
  weaknesses: TypeModifierDTO[];
  resistances: TypeModifierDTO[];
  retreatCost: number;
  trainerType: string | null;
  energyType: string | null;
  providedEnergyType: string | null;
  providedEnergyCount: number;
  imageLowUrl: string | null;
  imageHighUrl: string | null;
}

export interface PokemonViewDTO {
  instanceId: string;
  activeCard: CardViewDTO;
  maxHp: number;
  remainingHp: number;
  damage: number;
  attachedEnergies: CardViewDTO[];
  statusConditions: string[];
  attacks: AttackViewDTO[];
  turnEnteredPlay: number;
  turnLastEvolved: number;
}

export interface OpponentViewDTO {
  playerId: PlayerId;
  handCount: number;
  deckCount: number;
  prizesRemaining: number;
  active: PokemonViewDTO | null;
  bench: PokemonViewDTO[];
  discardPile: CardViewDTO[];
}

export interface PlayerViewDTO {
  viewer: PlayerId;
  turnNumber: number;
  activePlayer: PlayerId;
  startingPlayer: PlayerId;
  phase: TurnPhase;
  awaitingPromotionPlayer: PlayerId | null;
  energyAttachedThisTurn: boolean;
  retreatedThisTurn: boolean;
  canAttackThisTurn: boolean;
  mySetupReady: boolean;
  opponentSetupReady: boolean;
  winner: PlayerId | null;
  winReason: WinReason | null;
  myHand: CardViewDTO[];
  myActive: PokemonViewDTO | null;
  myBench: PokemonViewDTO[];
  myDiscard: CardViewDTO[];
  myDeckCount: number;
  myPrizesRemaining: number;
  opponent: OpponentViewDTO;
}

export interface GameEvent {
  sequence: number;
  type: GameEventType;
  actor: PlayerId;
  message: string;
  metadata: Record<string, unknown>;
}

export interface ActiveMatchSummary {
  matchId: string;
  playerDeckName: string;
  cpuDeckName: string;
  botDifficulty: BotDifficulty;
  turnNumber: number;
  phase: TurnPhase;
  updatedAt: string;
}

export interface MatchTurnResponse {
  matchId: string;
  status: MatchStatus;
  botDifficulty: BotDifficulty;
  playerDeckName: string;
  cpuDeckName: string;
  playerView: PlayerViewDTO;
  events: GameEvent[];
}

export interface CreateMatchRequest {
  playerDeckId: string;
  cpuDeckId: string;
  botDifficulty: BotDifficulty;
}

export type MatchActionType =
  | 'SELECT_SETUP'
  | 'PLAY_BASIC'
  | 'ATTACH_ENERGY'
  | 'ATTACK'
  | 'PROMOTE_ACTIVE'
  | 'END_TURN'
  | 'CONCEDE';

export interface MatchActionRequest {
  type: MatchActionType;
  activeInstanceId?: string;
  benchInstanceIds?: string[];
  handInstanceId?: string;
  energyInstanceId?: string;
  targetPokemonInstanceId?: string;
  attackIndex?: number;
  benchInstanceId?: string;
}

export function toCardDetailFromView(card: CardViewDTO): CardDetail {
  const parts = card.cardId.split('-');
  return {
    id: card.cardId,
    setId: parts[0] || 'base1',
    setName: 'Base Set',
    localId: parts[1] || '',
    sortOrder: 0,
    name: card.name,
    category: card.category,
    hp: card.hp,
    stage: card.stage,
    evolveFrom: card.evolveFrom,
    retreat: card.retreatCost,
    trainerType: card.trainerType,
    energyType: card.energyType,
    rarity: null,
    illustrator: null,
    imageLowUrl: card.imageLowUrl,
    imageHighUrl: card.imageHighUrl,
    types: card.types,
    attacks: card.attacks.map((atk) => ({
      cost: atk.cost,
      name: atk.name,
      effect: atk.text,
      damage: atk.rawDamage
    })),
    weaknesses: card.weaknesses,
    resistances: card.resistances,
    effectText: null
  };
}
