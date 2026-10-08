# Architecture Details

Read when: touching mappers, WebSocket broadcast code, or evaluating a new dependency/stack choice.

## Backend (Spring Boot 3.4.5)
- **Java 17** with Lombok for boilerplate reduction
- **PostgreSQL** production database (H2 in-memory for dev)
- **WebSocket/STOMP** for real-time game communication
- **MapStruct 1.5.5** for entity-DTO mapping (generates mappers at compile time)
- **JPA/Hibernate** with auto-schema updates

## Frontend (Angular 19)
- **Standalone components** architecture (no NgModules)
- **SCSS** styling
- **SSR enabled** with hydration and event replay
- **@stomp/ng2-stompjs** + SockJS for WebSocket client
- **Proxy configuration** routes `/api/*` and `/rg/*` to backend at localhost:8080

## Key Integration Points
- **WebSocket endpoint**: `/rg` with SockJS fallback
- **Message destinations**: `/app` prefix for client → server, `/topic` for broadcasts, `/queue` for player-specific messages
- **Game data**: Loaded from JSON files in `demo/src/main/resources/data/` (cars, clients, tuning options)

## Transaction Management
- Controllers use `@Transactional` to ensure entity collections are properly loaded
- After entity modifications, reload from database to ensure fresh state with initialized collections
- Example pattern in RoomController: modify → reload → convert to DTO → broadcast

## MapStruct + Lombok Integration
- Annotation processor order is critical (see pom.xml)
- Always use `@Builder`, `@Getter`, `@Setter` on entities for MapStruct compatibility
- Generated mappers appear in `target/generated-sources/annotations/` — stale after a plain `compile` if a mapped field was renamed/removed; use `mvn clean compile` when in doubt
- Never manually convert entities to DTOs; always use MapStruct mappers
- `GameSessionMapper` (`@Component`, hand-written) is the exception: it combines two sources (`GameSession` in-memory + `GameRoom` from DB) into one `RoomStateDto`, which is beyond what a single MapStruct `@Mapper` interface expresses cleanly. It delegates the one-source conversions to generated mappers — `CarMapper`, `ClientMapper`, `TuningMapper`, `GameRoomMapper`, and `SessionCardMapper` (maps `CurrentSale`→`CurrentSaleDto` and `CarInstance`→`CarDto`) — and only hand-codes the merge logic itself, such as overlaying `PlayerGameState` (session) onto `PlayerDto` (DB) and revealing a car's `hiddenNegativeCard` only after `SHOW_SECRET_CARD`.
- Never add a field to `RoomStateDto` that exposes a server-only secret (e.g. the remaining negative-card pool) — broadcasts go to every player in the room over `/topic`, with no per-player filtering.

## WebSocket Broadcasting Pattern

After any game state change, follow this pattern:
```java
// 1. Update in-memory session
gameSessionService.buyCar(roomId, telegramId, carId);

// 2. Get updated session
GameSession session = gameSessionService.getSession(roomId);

// 3. Get DB room data
GameRoom room = roomService.getRoomById(roomId);

// 4. Combine into DTO (MapStruct combines both sources)
RoomStateDto dto = gameSessionMapper.toDto(session, room);

// 5. Broadcast to all room subscribers
ws.convertAndSend("/topic/room." + roomId + ".state", dto);
```

**Destination patterns**:
- Room broadcasts: `/topic/room.{roomId}.state`
- Player-specific: `/topic/player.{telegramId}.{event}` (uses `/topic` not `/queue` since no auth)
- Turn notifications: `/topic/room.{roomId}.turn`

## WebSocket Communication Pattern

**Client → Server**: Send to `/app/{destination}`
```java
@MessageMapping("/room.join")  // Client sends to /app/room.join
public void join(JoinRoomRequest req, Principal principal) { ... }
```

**Server → Client**: Broadcast to `/topic/{destination}` or send to `/queue/{destination}`
```java
ws.convertAndSend("/topic/room." + roomId + ".state", roomDto);
```

**Frontend subscribes**:
```typescript
websocketService.subscribe(`/topic/room.${roomId}.state`).subscribe(data => { ... });
```
