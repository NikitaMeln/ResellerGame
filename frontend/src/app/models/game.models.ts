export interface Player {
  id: string;
  name: string;
  money: number;
  cars: Car[];
  isCurrentPlayer?: boolean;
}

export interface Car {
  id: string;
  brand: string;
  model: string;
  year: number;
  price: number;
  tunings: Tuning[];
  isOwned?: boolean;
}

export interface Tuning {
  id: number;
  name: string;
  description?: string;
  price: number;
  type: string; // POSITIVE or NEGATIVE
  properties?: any; // Additional tuning properties
}

export interface Client {
  id: string;
  name: string;
  description: string;
  requirements: string[];
  modifier: number;
  maxPrice: number;
}

export interface GameRoom {
  id: string;
  name: string;
  players: Player[];
  currentPhase: GamePhase;
  status: GameStatus;
  maxPlayers: number;
  currentPlayerIndex: number;
  round: number;
}

export enum GamePhase {
  WAITING = 'WAITING',
  CAR_BUYING = 'CAR_BUYING',
  CAR_TUNING = 'CAR_TUNING',
  CLIENT_SELECTION = 'CLIENT_SELECTION',
  SELLING = 'SELLING',
  FINISHED = 'FINISHED'
}

export enum GameStatus {
  PENDING = 'PENDING',
  PLAYING = 'PLAYING',
  FINISHED = 'FINISHED'
}

export interface GameAction {
  type: GameActionType;
  playerId: string;
  data: any;
}

export enum GameActionType {
  JOIN_ROOM = 'JOIN_ROOM',
  LEAVE_ROOM = 'LEAVE_ROOM',
  BUY_CAR = 'BUY_CAR',
  APPLY_TUNING = 'APPLY_TUNING',
  SELECT_CLIENT = 'SELECT_CLIENT',
  SELL_CAR = 'SELL_CAR'
}

export interface SaleResult {
  success: boolean;
  diceRoll: number;
  requiredRoll: number;
  profit: number;
  clientId: string;
  carId: string;
}

// Card Types (using string literals as per requirements)
export enum CardType {
  CAR = 'car',
  CLIENT = 'client',
  TUNING = 'tuning',
  DEBUFF = 'debuff',
}

// Base interface for all cards (matches spec format)
export interface GameCard {
  id: string;
  type: 'client' | 'car' | 'tuning' | 'debuff';
  title: string;
  image?: string;
  description?: string;
  stats?: any; // Use any for flexibility, specific types defined in extended interfaces
  isRevealed?: boolean;
  isSelected?: boolean;
  isDraggable?: boolean;
}

// Card for Car type
export interface CarCard extends GameCard {
  type: 'car';
  stats: {
    brand: string;
    model: string;
    year: number;
    price: number;
    tuning?: Tuning[]; // Optional array of tunings applied to this car
  };
}

// Card for Client type
export interface ClientCard extends GameCard {
  type: 'client';
  stats: {
    budget: number;
    modifier?: number;
    year?: string | number;
    requirements?: string[];
  };
}

// Card for Tuning type
export interface TuningCard extends GameCard {
  type: 'tuning';
  stats: {
    price: number;
    category: string;
    effect: string;
  };
}

// Card for Debuff type
export interface DebuffCard extends GameCard {
  type: 'debuff';
  stats: {
    effect: number;
    isPositive: boolean;
  };
}

// Legacy type alias for backward compatibility
export type BuffDebuffCard = DebuffCard;

// Deck of cards (stack with expand functionality)
export interface CardDeck {
  id: string;
  type: CardType;
  cards: GameCard[];
  isExpanded: boolean;
}

// Legacy CardStack interface for backward compatibility
export interface CardStack {
  id: string;
  type: CardType;
  cards: GameCard[];
  isExpanded: boolean;
  position: {
    x: number;
    y: number;
  };
}

// Zone types for drag and drop
export enum ZoneType {
  DECK = 'deck',
  CLIENTS = 'clients',
  GARAGE = 'garage'
}

// Player game state
export interface PlayerGameState {
  playerId: string;
  money: number;
  totalSales: number;
  garageSlots: number;
  usedGarageSlots: number;
  clientZone: (ClientCard | null)[];  // 3 slots
  garageZone: (CarCard | null)[];     // 3 slots
}

// Game Board configuration for first round
export interface FirstRoundBoard {
  playerState: PlayerGameState;
  clientDeck: CardDeck;
  carDeck: CardDeck;
  tuningDeck: CardDeck;
  debuffDeck: CardDeck;
}