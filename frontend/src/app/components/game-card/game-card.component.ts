import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameCard, CardType, CarCard, ClientCard, TuningCard, DebuffCard } from '../../models/game.models';

@Component({
  selector: 'app-game-card',
  imports: [CommonModule],
  templateUrl: './game-card.component.html',
  styleUrl: './game-card.component.scss'
})
export class GameCardComponent {
  @Input() card!: GameCard;
  @Input() isInStack: boolean = false;
  @Input() stackIndex: number = 0;
  @Output() cardClick = new EventEmitter<GameCard>();
  @Output() cardDragStart = new EventEmitter<GameCard>();
  @Output() cardDragEnd = new EventEmitter<GameCard>();

  // For template access to CardType enum
  CardType = CardType;

  onCardClick(): void {
    if (!this.card.isDraggable || this.isInStack) {
      this.cardClick.emit(this.card);
    }
  }

  onDragStart(event: DragEvent): void {
    if (this.card.isDraggable && !this.isInStack) {
      event.dataTransfer!.effectAllowed = 'move';
      event.dataTransfer!.setData('cardId', this.card.id);
      this.cardDragStart.emit(this.card);
    }
  }

  onDragEnd(event: DragEvent): void {
    this.cardDragEnd.emit(this.card);
  }

  getCardTypeClass(): string {
    switch (this.card.type) {
      case 'car':
        return 'card-car';
      case 'client':
        return 'card-client';
      case 'tuning':
        return 'card-tuning';
      case 'debuff':
        return 'card-debuff';
      default:
        return '';
    }
  }

  getCardTitle(): string {
    return this.card.title;
  }

  shouldShowImage(): boolean {
    return !!this.card.image && this.card.isRevealed !== false;
  }

  shouldShowBack(): boolean {
    return this.card.isRevealed === false;
  }

  // Type guards and getters for template
  get asCarCard(): CarCard | null {
    return this.card.type === 'car' ? (this.card as CarCard) : null;
  }

  get asClientCard(): ClientCard | null {
    return this.card.type === 'client' ? (this.card as ClientCard) : null;
  }

  get asTuningCard(): TuningCard | null {
    return this.card.type === 'tuning' ? (this.card as TuningCard) : null;
  }

  get asDebuffCard(): DebuffCard | null {
    return this.card.type === 'debuff' ? (this.card as DebuffCard) : null;
  }
}
