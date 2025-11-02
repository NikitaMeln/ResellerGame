import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlayerService } from '../../services/player.service';

@Component({
  selector: 'app-main-menu',
  imports: [CommonModule, FormsModule],
  templateUrl: './main-menu.component.html',
  styleUrl: './main-menu.component.scss'
})
export class MainMenuComponent {
  showRules = false;
  imageError = false;
  isConnecting = false;
  username = '';

  constructor(
    private router: Router,
    private playerService: PlayerService
  ) {}

  onPlayClick(): void {
    if (this.isConnecting || !this.username.trim()) return;

    this.isConnecting = true;

    // Create player with entered username
    const player = this.playerService.createPlayer(this.username.trim());
    console.log('Created player:', player);

    // Navigate to lobby
    this.router.navigate(['/lobby'], {
      state: {
        telegramId: player.telegramId,
        username: this.username.trim()
      }
    });
  }

  get isPlayButtonDisabled(): boolean {
    return !this.username.trim() || this.isConnecting;
  }

  toggleRules(): void {
    this.showRules = !this.showRules;
  }

  closeRules(): void {
    this.showRules = false;
  }

  onImageLoad(): void {
    console.log('Logo image loaded successfully!');
    this.imageError = false;
  }

  onImageError(event: any): void {
    console.error('Failed to load logo image:', event);
    console.error('Image src was:', event.target?.src);
    this.imageError = true;
  }
}
