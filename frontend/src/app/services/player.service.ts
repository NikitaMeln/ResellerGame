import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

export interface PlayerInfo {
  telegramId: string;
  username: string;
  language: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private playerInfo = new BehaviorSubject<PlayerInfo | null>(null);
  private readonly STORAGE_KEY = 'reseller_player_info';
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  constructor() {
    if (this.isBrowser) {
      this.loadPlayerInfo();
    }
  }

  getPlayerInfo(): Observable<PlayerInfo | null> {
    return this.playerInfo.asObservable();
  }

  getCurrentPlayerInfo(): PlayerInfo | null {
    return this.playerInfo.value;
  }

  createPlayer(username?: string): PlayerInfo {
    const playerInfo: PlayerInfo = {
      telegramId: uuidv4(),
      username: username || `Player_${Math.floor(Math.random() * 10000)}`,
      language: 'eng'
    };

    this.playerInfo.next(playerInfo);
    this.savePlayerInfo(playerInfo);
    return playerInfo;
  }

  updatePlayerInfo(playerInfo: PlayerInfo): void {
    this.playerInfo.next(playerInfo);
    this.savePlayerInfo(playerInfo);
  }

  clearPlayerInfo(): void {
    this.playerInfo.next(null);
    if (this.isBrowser) {
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  private loadPlayerInfo(): void {
    if (!this.isBrowser) return;

    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored) {
      try {
        const playerInfo = JSON.parse(stored);
        this.playerInfo.next(playerInfo);
      } catch (e) {
        console.error('Failed to parse stored player info', e);
      }
    }
  }

  private savePlayerInfo(playerInfo: PlayerInfo): void {
    if (this.isBrowser) {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(playerInfo));
    }
  }
}
