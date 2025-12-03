import { Component, OnInit, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { WebSocketService } from '../../services/websocket.service';
import { Subscription, interval } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-lobby',
  imports: [CommonModule],
  templateUrl: './lobby.component.html',
  styleUrl: './lobby.component.scss'
})
export class LobbyComponent implements OnInit, OnDestroy {
  telegramId: string = '';
  username: string = '';
  waitingTime: number = 10;
  private timerSubscription?: Subscription;
  private roomSubscription?: Subscription;
  private roomId: string | null = null;
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  constructor(
    private router: Router,
    private websocketService: WebSocketService
  ) {
    const navigation = this.router.getCurrentNavigation();
    console.log('Lobby constructor - navigation:', navigation);
    const state = navigation?.extras?.state as { telegramId: string, username: string };
    console.log('Lobby constructor - state from navigation:', state);

    if (state?.telegramId && state?.username) {
      this.telegramId = state.telegramId;
      this.username = state.username;
      console.log('Lobby constructor - set from navigation state');
    } else if (this.isBrowser) {
      // Try history.state (works after page refresh or SSR) - only in browser
      const historyState = history.state as { telegramId: string, username: string };
      console.log('Lobby constructor - history.state:', historyState);

      if (historyState?.telegramId && historyState?.username) {
        this.telegramId = historyState.telegramId;
        this.username = historyState.username;
        console.log('Lobby constructor - set from history.state');
      } else {
        console.error('Lobby constructor - no state found, redirecting to menu');
        this.router.navigate(['/menu']);
      }
    } else {
      // SSR fallback - redirect to menu
      console.log('Lobby constructor - SSR detected, redirecting to menu');
      this.router.navigate(['/menu']);
    }
  }

  ngOnInit(): void {
    console.log('Lobby ngOnInit - telegramId:', this.telegramId, 'username:', this.username);

    if (!this.telegramId || !this.username) {
      console.error('Missing telegramId or username, redirecting to menu');
      this.router.navigate(['/menu']);
      return;
    }

    // Connect to WebSocket
    this.websocketService.connect();

    // Subscribe to room assignment
    this.subscribeToRoomAssignment();

    // Wait for connection and send join request
    setTimeout(() => {
      console.log('Sending join request...', {
        telegramId: this.telegramId,
        username: this.username,
        language: 'eng',
        roomId: null
      });
      this.websocketService.publish('/app/room.join', {
        telegramId: this.telegramId,
        username: this.username,
        language: 'eng',
        roomId: null
      });
    }, 1000);

    // Start 10-second timer
    this.startTimer();
  }

  ngOnDestroy(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
    if (this.roomSubscription) {
      this.roomSubscription.unsubscribe();
    }
  }

  private subscribeToRoomAssignment(): void {
    console.log('Subscribing to room assignment for:', this.telegramId);
    this.roomSubscription = this.websocketService
      .subscribe(`/topic/player.${this.telegramId}.room-assigned`)
      .subscribe({
        next: (response: any) => {
          console.log('Room assigned response received:', response);
          this.roomId = response.roomId;
          const roomState = response.roomState;

          // Store room state for navigation
          if (roomState === 'STARTED') {
            // Navigate to game room when game starts
            this.navigateToGame();
          }
        },
        error: (error) => {
          console.error('Failed to receive room assignment:', error);
          this.router.navigate(['/menu']);
        }
      });
  }

  private startTimer(): void {
    // Start countdown from 10 to 0
    // Backend will automatically start the game after 10 seconds
    // We just show the countdown and wait for STARTED state from backend
    this.timerSubscription = interval(1000)
      .pipe(take(10))
      .subscribe({
        next: (count) => {
          this.waitingTime = 10 - count - 1;

          if (this.waitingTime === 0) {
            console.log('Timer finished, waiting for backend to start game...');
          }
        }
      });
  }

  private startGame(): void {
    // This method is no longer needed - backend handles auto-start
    // Keeping it for potential manual start button in future
    if (this.roomId) {
      console.log('Manual start requested for room:', this.roomId);
      this.websocketService.publish('/app/room.start', {
        roomId: this.roomId,
        telegramId: this.telegramId
      });
    }
  }

  private navigateToGame(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }

    if (!this.roomId) {
      console.error('Cannot navigate: no room ID');
      this.router.navigate(['/menu']);
      return;
    }

    console.log('Navigating to game room:', this.roomId);
    this.router.navigate(['/room', this.roomId]);
  }
}
