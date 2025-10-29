import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameHeaderComponent } from '../game-header/game-header.component';
import { GameCardComponent } from '../game-card/game-card.component';
import {
  PlayerGameState,
  CardDeck,
  GameCard,
  ClientCard,
  CarCard
} from '../../models/game.models';

@Component({
  selector: 'app-first-round-board',
  imports: [CommonModule, GameHeaderComponent, GameCardComponent],
  templateUrl: './first-round-board.component.html',
  styleUrl: './first-round-board.component.scss'
})
export class FirstRoundBoardComponent implements OnInit {
  playerState!: PlayerGameState;
  numberOfPlayers: number = 3; // Default number of players

  // Zones
  clientZone: (ClientCard | null)[] = [];
  marketZone: (CarCard | null)[] = [];
  tuningZone: (GameCard | null)[] = [];
  garageZone: (CarCard | null)[] = [];

  // Decks
  clientDeck: CardDeck | null = null;
  carDeck: CardDeck | null = null;
  tuningDeck: CardDeck | null = null;
  debuffDeck: CardDeck | null = null;

  // Drag and drop state
  draggedCard: GameCard | null = null;
  draggedFromDeck: string | null = null;

  ngOnInit(): void {
    this.initializeDemoData();
  }

  initializeDemoData(): void {
    // Calculate number of clients and cars based on number of players
    const numberOfClients = this.numberOfPlayers + 1;
    const numberOfMarketCars = this.numberOfPlayers + 1;
    const numberOfTuningSlots = numberOfMarketCars;
    const numberOfGarageSlots = 3;

    // Initialize zones with calculated number of slots
    this.clientZone = Array(numberOfClients).fill(null);
    this.marketZone = Array(numberOfMarketCars).fill(null);
    this.tuningZone = Array(numberOfTuningSlots).fill(null);
    this.garageZone = Array(numberOfGarageSlots).fill(null);

    // Initialize player state
    this.playerState = {
      playerId: 'player-1',
      money: 50000,
      totalSales: 0,
      garageSlots: numberOfGarageSlots,
      usedGarageSlots: 0,
      clientZone: this.clientZone,
      garageZone: this.garageZone
    };

    // Generate cards
    const clientCards = this.generateClientCards(numberOfClients);
    const marketCars = this.generateCarCards(numberOfMarketCars, 'market');
    const tuningCards = this.generateTuningCards(numberOfTuningSlots);

    // Place cards directly in zones (garage starts empty)
    this.clientZone = clientCards.map(card => ({ ...card, isRevealed: true }));
    this.marketZone = marketCars.map(card => ({ ...card, isRevealed: true }));
    this.tuningZone = tuningCards.map(card => ({ ...card, isRevealed: true }));

    // Initialize empty decks - all null for first round demo
    this.clientDeck = null;
    this.carDeck = null;
    this.tuningDeck = null;
    this.debuffDeck = null;
  }

  // Helper methods to generate demo cards
  private generateClientCards(count: number): ClientCard[] {
    const cards: ClientCard[] = [];
    for (let i = 1; i <= count; i++) {
      cards.push({
        id: `client-${i}`,
        type: 'client',
        title: `Client ${i}`,
        description: 'description',
        stats: {
          budget: 30000 + (i * 5000),
          modifier: i % 2 === 0 ? 1 : -1
        },
        isRevealed: false,
        isDraggable: true
      });
    }
    return cards;
  }

  private generateCarCards(count: number, prefix: string = 'car'): CarCard[] {
    const brands = ['BMW', 'Toyota', 'Mercedes', 'Audi', 'VW', 'Honda'];
    const models = ['320i', 'Camry', 'E-Class', 'A4', 'Golf', 'Civic'];
    const cards: CarCard[] = [];
    for (let i = 1; i <= count; i++) {
      const brandIndex = (i - 1) % brands.length;
      cards.push({
        id: `${prefix}-${i}`,
        type: 'car',
        title: `${brands[brandIndex]}`,
        description: 'Отличное состояние',
        stats: {
          brand: brands[brandIndex],
          model: models[brandIndex],
          year: 2020,
          price: 25000 + (i * 5000)
        },
        isRevealed: false,
        isDraggable: true
      });
    }
    return cards;
  }

  private generateTuningCards(count: number): GameCard[] {
    const cards: GameCard[] = [];
    for (let i = 1; i <= count; i++) {
      cards.push({
        id: `tuning-${i}`,
        type: 'tuning',
        title: `name ${i}`,
        description: 'Tuning part description',
        stats: {
          price: 3000,
          category: 'Exterior',
          effect: '+1 to appeal'
        },
        isRevealed: false,
        isDraggable: true
      });
    }
    return cards;
  }

  private generateDebuffCards(count: number): GameCard[] {
    const cards: GameCard[] = [];
    for (let i = 1; i <= count; i++) {
      cards.push({
        id: `debuff-${i}`,
        type: 'debuff',
        title: i % 2 === 0 ? 'Удача' : 'Проблема',
        description: i % 2 === 0 ? 'Вам повезло!' : 'Неудача',
        stats: {
          effect: i % 2 === 0 ? 2 : -1,
          isPositive: i % 2 === 0
        },
        isRevealed: false,
        isDraggable: true
      });
    }
    return cards;
  }

  // Deck interactions
  toggleDeck(deck: CardDeck): void {
    deck.isExpanded = !deck.isExpanded;

    // Close other decks
    [this.clientDeck, this.carDeck, this.tuningDeck, this.debuffDeck].forEach(d => {
      if (d && d.id !== deck.id) {
        d.isExpanded = false;
      }
    });
  }

  // Drag and drop handlers
  onDragStart(card: GameCard, deckId: string): void {
    this.draggedCard = card;
    this.draggedFromDeck = deckId;
  }

  onDragEnd(): void {
    this.draggedCard = null;
    this.draggedFromDeck = null;
  }

  onDropToGarage(index: number, event: DragEvent): void {
    event.preventDefault();
    if (this.draggedCard && this.draggedCard.type === 'car') {
      this.garageZone[index] = this.draggedCard as CarCard;
      this.removeCardFromDeck(this.draggedCard.id);
      this.playerState.usedGarageSlots++;
    }
  }

  onDropToMarket(index: number, event: DragEvent): void {
    event.preventDefault();
    if (this.draggedCard && this.draggedCard.type === 'car') {
      this.marketZone[index] = this.draggedCard as CarCard;
      this.removeCardFromDeck(this.draggedCard.id);
    }
  }

  onDropToClients(index: number, event: DragEvent): void {
    event.preventDefault();
    if (this.draggedCard && this.draggedCard.type === 'client') {
      this.clientZone[index] = this.draggedCard as ClientCard;
      this.removeCardFromDeck(this.draggedCard.id);
    }
  }

  onDropToTuning(index: number, event: DragEvent): void {
    event.preventDefault();
    if (this.draggedCard && this.draggedCard.type === 'tuning') {
      this.tuningZone[index] = this.draggedCard;
      this.removeCardFromDeck(this.draggedCard.id);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  private removeCardFromDeck(cardId: string): void {
    const decks = [this.clientDeck, this.carDeck, this.tuningDeck, this.debuffDeck];
    decks.forEach(deck => {
      if (deck) {
        const index = deck.cards.findIndex(c => c.id === cardId);
        if (index !== -1) {
          deck.cards.splice(index, 1);
        }
      }
    });
  }

  // Helper methods
  getTopCard(deck: CardDeck): GameCard | null {
    return deck.cards.length > 0 ? deck.cards[deck.cards.length - 1] : null;
  }
}
