import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, Message, StompConfig, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { filter, first } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client;
  private connectionStatus = new BehaviorSubject<boolean>(false);

  constructor() {
    this.stompClient = new Client({
      webSocketFactory: () => new (SockJS as any)('http://localhost:8080/rg'),
      connectHeaders: {},
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = () => {
      console.log('Connected to WebSocket');
      this.connectionStatus.next(true);
    };

    this.stompClient.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.connectionStatus.next(false);
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP Error:', frame);
      this.connectionStatus.next(false);
    };
  }

  connect(): void {
    if (!this.stompClient.connected) {
      this.stompClient.activate();
    }
  }

  disconnect(): void {
    if (this.stompClient.connected) {
      this.stompClient.deactivate();
    }
  }

  isConnected(): Observable<boolean> {
    return this.connectionStatus.asObservable();
  }

  subscribe(destination: string): Observable<any> {
    return new Observable(observer => {
      let subscription: StompSubscription | null = null;

      const doSubscribe = () => {
        console.log('Subscribing to:', destination);
        subscription = this.stompClient.subscribe(destination, (message: Message) => {
          console.log('Message received on', destination, ':', message.body);
          try {
            observer.next(JSON.parse(message.body));
          } catch (e) {
            console.error('Failed to parse message:', e);
            observer.error(e);
          }
        });
      };

      if (this.stompClient.connected) {
        doSubscribe();
      } else {
        // Wait for connection
        console.log('Waiting for connection to subscribe to:', destination);
        this.connectionStatus.pipe(
          filter(connected => connected),
          first()
        ).subscribe(() => {
          doSubscribe();
        });
      }

      // Cleanup function
      return () => {
        if (subscription) {
          console.log('Unsubscribing from:', destination);
          subscription.unsubscribe();
        }
      };
    });
  }

  publish(destination: string, body: any): void {
    if (this.stompClient.connected) {
      console.log('Publishing to', destination, ':', body);
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body)
      });
    } else {
      console.warn('WebSocket not connected. Message not sent to', destination);
    }
  }

  // Game-specific methods
  joinRoom(roomId: string, playerName: string): void {
    this.publish('/app/room.join', { roomId, playerName });
  }

  leaveRoom(roomId: string): void {
    this.publish('/app/room.leave', { roomId });
  }

  subscribeToRoom(roomId: string): Observable<any> {
    return this.subscribe(`/topic/room.${roomId}`);
  }

  subscribeToRoomState(roomId: string): Observable<any> {
    return this.subscribe(`/topic/room.${roomId}.state`);
  }

  subscribeToRoomTurn(roomId: string): Observable<any> {
    return this.subscribe(`/topic/room.${roomId}.turn`);
  }

  subscribeToPlayerQueue(playerId: string): Observable<any> {
    return this.subscribe(`/queue/player.${playerId}`);
  }

  // Game action methods
  startGame(roomId: string): void {
    this.publish('/app/room.start', { roomId });
  }

  buyCar(roomId: number, telegramId: string, carId: number): void {
    this.publish('/app/game.buyCar', { roomId, telegramId, carId });
  }

  buyTuning(roomId: number, telegramId: string, tuningId: number, carId: number): void {
    console.log('🔵 WebSocketService.buyTuning called:', { roomId, telegramId, tuningId, carId });
    this.publish('/app/game.buyTuning', { roomId, telegramId, tuningId, carId });
    console.log('🔵 Message published to /app/game.buyTuning');
  }

  skipAction(roomId: number, telegramId: string): void {
    this.publish('/app/game.skip', { roomId, telegramId });
  }
}
