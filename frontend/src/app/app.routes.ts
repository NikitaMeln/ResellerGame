import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: '/menu', pathMatch: 'full' },
  {
    path: 'menu',
    loadComponent: () => import('./components/main-menu/main-menu.component').then(m => m.MainMenuComponent)
  },
  {
    path: 'lobby',
    loadComponent: () => import('./components/lobby/lobby.component').then(m => m.LobbyComponent)
  },
  {
    path: 'room/:id',
    loadComponent: () => import('./components/game-room/game-room.component').then(m => m.GameRoomComponent)
  },
  {
    path: 'demo',
    loadComponent: () => import('./components/game-board-demo/game-board-demo.component').then(m => m.GameBoardDemoComponent)
  },
  {
    path: 'first-round',
    loadComponent: () => import('./components/first-round-board/first-round-board.component').then(m => m.FirstRoundBoardComponent)
  },
  { path: '**', redirectTo: '/menu' }
];
