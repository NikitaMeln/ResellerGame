# Game polish backlog

Improvements identified after the end-to-end selling-phase milestone. None of these are blockers — the loop plays through — but they shape the feel of the demo. Roughly ordered by impact on playability.

## Gameplay balance

- **Car economy is too expensive — RESOLVED.** Car prices in `cars.json` were lowered from the 400-3000 range to 300-1600: every price level shifted down one step (400→300, 600→400, … 3000→1600), so the relative ordering and the distribution across levels are unchanged. `STARTING_BALANCE` (1300) and tuning costs (flat 200) were deliberately left alone. Effect: cars affordable on turn 1 went from 17/50 to 39/50, and loss-making car/client pairs dropped from 35% to 5.6% (median margin per sale 400 → 1100). Client budgets (1000-3900) are untouched and are the next lever if the rich clients now feel too generous against the new 1600 ceiling.

- **Dice distribution feels skewed low.** Players report 1-2-3 dominate, ≥4 is rare. Two possible causes:
  - Real `Math.random()` distribution — feels off because thresholds (3-6) put most clients on the unlucky side. Could be perception, not statistics.
  - Rolling on the client (see [game-room.component.ts:rollDice](frontend/src/app/components/game-room/game-room.component.ts)) — a malicious client could send any value 1-6, but our own implementation is uniform.
  - Investigation: log every roll on the server, eyeball the distribution after ~30 rolls. If genuinely uneven, swap to `SecureRandom` or revisit threshold formula in `computeThreshold()`.

## UX / Visual

- **Modal overlays positioning.** The popups (secret card, dice roll, result, finished, error toast) need layout review — current panels are functional but not centered against the game board, can overlap header/garage on smaller screens. Audit z-index, fixed vs. absolute positioning, max-width per viewport.

- **Secret card and dice roll visuals.** Currently a placeholder "?" tile and a CSS-spinning square. Should look like part of the game, not a debug screen:
  - Secret card: actual flip animation (face-down → face-up), themed back like other cards in the game.
  - Dice: 3D-looking die with pips, or at least proper pip layout per face. Roll animation should feel weighted (decelerate before settling).

- **Card artwork.** Cars and clients are text-only. Add image assets:
  - Car avatars in `frontend/src/assets/cars/` keyed by car id, displayed in `CarCard` component.
  - Client portraits in `frontend/src/assets/clients/` keyed by client id, displayed in `ClientCard` component.
  - The `image` field already exists on `GameCard` interface — wiring is partially there.

## Known design holes (not in user's list, but worth tracking)

- **No game restart UI.** Once `FINISHED` modal shows, players are stuck on that screen. Need "Back to menu" / "Play again".
- **Negative card effects beyond `addToRandomModifier` are ignored.** `priceEffect`, ban-round, deactivate-tunings effects from `tuning-data-en.json` are loaded but not applied. Deferred per Q3 in the selling-phase planning.
- **Dice value is client-authoritative.** OK for demo, must move server-side for any competitive use.
