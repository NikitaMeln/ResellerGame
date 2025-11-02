import { Component, OnInit, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../../services/websocket.service';
import { PlayerService } from '../../services/player.service';

interface RoomState {
  roomState: string;
  clients: any[];
  playerQueue: any[];
  cars: any[];
  tunings: any[];
  startTime: string;
  phase: string;
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
  imports: [CommonModule],
  templateUrl: './game-room.component.html',
  styleUrl: './game-room.component.scss'
})
export class GameRoomComponent implements OnInit, OnDestroy {
  roomId: string = '';
  roomState: RoomState | null = null;
  turnInfo: TurnInfo | null = null;
  myTelegramId: string = '';

  availableCars: any[] = [];
  availableTunings: any[] = [];

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

    // Get initial room state from navigation state (only in browser)
    if (this.isBrowser) {
      const navigation = this.router.getCurrentNavigation();
      const initialRoomState = navigation?.extras?.state?.['initialRoomState'] ||
                              (history.state as any)?.initialRoomState;

      if (initialRoomState) {
        console.log('Using initial room state:', initialRoomState);
        this.roomState = initialRoomState;
        this.updateAvailableCards();
      }
    }

    // Subscribe to room state updates
    this.subscriptions.push(
      this.websocketService.subscribeToRoomState(roomId).subscribe({
        next: (state: RoomState) => {
          console.log('Room state update:', state);
          this.roomState = state;
          this.updateAvailableCards();
        },
        error: (error) => console.error('Room state error:', error)
      })
    );

    // Subscribe to turn updates
    this.subscriptions.push(
      this.websocketService.subscribeToRoomTurn(roomId).subscribe({
        next: (turnInfo: TurnInfo) => {
          console.log('Turn update:', turnInfo);
          this.turnInfo = turnInfo;
        },
        error: (error) => console.error('Turn info error:', error)
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private updateAvailableCards(): void {
    if (!this.roomState) return;

    // Calculate how many cards to show: players.size() + 1
    const playerCount = this.roomState.playerQueue?.length || 0;
    const cardsToShow = playerCount + 1;

    this.availableCars = this.roomState.cars?.slice(0, cardsToShow) || [];

    // Filter only POSITIVE tunings
    const positiveTunings = this.roomState.tunings?.filter(t => t.type === 'POSITIVE') || [];
    this.availableTunings = positiveTunings.slice(0, cardsToShow);
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

  buyCar(car: any): void {
    if (this.isMyTurn() && this.isCarSelection()) {
      this.websocketService.buyCar(
        parseInt(this.roomId),
        this.myTelegramId,
        car.id
      );
    }
  }

  buyTuning(tuning: any): void {
    if (this.isMyTurn() && this.isTuningSelection()) {
      this.websocketService.buyTuning(
        parseInt(this.roomId),
        this.myTelegramId,
        tuning.id
      );
    }
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
}
