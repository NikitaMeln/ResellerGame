import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerGameState } from '../../models/game.models';

@Component({
  selector: 'app-game-header',
  imports: [CommonModule],
  templateUrl: './game-header.component.html',
  styleUrl: './game-header.component.scss'
})
export class GameHeaderComponent {
  @Input() playerState!: PlayerGameState;

  get availableGarageSlots(): number {
    return this.playerState.garageSlots - this.playerState.usedGarageSlots;
  }
}
