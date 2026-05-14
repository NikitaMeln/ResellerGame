import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { WebSocketService } from './websocket.service';
import {
  GameRoom,
  Player,
  Car,
  Client,
  Tuning,
  GameAction,
  GameActionType,
  SaleResult
} from '../models/game.models';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private currentRoom = new BehaviorSubject<GameRoom | null>(null);
  private currentPlayer = new BehaviorSubject<Player | null>(null);
  private availableCars = new BehaviorSubject<Car[]>([]);
  private availableClients = new BehaviorSubject<Client[]>([]);
  private availableTunings = new BehaviorSubject<Tuning[]>([]);

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {}

  // Observable getters
  getCurrentRoom(): Observable<GameRoom | null> {
    return this.currentRoom.asObservable();
  }

  getCurrentPlayer(): Observable<Player | null> {
    return this.currentPlayer.asObservable();
  }

  getAvailableCars(): Observable<Car[]> {
    return this.availableCars.asObservable();
  }

  getAvailableClients(): Observable<Client[]> {
    return this.availableClients.asObservable();
  }

  getAvailableTunings(): Observable<Tuning[]> {
    return this.availableTunings.asObservable();
  }

  // Room management
  joinRoom(roomId: string, playerName: string): void {
    this.webSocketService.connect();
    this.webSocketService.joinRoom(roomId, playerName);

    // Subscribe to room updates
    this.webSocketService.subscribeToRoom(roomId).subscribe(roomUpdate => {
      this.currentRoom.next(roomUpdate);
    });
  }

  leaveRoom(): void {
    const room = this.currentRoom.value;
    if (room) {
      this.webSocketService.leaveRoom(room.id);
      this.currentRoom.next(null);
      this.currentPlayer.next(null);
    }
  }

  createRoom(roomName: string, maxPlayers: number): Observable<GameRoom> {
    return this.http.post<GameRoom>('/api/rooms', { name: roomName, maxPlayers });
  }

  getRooms(): Observable<GameRoom[]> {
    return this.http.get<GameRoom[]>('/api/rooms');
  }

  getRoomState(roomId: string | number): Observable<any> {
    return this.http.get<any>(`http://localhost:8080/api/game/room/${roomId}`);
  }

  // Game actions
  buyCar(carId: string): void {
    const action: GameAction = {
      type: GameActionType.BUY_CAR,
      playerId: this.currentPlayer.value?.id || '',
      data: { carId }
    };
    this.sendGameAction(action);
  }

  applyTuning(carId: string, tuningId: string): void {
    const action: GameAction = {
      type: GameActionType.APPLY_TUNING,
      playerId: this.currentPlayer.value?.id || '',
      data: { carId, tuningId }
    };
    this.sendGameAction(action);
  }

  selectClient(clientId: string): void {
    const action: GameAction = {
      type: GameActionType.SELECT_CLIENT,
      playerId: this.currentPlayer.value?.id || '',
      data: { clientId }
    };
    this.sendGameAction(action);
  }

  sellCar(carId: string, clientId: string): Observable<SaleResult> {
    return this.http.post<SaleResult>('/api/game/sell', { carId, clientId });
  }

  private sendGameAction(action: GameAction): void {
    this.webSocketService.publish('/app/game.action', action);
  }

  // Utility methods
  canBuyCar(car: Car): boolean {
    const player = this.currentPlayer.value;
    return player ? player.money >= car.price : false;
  }

  canApplyTuning(tuning: Tuning): boolean {
    const player = this.currentPlayer.value;
    return player ? player.money >= tuning.price : false;
  }

  isPlayerTurn(): boolean {
    const room = this.currentRoom.value;
    const player = this.currentPlayer.value;

    if (!room || !player) return false;

    const currentPlayerInRoom = room.players[room.currentPlayerIndex];
    return currentPlayerInRoom?.id === player.id;
  }
}