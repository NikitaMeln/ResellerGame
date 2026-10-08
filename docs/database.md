# Database & Persistence

Read when: working with JPA entities, repositories, or `application.properties`.

## Hybrid Persistence Model (full detail)

The system uses a **dual-layer persistence architecture** that separates permanent player data from temporary game state. See CLAUDE.md for the one-paragraph summary; this is the detail.

### Database Layer (Persistent - PostgreSQL/H2)
Stores data that survives across games:
- **Player profiles**: Career statistics (total profit, cars sold)
- **Game room metadata**: Room ID, state (PENDING/STARTED/FINISHED), player list, player queue
- **Reference data templates**: Car, Client, and Tuning definitions (loaded from JSON files)

**Key entities**: `GameRoom`, `Player`, `Car`, `Client`, `Tuning`

### In-Memory Session Layer (Temporary - ConcurrentHashMap)
Stores game state that only exists during active gameplay:
- **GameSession**: Active game instances (one per room)
- **PlayerGameState**: Player's temporary balance, garage, purchased cars
- **CarInstance**: Individual car with applied tunings
- **Game mechanics**: Current turn, available card pools, turn step

**Stored in**: `GameSessionServiceImpl` uses `ConcurrentHashMap<Long, GameSession>`
**Lifecycle**: Created when room starts, destroyed when game ends
**Critical**: Session state is lost on server restart

## Database Configuration

**Current Configuration** (`application.properties`):
- PostgreSQL database configured by default
- Connection: `jdbc:postgresql://localhost:5433/reseller_game`
- Username: `postgres`, Password: `19738246`
- Schema recreates on startup via `spring.jpa.hibernate.ddl-auto=create`
- H2 dependency available for switching to in-memory database

**Important**: `create` rebuilds the schema on every application startup. All data is lost. Change to `update` for persistent development data.

**To Switch to H2 (Development)**:
Edit `demo/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
# Remove PostgreSQL settings
```

## Backend Project Structure

```
demo/src/main/java/com/reseller/game/
├── config/          # WebSocketConfig, DataConfig, I18nConfig
├── controller/      # RoomController (WebSocket @MessageMapping), GameRestController (REST endpoints under /api/game for Swagger-based testing of the same game actions)
├── dto/req/         # Request DTOs (e.g., JoinRoomRequest, BuyCarRequest, BuyTuningRequest)
├── dto/res/         # Response DTOs
├── dto/             # Core DTOs (RoomStateDto, PlayerDto, CarDto, ClientDto, TuningDto)
├── mapper/          # MapStruct interfaces (implementations auto-generated)
├── model/entity/    # JPA entities: GameRoom, Player, Car, Client, Tuning
├── session/         # In-memory game state classes (GameSession, PlayerGameState, CarInstance)
├── repository/      # Spring Data JPA repositories
├── service/         # Service interfaces
├── service/impl/    # Service implementations (business logic)
├── exception/       # Custom exceptions (InsufficientBalanceException, GarageFullException, etc.)
└── util/            # InitializeStorage, JsonMapConverter, RandomSelectionUtil

demo/src/main/resources/data/
├── cars.json               # 50 car templates (model, year, price)
├── client.json             # 50 client profiles (name, budget, yearForPurchase, randomCounter, stockOrNot)
└── tuning-data-en.json     # Positive/negative tunings with properties
```

## Car Template vs Instance Pattern

```java
// Database entity: reference template (read-only, reusable)
Car carTemplate = carRepository.findById(1L);

// In-memory instance: specific purchased car (owned by player)
CarInstance carInstance = CarInstance.fromTemplate(carTemplate);
carInstance.addTuning(tuning);  // Applied to THIS instance only
```

Templates remain unchanged in database. Each purchase creates a new `CarInstance` in the `PlayerGameState.garage`.

## Tuning Properties Storage

Tunings store dynamic properties as `Map<String, Object>` for flexibility:
```json
{
  "name": "Restoration",
  "type": "POSITIVE",
  "properties": {
    "priceEffect": 500,
    "addToRandomModifier": -3
  }
}
```
Converted via `JsonMapConverter` for JPA persistence. Accessed in code via `tuning.getProperties().get("priceEffect")`.
