import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
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

  constructor(
    private router: Router,
    private websocketService: WebSocketService
  ) {
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras?.state as { telegramId: string, username: string };

    if (state) {
      this.telegramId = state.telegramId;
      this.username = state.username;
    } else {
      // If no state, redirect back to menu
      this.router.navigate(['/menu']);
    }
  }

  ngOnInit(): void {
    if (!this.telegramId || !this.username) {
      this.router.navigate(['/menu']);
      return;
    }

    // Connect to WebSocket
    this.websocketService.connect();

    // Subscribe to room assignment
    this.subscribeToRoomAssignment();

    // Wait for connection and send join request
    setTimeout(() => {
      console.log('Sending join request...');
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
          if (roomState === 'PLAYING') {
            // Navigate to first-round-board when game starts
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
    this.timerSubscription = interval(1000)
      .pipe(take(10))
      .subscribe({
        next: (count) => {
          this.waitingTime = 10 - count - 1;

          if (this.waitingTime === 0) {
            // Timer finished, start game even with 1 player
            this.startGame();
          }
        }
      });
  }

  private startGame(): void {
    if (this.roomId) {
      // Send start game request
      console.log('Starting game for room:', this.roomId);
      this.websocketService.publish('/app/room.start', {
        roomId: this.roomId,
        telegramId: this.telegramId
      });

      // Navigate to game
      this.navigateToGame();
    } else {
      console.error('No room assigned yet');
      this.router.navigate(['/menu']);
    }
  }

  private navigateToGame(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }

    console.log('Navigating to first-round-board');
    this.router.navigate(['/first-round']);
  }
}
