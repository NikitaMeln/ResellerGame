# Game Flow Mechanics

Read when: changing turn progression, selling/buying logic, or dice/profit calculations in `GameSessionServiceImpl`.

1. **Room States**: `PENDING` → `STARTED` → `FINISHED`
   - Room auto-starts when first player joins (10 second delay) OR when 5 players join (MAX_PLAYERS)
   - MIN_PLAYERS = 1 for development.

2. **Player Management**:
   - `GameRoom.players`: All players in the room (database - persistent)
   - `GameRoom.playerQueue`: Ordered turn sequence maintained separately (database - persistent)
   - `GameSession.currentPlayerIndex`: Tracks whose turn it is (in-memory - temporary)

3. **Turn Steps**: Each action progresses through defined steps
   - `CAR_SELECTION`: Player buys a car
   - `TUNING_SELECTION`: Player optionally applies tuning (or skips)
   - `CHOICE_CLIENT_TO_SELL`: Player selects client to sell to
   - `SHOW_SECRET_CARD`: Server reveals buff/debuff card
   - `TURN_MULTIPLIER`: Player rolls dice
   - `RESULT`: Server calculates profit/loss

4. **Turn Order**: Two-phase rounds
   - **Buying Phase** (sequential order): Each player buys only one car → buys and applies only one tuning → receives one negative tuning card as a secret.
   - **Selling Phase** (reverse order): Each player sells only one car to clients in the game zone.

5. **Selling Mechanics**:
   - Player receives secret card (buff/debuff) stored in `CarInstance.hiddenNegativeCard`
   - Dice roll (1-6): Base success threshold is ≥3
   - Client modifiers adjust threshold (e.g., +2 modifier requires ≥5)
   - Success: Profit = client budget. Car and tuning costs were already deducted from the balance at purchase time, so they are NOT subtracted again here — doing so would charge the player twice
   - Failure: No payout, but the car is NOT lost — it stays in the player's garage and can be offered to a different client in a later round. The client leaves the window either way
   - Real-time updates broadcast via WebSocket topics

## Known Design Gaps

See [../NOTES.md](../NOTES.md) for the current polish backlog. Notably:

- Dice roll is client-authoritative (rolled in `game-room.component.ts`, not verified server-side) — fine for the demo, but must move server-side before any competitive/anti-cheat use.
- Negative-card effects beyond `addToRandomModifier` (e.g. `priceEffect`, ban-round, deactivate-tunings from `tuning-data-en.json`) are loaded but not yet applied in game logic.
- Starting balance vs. car/tuning prices is not yet balanced as a system; early turns can be unaffordable.
