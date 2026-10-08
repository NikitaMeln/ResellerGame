# Swagger API Testing Guide

## Accessing Swagger UI

After starting the backend, open your browser and navigate to:

**Swagger UI**: http://localhost:8080/swagger-ui.html

**OpenAPI JSON**: http://localhost:8080/api-docs

## Quick Start Testing Flow

### 1. Check API Health
```
GET /api/game/health
```
No parameters needed. Should return:
```json
{
  "status": "UP",
  "service": "Reseller Game API",
  "version": "1.0"
}
```

### 2. Join Room (Create Player 1)
```
POST /api/game/room/join
```
Request body:
```json
{
  "telegramId": "player1",
  "username": "TestPlayer1",
  "language": "en"
}
```

Response will include `roomId` - save this for next steps!

### 3. Join Room (Create Player 2)
```
POST /api/game/room/join
```
Request body:
```json
{
  "telegramId": "player2",
  "username": "TestPlayer2",
  "language": "en"
}
```

### 4. Start Game
```
POST /api/game/room/{roomId}/start
```
Replace `{roomId}` with the room ID from step 2.

### 5. Get Room State
```
GET /api/game/room/{roomId}
```
This will show you:
- All players in the room
- Available cars in the market
- Available tunings
- Current turn information
- Player balances and garages

### 6. Buy a Car
```
POST /api/game/action/buy-car
```
Request body:
```json
{
  "roomId": 1,
  "telegramId": "player1",
  "carId": 1
}
```

**Note**: Get `carId` from the room state response (step 5) - look for available cars in the market.

### 7. Buy Tuning (Optional)
```
POST /api/game/action/buy-tuning
```
Request body:
```json
{
  "roomId": 1,
  "telegramId": "player1",
  "tuningId": 1,
  "carId": "car-instance-uuid"
}
```

**Note**: Get `tuningId` from available tunings and `carId` from the car you just purchased in your garage.

### 8. Skip Tuning
```
POST /api/game/action/skip
```
Request body:
```json
{
  "roomId": 1,
  "telegramId": "player1"
}
```

## Common Testing Scenarios

### Scenario 1: Full Game Flow (2 Players)

1. **Player 1 joins** → Creates room (state: PENDING)
2. **Player 2 joins** → Room fills up
3. **Start game** → Room state changes to STARTED
4. **Player 1 buys car** → Turn step: TUNING_SELECTION
5. **Player 1 skips tuning** → Turn passes to Player 2
6. **Player 2 buys car** → Turn step: TUNING_SELECTION
7. **Player 2 buys tuning** → Apply tuning to car
8. Continue with selling phase...

### Scenario 2: Testing Balance Validation

Try to buy a car that costs more than player's balance (1300):
```json
{
  "roomId": 1,
  "telegramId": "player1",
  "carId": 50
}
```
Should return error: "Insufficient balance"

### Scenario 3: Multiple Rooms

Create different players with different `telegramId` values to test multiple concurrent rooms.

## Tips for Testing

1. **Use GET /api/game/room/{roomId}** frequently to check current state
2. **Watch the turn step** in the response - it tells you what action is expected next
3. **Check player balance** after each purchase
4. **Inspect available cars/tunings** to know valid IDs for next actions
5. **Use browser DevTools Network tab** to see full request/response payloads

## Response Structure

All game action endpoints return `RoomStateDto` containing:

```json
{
  "id": 1,
  "state": "STARTED",
  "turnStep": "CAR_SELECTION",
  "currentPlayerIndex": 0,
  "players": [
    {
      "telegramId": "player1",
      "username": "TestPlayer1",
      "balance": 1300,
      "garage": [],
      "totalProfit": 0,
      "soldCars": 0
    }
  ],
  "cars": [ /* available cars in market */ ],
  "clients": [ /* available clients */ ],
  "tunings": [ /* available tunings */ ]
}
```

## Error Handling

Common errors you might encounter:

- **404 Room not found**: Invalid roomId
- **400 Insufficient balance**: Not enough money for purchase
- **400 Garage full**: Player already has maximum cars
- **400 Invalid turn**: Not the player's turn or wrong turn step
- **400 Car not found**: Invalid carId
- **400 Tuning not found**: Invalid tuningId

## Advanced Testing with Postman/Insomnia

You can import the OpenAPI spec from http://localhost:8080/api-docs into Postman or Insomnia for more advanced testing:

1. Open Postman/Insomnia
2. Import → OpenAPI/Swagger
3. Enter URL: http://localhost:8080/api-docs
4. All endpoints will be automatically imported with examples

## Comparing REST vs WebSocket

**REST API** (this Swagger interface):
- Synchronous request/response
- No real-time updates
- Good for testing individual actions
- Easier to debug

**WebSocket API** (frontend uses this):
- Real-time bidirectional communication
- Server pushes updates to all clients
- Used in production for multiplayer sync
- More complex to test

Use Swagger for testing backend logic, then integrate with WebSocket for real-time features.
