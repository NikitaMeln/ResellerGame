import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, Message, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

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
      if (this.stompClient.connected) {
        const subscription = this.stompClient.subscribe(destination, (message: Message) => {
          observer.next(JSON.parse(message.body));
        });

        return () => subscription.unsubscribe();
      } else {
        this.stompClient.onConnect = () => {
          const subscription = this.stompClient.subscribe(destination, (message: Message) => {
            observer.next(JSON.parse(message.body));
          });
        };

        return () => {
          // Cleanup function for when not connected
        };
      }
    });
  }

  publish(destination: string, body: any): void {
    if (this.stompClient.connected) {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body)
      });
    } else {
      console.warn('WebSocket not connected. Message not sent.');
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
    this.publish('/app/game.start', { roomId });
  }

  buyCar(roomId: number, telegramId: string, carId: number): void {
    this.publish('/app/game.buyCar', { roomId, telegramId, carId });
  }

  buyTuning(roomId: number, telegramId: string, tuningId: number): void {
    this.publish('/app/game.buyTuning', { roomId, telegramId, tuningId });
  }

  skipAction(roomId: number, telegramId: string): void {
    this.publish('/app/game.skip', { roomId, telegramId });
  }
}