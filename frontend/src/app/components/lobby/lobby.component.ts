import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { GameService } from '../../services/game.service';
import { GameRoom } from '../../models/game.models';

@Component({
  selector: 'app-lobby',
  imports: [CommonModule, FormsModule],
  templateUrl: './lobby.component.html',
  styleUrl: './lobby.component.scss'
})
export class LobbyComponent implements OnInit {
  rooms: GameRoom[] = [];
  playerName: string = '';
  newRoomName: string = '';
  maxPlayers: number = 4;
  loading: boolean = false;

  constructor(
    private gameService: GameService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRooms();
  }

  loadRooms(): void {
    this.loading = true;
    this.gameService.getRooms().subscribe({
      next: (rooms) => {
        this.rooms = rooms;
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load rooms:', error);
        this.loading = false;
      }
    });
  }

  joinRoom(roomId: string): void {
    if (!this.playerName.trim()) {
      alert('Пожалуйста, введите ваше имя');
      return;
    }

    this.gameService.joinRoom(roomId, this.playerName);
    this.router.navigate(['/room', roomId]);
  }

  createRoom(): void {
    if (!this.newRoomName.trim()) {
      alert('Пожалуйста, введите название комнаты');
      return;
    }

    if (!this.playerName.trim()) {
      alert('Пожалуйста, введите ваше имя');
      return;
    }

    this.loading = true;
    this.gameService.createRoom(this.newRoomName, this.maxPlayers).subscribe({
      next: (room) => {
        this.gameService.joinRoom(room.id, this.playerName);
        this.router.navigate(['/room', room.id]);
      },
      error: (error) => {
        console.error('Failed to create room:', error);
        this.loading = false;
      }
    });
  }

  refreshRooms(): void {
    this.loadRooms();
  }
}
