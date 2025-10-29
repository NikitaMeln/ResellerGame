import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-main-menu',
  imports: [CommonModule],
  templateUrl: './main-menu.component.html',
  styleUrl: './main-menu.component.scss'
})
export class MainMenuComponent {
  showRules = false;
  imageError = false;

  constructor(private router: Router) {}

  onPlayClick(): void {
    // TODO: Implement play functionality
    console.log('Play button clicked - not implemented yet');
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
