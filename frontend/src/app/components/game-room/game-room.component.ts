import { Component, OnInit, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../../services/websocket.service';
import { PlayerService } from '../../services/player.service';
import { GameHeaderComponent } from '../game-header/game-header.component';
import { GameCardComponent } from '../game-card/game-card.component';
import {
  PlayerGameState,
  CarCard,
  ClientCard,
  TuningCard,
  GameCard
} from '../../models/game.models';

interface RoomState {
  roomState: string;
  clients: any[];
  playerQueue: any[];
  cars: any[];
  tunings: any[];
  startTime: string;
  currentPlayerIndex: number;
  turnStep: string;
  negativeCards: any[];
}

interface TurnInfo {
  currentPlayer: string;
  turnStep: string;
  negativeCard?: any;
}

@Component({
  selector: 'app-game-room',
  imports: [CommonModule, GameHeaderComponent, GameCardComponent],
  templateUrl: './game-room.component.html',
  styleUrl: './game-room.component.scss'
})
export class GameRoomComponent implements OnInit, OnDestroy {
  roomId: string = '';
  roomState: RoomState | null = null;
  turnInfo: TurnInfo | null = null;
  myTelegramId: string = '';

  // Player state for header
  playerState!: PlayerGameState;

  // All available cards for room (full pools initialized once)
  clientAvailableCards: (ClientCard | null)[] = [];
  marketAvailableCards: (CarCard | null)[] = [];
  tuningAvailableCards: (TuningCard | null)[] = [];

  // Zones for each round display (playerCount + 1 cards from pools)
  // These are FIXED for the round and only updated when new round starts
  clientZone: (ClientCard | null)[] = [];
  marketZone: (CarCard | null)[] = [];
  tuningZone: (TuningCard | null)[] = [];
  garageZone: (CarCard | null)[] = [];

  // Track which round zones were initialized for
  private zoneRoundSnapshot: string = '';
  private lastProcessedPlayerIndex: number = -1;
  private lastProcessedTurnStep: string = '';

  private subscriptions: Subscription[] = [];
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private websocketService: WebSocketService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    const roomId = this.route.snapshot.paramMap.get('id');
    if (!roomId) {
      this.router.navigate(['/menu']);
      return;
    }

    this.roomId = roomId;
    const playerInfo = this.playerService.getCurrentPlayerInfo();
    if (playerInfo) {
      this.myTelegramId = playerInfo.telegramId;
    }

    // Initialize player state
    this.initializePlayerState();

    if (this.isBrowser) {
      const navigation = this.router.getCurrentNavigation();
      const initialRoomState = navigation?.extras?.state?.['initialRoomState'] ||
                              (history.state as any)?.initialRoomState;

      if (initialRoomState) {
        console.log('Using initial room state:', initialRoomState);
        this.roomState = initialRoomState;
        this.initializeAvailablePools();
        this.updateZones();
      }
    }

    // Subscribe to room state updates
    this.subscriptions.push(
      this.websocketService.subscribeToRoomState(roomId).subscribe({
        next: (state: RoomState) => {
          console.log('Room state update:', state);
          this.roomState = state;

          // Initialize pools only once (when they're empty)
          if (this.clientAvailableCards.length === 0) {
            this.initializeAvailablePools();
          }

          this.updateZones();
        },
        error: (error: any) => console.error('Room state error:', error)
      })
    );

    // Subscribe to turn updates
    this.subscriptions.push(
      this.websocketService.subscribeToRoomTurn(roomId).subscribe({
        next: (turnInfo: TurnInfo) => {
          console.log('Turn update:', turnInfo);
          this.turnInfo = turnInfo;
        },
        error: (error: any) => console.error('Turn info error:', error)
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private initializePlayerState(): void {
    const playerCount = 3; // Default or get from room
    this.playerState = {
      playerId: this.myTelegramId,
      money: 1300,
      totalSales: 0,
      garageSlots: 3,
      usedGarageSlots: 0,
      clientZone: Array(playerCount + 1).fill(null),
      garageZone: Array(3).fill(null)
    };

    // Initialize empty zones
    this.clientZone = Array(playerCount + 1).fill(null);
    this.marketZone = Array(playerCount + 1).fill(null);
    this.tuningZone = Array(playerCount + 1).fill(null);
    this.garageZone = Array(3).fill(null);
  }

  /**
   * Initialize full available card pools from room state (called once)
   */
  private initializeAvailablePools(): void {
    if (!this.roomState) return;

    console.log('Initializing available pools from room state');

    // Initialize full client pool
    if (this.roomState.clients) {
      this.clientAvailableCards = this.roomState.clients.map((client, index) => ({
        id: client.id?.toString() || `client-${index}`,
        type: 'client' as const,
        title: client.name || `Client ${index + 1}`,
        description: client.description || '',
        stats: {
          budget: client.budget || 0,
          modifier: client.randomCounter,
          year: client.yearForPurchase
        },
        isRevealed: true,
        isDraggable: false
      }));
      console.log(`Initialized ${this.clientAvailableCards.length} clients in pool`);
    }

    // Initialize full car market pool
    if (this.roomState.cars) {
      this.marketAvailableCards = this.roomState.cars.map((car, index) => ({
        id: car.id?.toString() || `car-${index}`,
        type: 'car' as const,
        title: car.model || 'Unknown',
        description: `Year: ${car.year || 'N/A'}`,
        stats: {
          brand: car.model?.split(' ')[0] || 'Unknown',
          model: car.model || 'Unknown',
          year: parseInt(car.year) || 2020,
          price: car.price || 0
        },
        isRevealed: true,
        isDraggable: false // Will be set per zone
      }));
      console.log(`Initialized ${this.marketAvailableCards.length} cars in pool`);
    }

    // Initialize full tuning pool (only POSITIVE)
    if (this.roomState.tunings) {
      const positiveTunings = this.roomState.tunings.filter((t: any) => t.type === 'POSITIVE');
      this.tuningAvailableCards = positiveTunings.map((tuning, index) => ({
        id: tuning.id?.toString() || `tuning-${index}`,
        type: 'tuning' as const,
        title: tuning.name || `Tuning ${index + 1}`,
        description: tuning.description || '',
        stats: {
          price: tuning.price || 0,
          category: tuning.type || 'POSITIVE',
          effect: tuning.description || ''
        },
        isRevealed: true,
        isDraggable: false // Will be set per zone
      }));
      console.log(`Initialized ${this.tuningAvailableCards.length} tunings in pool`);
    }
  }

  /**
   * Update zones - either initialize new round or update existing cards
   * Zones are FIXED per round and only refresh when new round starts
   */
  private updateZones(): void {
    if (!this.roomState) return;

    const playerCount = this.roomState.playerQueue?.length || 0;
    const cardsToShow = playerCount + 1;

    // Detect new round: when we're back at CAR_SELECTION with player index 0
    // This means all players completed all turn steps (CAR_SELECTION → RESULT)
    const currentPlayerIndex = this.roomState.currentPlayerIndex ?? 0;
    const currentTurnStep = this.roomState.turnStep || '';

    console.log(`Update zones - Turn step: ${currentTurnStep}, Player index: ${currentPlayerIndex}`);
    console.log(`Update zones - Last processed index: ${this.lastProcessedPlayerIndex}`);
    console.log(`Update zones - Previous snapshot: ${this.zoneRoundSnapshot}`);

    // Check if we need to initialize new round zones
    // First time: zoneRoundSnapshot is empty
    // New round: back to CAR_SELECTION AND we were at RESULT before (cycle completed)
    const isFirstTime = this.zoneRoundSnapshot === '';
    const isNewRound =
      currentTurnStep === 'CAR_SELECTION' &&
      currentPlayerIndex === 0 &&
      this.lastProcessedTurnStep === 'RESULT';
    const shouldRefreshZones = isFirstTime || isNewRound;

    console.log(`Is first time: ${isFirstTime}, Is new round: ${isNewRound}, Should refresh: ${shouldRefreshZones}`);

    if (shouldRefreshZones) {
      console.log('🔄 Initializing NEW round zones');
      this.initializeRoundZones(cardsToShow);
      this.zoneRoundSnapshot = `round-${currentPlayerIndex}`;
    } else {
      console.log('✅ Updating EXISTING round zones (removing purchased cards)');
      this.updateExistingZones();
    }

    // Update last processed state
    this.lastProcessedPlayerIndex = currentPlayerIndex;
    this.lastProcessedTurnStep = currentTurnStep;

    // Always update player state and garage
    this.updatePlayerStateAndGarage();
  }

  /**
   * Initialize zones for a new round with fresh cards from pools
   */
  private initializeRoundZones(cardsToShow: number): void {
    if (!this.roomState) return;

    console.log(`Initializing round zones with ${cardsToShow} cards`);

    // Get available IDs from room state
    const availableCarIds = new Set(this.roomState.cars?.map((c: any) => c.id?.toString()) || []);
    const availableClientIds = new Set(this.roomState.clients?.map((c: any) => c.id?.toString()) || []);
    const availableTuningIds = new Set(
      this.roomState.tunings?.filter((t: any) => t.type === 'POSITIVE').map((t: any) => t.id?.toString()) || []
    );

    // Initialize client zone
    this.clientZone = this.clientAvailableCards
      .filter(card => card && availableClientIds.has(card.id))
      .slice(0, cardsToShow)
      .map(card => card ? { ...card } : null);

    // Initialize market zone
    this.marketZone = this.marketAvailableCards
      .filter(card => card && availableCarIds.has(card.id))
      .slice(0, cardsToShow)
      .map(card => card ? {
        ...card,
        isDraggable: true // Will be updated based on turn
      } : null);

    // Initialize tuning zone
    const currentTurnStep = this.roomState?.turnStep || '';
    const canDragTuning = currentTurnStep === 'TUNING_SELECTION';

    this.tuningZone = this.tuningAvailableCards
      .filter(card => card && availableTuningIds.has(card.id))
      .slice(0, cardsToShow)
      .map(card => card ? {
        ...card,
        isDraggable: canDragTuning // Set based on current turn step
      } : null);

    console.log(`✅ Round zones initialized - Client: ${this.clientZone.length}, Market: ${this.marketZone.length}, Tuning: ${this.tuningZone.length}, TuningDraggable: ${canDragTuning}`);
  }

  /**
   * Update existing zones by removing purchased/used cards (replace with null)
   */
  private updateExistingZones(): void {
    if (!this.roomState) return;

    // Get currently available IDs from room state
    const availableCarIds = new Set(this.roomState.cars?.map((c: any) => c.id?.toString()) || []);
    const availableClientIds = new Set(this.roomState.clients?.map((c: any) => c.id?.toString()) || []);
    const availableTuningIds = new Set(
      this.roomState.tunings?.filter((t: any) => t.type === 'POSITIVE').map((t: any) => t.id?.toString()) || []
    );

    // Update client zone - replace purchased clients with null
    this.clientZone = this.clientZone.map(card => {
      if (!card) return null;
      return availableClientIds.has(card.id) ? card : null;
    });

    // Update market zone - replace purchased cars with null
    this.marketZone = this.marketZone.map(card => {
      if (!card) return null;
      const isAvailable = availableCarIds.has(card.id);
      console.log(`Car ${card.id} (${card.title}) - Still available: ${isAvailable}`);
      return isAvailable ? { ...card, isDraggable: true } : null;
    });

    // Update tuning zone - replace purchased tunings with null
    // Use roomState directly instead of turnInfo to avoid sync issues
    const currentTurnStep = this.roomState?.turnStep || '';
    const currentPlayerIndex = this.roomState?.currentPlayerIndex ?? -1;
    const currentPlayerTelegramId = this.roomState?.playerQueue?.[currentPlayerIndex]?.telegramId;
    const isMyTurnNow = currentPlayerTelegramId === this.myTelegramId;
    const isTuningPhase = currentTurnStep === 'TUNING_SELECTION';
    const canDrag = isMyTurnNow && isTuningPhase;

    this.tuningZone = this.tuningZone.map(card => {
      if (!card) return null;
      const isAvailable = availableTuningIds.has(card.id);
      console.log(`Tuning ${card.id} (${card.title}) - Still available: ${isAvailable}, Can drag: ${canDrag}, MyTurn: ${isMyTurnNow}, Phase: ${currentTurnStep}`);
      return isAvailable ? { ...card, isDraggable: canDrag } : null;
    });

    console.log(`✅ Existing zones updated - Market: ${this.marketZone.filter(c => c !== null).length} cards, Tuning: ${this.tuningZone.filter(c => c !== null).length} cards remaining`);
  }

  /**
   * Update player state and garage zone from current player data
   */
  private updatePlayerStateAndGarage(): void {
    if (!this.roomState) return;

    const currentPlayer = this.roomState.playerQueue?.find((p: any) => p.telegramId === this.myTelegramId);
    if (currentPlayer) {
      console.log('Current player data:', currentPlayer);
      console.log('Current player cars:', currentPlayer.cars);

      this.playerState.money = currentPlayer.balance || 1300;
      this.playerState.totalSales = currentPlayer.soldCars || 0;
      this.playerState.usedGarageSlots = currentPlayer.cars?.length || 0;

      // Update garage zone with player's cars
      if (currentPlayer.cars && currentPlayer.cars.length > 0) {
        console.log('Updating garage with cars:', currentPlayer.cars);
        this.garageZone = currentPlayer.cars.map((car: any, index: number) => ({
          id: car.id?.toString() || `garage-car-${index}`,
          type: 'car' as const,
          title: car.model || 'Unknown',
          description: `Year: ${car.year || 'N/A'}`,
          stats: {
            brand: car.model?.split(' ')[0] || 'Unknown',
            model: car.model || 'Unknown',
            year: parseInt(car.year) || 2020,
            price: car.price || 0,
            tuning: car.tuning || []
          },
          isRevealed: true,
          isDraggable: false
        }));

        // Fill remaining slots with null
        while (this.garageZone.length < this.playerState.garageSlots) {
          this.garageZone.push(null);
        }
        console.log('Updated garageZone:', this.garageZone);
      } else {
        console.log('No cars, filling with empty slots');
        // No cars - fill with empty slots
        this.garageZone = Array(this.playerState.garageSlots).fill(null);
      }
    }
  }

  isPending(): boolean {
    return this.roomState?.roomState === 'PENDING';
  }

  isStarted(): boolean {
    return this.roomState?.roomState === 'STARTED';
  }

  isMyTurn(): boolean {
    return this.turnInfo?.currentPlayer === this.myTelegramId;
  }

  isCarSelection(): boolean {
    return this.turnInfo?.turnStep === 'CAR_SELECTION';
  }

  isTuningSelection(): boolean {
    return this.turnInfo?.turnStep === 'TUNING_SELECTION';
  }

  canStartGame(): boolean {
    return this.isPending() && (this.roomState?.playerQueue?.length || 0) >= 1;
  }

  startGame(): void {
    if (this.canStartGame()) {
      this.websocketService.startGame(this.roomId);
    }
  }

  onCarClick(_car: CarCard): void {
    // Disabled: Only drag-and-drop to buy cars
    console.log('Car click disabled - use drag and drop to garage');
  }

  onTuningClick(_tuning: TuningCard): void {
    // Disabled: Only drag-and-drop to garage cars to buy tuning
    console.log('Tuning click disabled - use drag and drop to garage car');
  }

  onClientClick(client: ClientCard): void {
    // TODO: Implement client selection when needed
    console.log('Client clicked:', client);
  }

  skipTurn(): void {
    if (this.isMyTurn()) {
      this.websocketService.skipAction(
        parseInt(this.roomId),
        this.myTelegramId
      );
    }
  }

  getCurrentPlayerName(): string {
    if (!this.roomState || !this.turnInfo) return '';
    const currentPlayer = this.roomState.playerQueue?.find(
      p => p.telegramId === this.turnInfo?.currentPlayer
    );
    return currentPlayer?.username || '';
  }

  getPlayerCount(): number {
    return this.roomState?.playerQueue?.length || 0;
  }

  getPhaseText(): string {
    if (this.isPending()) return 'Waiting for players...';
    if (this.isCarSelection()) return 'Car Selection Phase';
    if (this.isTuningSelection()) return 'Tuning Selection Phase';
    return 'Game in progress';
  }

  // Drag and drop handlers for car buying
  private draggedCarId: string | null = null;
  isDraggingCar: boolean = false;

  // Drag and drop handlers for tuning buying
  private draggedTuningId: string | null = null;
  isDraggingTuning: boolean = false;
  private hoveredGarageCarId: string | null = null;

  onCarDragStart(card: GameCard): void {
    if (this.isMyTurn() && this.isCarSelection() && card.type === 'car') {
      this.draggedCarId = card.id;
      this.isDraggingCar = true;
      console.log('Started dragging car:', card.id);
    }
  }

  onCarDragEnd(card: GameCard): void {
    this.draggedCarId = null;
    this.isDraggingCar = false;
    console.log('Stopped dragging car:', card.id);
  }

  onGarageDragOver(event: DragEvent): void {
    if (this.isMyTurn() && this.isCarSelection() && this.draggedCarId) {
      event.preventDefault(); // Allow drop
      event.dataTransfer!.dropEffect = 'move';
    }
  }

  onGarageDragLeave(event: DragEvent): void {
    // Only reset if leaving the entire garage zone
    const target = event.relatedTarget as HTMLElement;
    if (!target || !target.closest('.garage-slots-zone')) {
      // Visual feedback handled by CSS
    }
  }

  onGarageDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDraggingCar = false;

    if (!this.isMyTurn() || !this.isCarSelection()) {
      console.log('Not your turn or not car selection phase');
      return;
    }

    const carId = event.dataTransfer?.getData('cardId') || this.draggedCarId;
    if (carId) {
      console.log('Buying car with ID:', carId);
      this.websocketService.buyCar(
        parseInt(this.roomId),
        this.myTelegramId,
        parseInt(carId)
      );
    }

    this.draggedCarId = null;
  }

  // Drag and drop handlers for tuning buying
  onTuningDragStart(card: GameCard): void {
    console.log('onTuningDragStart called:', {
      cardId: card.id,
      cardType: card.type,
      isDraggable: card.isDraggable,
      isMyTurn: this.isMyTurn(),
      isTuningSelection: this.isTuningSelection(),
      turnInfo: this.turnInfo,
      roomState: this.roomState?.turnStep
    });

    if (this.isMyTurn() && this.isTuningSelection() && card.type === 'tuning') {
      this.draggedTuningId = card.id;
      this.isDraggingTuning = true;
      console.log('✅ Started dragging tuning:', card.id);
    } else {
      console.warn('❌ Cannot drag tuning - conditions not met');
    }
  }

  onTuningDragEnd(card: GameCard): void {
    this.draggedTuningId = null;
    this.isDraggingTuning = false;
    this.hoveredGarageCarId = null;
    console.log('Stopped dragging tuning:', card.id);
  }

  onGarageCarDragOver(event: DragEvent, garageCarId: string): void {
    if (this.isMyTurn() && this.isTuningSelection() && this.draggedTuningId) {
      event.preventDefault(); // Allow drop
      event.dataTransfer!.dropEffect = 'move';
      this.hoveredGarageCarId = garageCarId;
    }
  }

  onGarageCarDragLeave(event: DragEvent, _garageCarId: string): void {
    const target = event.relatedTarget as HTMLElement;
    // Reset hover state when leaving the card
    if (!target || !target.closest('.garage-car-slot')) {
      this.hoveredGarageCarId = null;
    }
  }

  onGarageCarDrop(event: DragEvent, garageCarId: string): void {
    event.preventDefault();
    this.isDraggingTuning = false;
    this.hoveredGarageCarId = null;

    if (!this.isMyTurn() || !this.isTuningSelection()) {
      console.log('Not your turn or not tuning selection phase');
      return;
    }

    const tuningId = event.dataTransfer?.getData('cardId') || this.draggedTuningId;
    if (tuningId && garageCarId) {
      console.log(`Buying tuning ${tuningId} for car ${garageCarId}`);
      this.websocketService.buyTuning(
        parseInt(this.roomId),
        this.myTelegramId,
        parseInt(tuningId),
        parseInt(garageCarId)
      );
    }

    this.draggedTuningId = null;
  }

  isGarageCarHovered(carId: string): boolean {
    return this.hoveredGarageCarId === carId && this.isDraggingTuning;
  }
}
