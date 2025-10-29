import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameBoardComponent } from '../game-board/game-board.component';
import { CardStack, GameCard, CardType, CarCard, ClientCard, TuningCard, DebuffCard } from '../../models/game.models';

@Component({
  selector: 'app-game-board-demo',
  imports: [CommonModule, GameBoardComponent],
  templateUrl: './game-board-demo.component.html',
  styleUrl: './game-board-demo.component.scss'
})
export class GameBoardDemoComponent implements OnInit {
  cardStacks: CardStack[] = [];

  ngOnInit(): void {
    this.initializeDemoData();
  }

  initializeDemoData(): void {
    // Create demo car cards
    const carCards: CarCard[] = [
      {
        id: 'car-1',
        type: 'car',
        title: 'BMW 320i',
        description: 'Спортивный седан премиум класса',
        stats: {
          brand: 'BMW',
          model: '320i',
          year: 2020,
          price: 35000
        },
        image: 'assets/cars/bmw.jpg',
        isRevealed: true,
        isDraggable: true
      },
      {
        id: 'car-2',
        type: 'car',
        title: 'Toyota Camry',
        description: 'Надежный семейный седан',
        stats: {
          brand: 'Toyota',
          model: 'Camry',
          year: 2019,
          price: 25000
        },
        isRevealed: true,
        isDraggable: true
      },
      {
        id: 'car-3',
        type: 'car',
        title: 'Mercedes C-Class',
        description: 'Элегантный бизнес-седан',
        stats: {
          brand: 'Mercedes',
          model: 'C-Class',
          year: 2021,
          price: 45000
        },
        isRevealed: true,
        isDraggable: true
      }
    ];

    // Create demo client cards
    const clientCards: ClientCard[] = [
      {
        id: 'client-1',
        type: 'client',
        title: 'Богатый бизнесмен',
        description: 'Ищет престижный автомобиль',
        stats: {
          budget: 50000,
          modifier: 0,
          requirements: ['Премиум класс', 'Кожаный салон']
        },
        isRevealed: true
      },
      {
        id: 'client-2',
        type: 'client',
        title: 'Молодая семья',
        description: 'Нужен надежный семейный автомобиль',
        stats: {
          budget: 30000,
          modifier: -1,
          requirements: ['Просторный', 'Экономичный']
        },
        isRevealed: true
      },
      {
        id: 'client-3',
        type: 'client',
        title: 'Спортсмен',
        description: 'Хочет быстрый и стильный автомобиль',
        stats: {
          budget: 40000,
          modifier: 1,
          requirements: ['Спортивный', 'Мощный двигатель']
        },
        isRevealed: true
      }
    ];

    // Create demo tuning cards
    const tuningCards: TuningCard[] = [
      {
        id: 'tuning-1',
        type: 'tuning',
        title: 'Спортивный обвес',
        description: 'Улучшает внешний вид автомобиля',
        stats: {
          price: 3000,
          category: 'Экстерьер',
          effect: '+1 к продаже спортсменам'
        },
        isRevealed: true,
        isDraggable: true
      },
      {
        id: 'tuning-2',
        type: 'tuning',
        title: 'Кожаный салон',
        description: 'Премиум отделка салона',
        stats: {
          price: 5000,
          category: 'Интерьер',
          effect: '+1 к продаже бизнесменам'
        },
        isRevealed: true,
        isDraggable: true
      },
      {
        id: 'tuning-3',
        type: 'tuning',
        title: 'Турбо двигатель',
        description: 'Увеличивает мощность',
        stats: {
          price: 7000,
          category: 'Двигатель',
          effect: '+2 к продаже'
        },
        isRevealed: true,
        isDraggable: true
      }
    ];

    // Create demo buff/debuff cards
    const buffDebuffCards: DebuffCard[] = [
      {
        id: 'buff-1',
        type: 'debuff',
        title: 'Удачная сделка',
        description: 'Вам повезло!',
        stats: {
          effect: 2,
          isPositive: true
        },
        isRevealed: true
      },
      {
        id: 'debuff-1',
        type: 'debuff',
        title: 'Проблемы с документами',
        description: 'Клиент настороже',
        stats: {
          effect: -1,
          isPositive: false
        },
        isRevealed: true
      }
    ];

    // Create card stacks
    this.cardStacks = [
      {
        id: 'stack-cars',
        type: 'car' as CardType,
        cards: carCards,
        isExpanded: false,
        position: { x: 1, y: 1 }
      },
      {
        id: 'stack-clients',
        type: 'client' as CardType,
        cards: clientCards,
        isExpanded: false,
        position: { x: 2, y: 1 }
      },
      {
        id: 'stack-tuning',
        type: 'tuning' as CardType,
        cards: tuningCards,
        isExpanded: false,
        position: { x: 3, y: 1 }
      },
      {
        id: 'stack-buffs',
        type: 'debuff' as CardType,
        cards: buffDebuffCards,
        isExpanded: false,
        position: { x: 4, y: 1 }
      }
    ];
  }

  onCardSelected(card: GameCard): void {
    console.log('Card selected:', card);
    alert(`Выбрана карта: ${card.title}`);
  }

  onStackClicked(stack: CardStack): void {
    console.log('Stack clicked:', stack);
  }
}
