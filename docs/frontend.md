# Frontend Zone Management (Complex UI Pattern)

Read when: touching card rendering, zones, or round-refresh logic in `game-room.component.ts`.

The frontend (`game-room.component.ts`) manages card display using a three-tier architecture to handle real-time updates efficiently:

## Three-Tier Card State

1. **Available Pools**: Full card sets loaded once from backend
   ```typescript
   clientAvailableCards: (ClientCard | null)[] = [];  // All 50 clients
   marketAvailableCards: (CarCard | null)[] = [];     // All 50 cars
   tuningAvailableCards: (TuningCard | null)[] = [];  // All tunings
   ```

2. **Zone Snapshots**: Fixed subsets displayed per round (playerCount + 1 cards)
   ```typescript
   clientZone: (ClientCard | null)[] = [];  // Visible clients this round
   marketZone: (CarCard | null)[] = [];     // Visible cars this round
   ```

3. **Purchased State**: Cards removed by setting to `null` (not deleted from array)

## Synchronization Logic

```typescript
// On new round: create fresh zone snapshots
initializeRoundZones(cardsToShow: number) {
  this.clientZone = this.clientAvailableCards
    .filter(card => availableIds.has(card.id))
    .slice(0, cardsToShow);
}

// During round: update existing zones
updateExistingZones() {
  this.marketZone = this.marketZone.map(card =>
    availableIds.has(card?.id) ? card : null  // Replace purchased with null
  );
}
```

This prevents flickering during rapid WebSocket updates and maintains stable card positions in the UI.

## Frontend Project Structure

```
frontend/src/app/
├── components/      # Game UI components (standalone)
├── services/        # WebSocketService, PlayerService
├── models/          # TypeScript interfaces (GameCard, PlayerGameState, etc.)
└── app.routes.ts    # Routing configuration
```
