import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { GameService } from '../../services/game.service';
import { GameRoom, Player, Car, Client, GamePhase } from '../../models/game.models';

@Component({
  selector: 'app-game-room',
  imports: [CommonModule],
  templateUrl: './game-room.component.html',
  styleUrl: './game-room.component.scss'
})
export class GameRoomComponent implements OnInit, OnDestroy {
  room: GameRoom | null = null;
  currentPlayer: Player | null = null;
  availableCars: Car[] = [];
  availableClients: Client[] = [];
  isPlayerTurn: boolean = false;

  private subscriptions: Subscription[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private gameService: GameService
  ) {}

  ngOnInit(): void {
    const roomId = this.route.snapshot.paramMap.get('id');
    if (!roomId) {
      this.router.navigate(['/lobby']);
      return;
    }

    this.subscriptions.push(
      this.gameService.getCurrentRoom().subscribe(room => {
        this.room = room;
        this.updatePlayerTurn();
      })
    );

    this.subscriptions.push(
      this.gameService.getCurrentPlayer().subscribe(player => {
        this.currentPlayer = player;
        this.updatePlayerTurn();
      })
    );

    this.subscriptions.push(
      this.gameService.getAvailableCars().subscribe(cars => {
        this.availableCars = cars;
      })
    );

    this.subscriptions.push(
      this.gameService.getAvailableClients().subscribe(clients => {
        this.availableClients = clients;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private updatePlayerTurn(): void {
    this.isPlayerTurn = this.gameService.isPlayerTurn();
  }

  leaveRoom(): void {
    this.gameService.leaveRoom();
    this.router.navigate(['/lobby']);
  }

  buyCar(car: Car): void {
    if (this.canBuyCar(car)) {
      this.gameService.buyCar(car.id);
    }
  }

  canBuyCar(car: Car): boolean {
    return this.isPlayerTurn &&
           this.room?.currentPhase === GamePhase.CAR_BUYING &&
           this.gameService.canBuyCar(car);
  }

  canSelectClient(client: Client): boolean {
    return this.isPlayerTurn && this.room?.currentPhase === GamePhase.CLIENT_SELECTION;
  }

  selectClient(client: Client): void {
    if (this.canSelectClient(client)) {
      this.gameService.selectClient(client.id);
    }
  }

  getPhaseText(): string {
    if (!this.room) return '';

    switch (this.room.currentPhase) {
      case GamePhase.WAITING:
        return 'Ожидание игроков';
      case GamePhase.CAR_BUYING:
        return 'Покупка автомобилей';
      case GamePhase.CAR_TUNING:
        return 'Тюнинг автомобилей';
      case GamePhase.CLIENT_SELECTION:
        return 'Выбор клиентов';
      case GamePhase.SELLING:
        return 'Продажа автомобилей';
      case GamePhase.FINISHED:
        return 'Игра завершена';
      default:
        return '';
    }
  }

  getCurrentPlayerName(): string {
    if (!this.room || !this.room.players[this.room.currentPlayerIndex]) {
      return '';
    }
    return this.room.players[this.room.currentPlayerIndex].name;
  }
}
