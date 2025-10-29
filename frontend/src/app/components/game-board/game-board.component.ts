import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameCardComponent } from '../game-card/game-card.component';
import { CardStack, GameCard, CardType } from '../../models/game.models';

@Component({
  selector: 'app-game-board',
  imports: [CommonModule, GameCardComponent],
  templateUrl: './game-board.component.html',
  styleUrl: './game-board.component.scss'
})
export class GameBoardComponent {
  @Input() cardStacks: CardStack[] = [];
  @Output() cardSelected = new EventEmitter<GameCard>();
  @Output() stackClicked = new EventEmitter<CardStack>();

  // Track which stack is expanded
  expandedStackId: string | null = null;

  onStackClick(stack: CardStack): void {
    // Toggle expansion
    if (this.expandedStackId === stack.id) {
      this.expandedStackId = null;
      stack.isExpanded = false;
    } else {
      // Collapse previous expanded stack
      if (this.expandedStackId) {
        const prevStack = this.cardStacks.find(s => s.id === this.expandedStackId);
        if (prevStack) {
          prevStack.isExpanded = false;
        }
      }

      this.expandedStackId = stack.id;
      stack.isExpanded = true;
    }

    this.stackClicked.emit(stack);
  }

  onCardClick(card: GameCard): void {
    this.cardSelected.emit(card);
  }

  onCardDragStart(card: GameCard): void {
    console.log('Drag started:', card);
  }

  onCardDragEnd(card: GameCard): void {
    console.log('Drag ended:', card);
  }

  getStackTopCard(stack: CardStack): GameCard | null {
    return stack.cards.length > 0 ? stack.cards[stack.cards.length - 1] : null;
  }

  getStackCards(stack: CardStack): GameCard[] {
    return stack.isExpanded ? stack.cards : [];
  }

  getCardPositionInStack(index: number, total: number, isExpanded: boolean): any {
    if (!isExpanded) {
      return {
        top: `${index * 2}px`,
        left: `${index * 2}px`
      };
    }

    // When expanded, spread cards horizontally
    const spacing = 90; // 80px card width + 10px gap
    return {
      top: '0px',
      left: `${index * spacing}px`
    };
  }

  getStackLabel(type: CardType): string {
    switch (type) {
      case CardType.CAR:
      case 'car':
        return 'Cars';
      case CardType.CLIENT:
      case 'client':
        return 'Clients';
      case CardType.TUNING:
      case 'tuning':
        return 'Tuning';
      case CardType.DEBUFF:
      case 'debuff':
        return 'Effects';
      default:
        return '';
    }
  }

  getStackCardCount(stack: CardStack): number {
    return stack.cards.length;
  }
}
