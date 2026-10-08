# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multiplayer reseller simulation game with Spring Boot backend and Angular 19 frontend. Players join game rooms via WebSocket to simulate car reselling: buying cars, applying tuning, and selling to clients with dice-roll mechanics to achieve profit goals.

## Critical Architecture Concept: Hybrid Persistence Model

**This is the most important concept to understand when working with this codebase.** The system uses a **dual-layer persistence architecture**: permanent player/room data lives in the database (`GameRoom`, `Player`, `Car`, `Client`, `Tuning`), while active-game state (balance, garage, current turn, card pools) lives only in memory (`GameSession`, `PlayerGameState`, `CarInstance`) inside `GameSessionServiceImpl`'s `ConcurrentHashMap<Long, GameSession>` — and is lost on server restart. Always ask: is this field persistent (DB) or temporary (session)?

Full detail, entity list, and project structure: see [docs/database.md](docs/database.md) — read when working with JPA entities, repositories, or `application.properties`.

## Development Commands

### Backend (from `demo/` directory)
```bash
# Compile (required after entity changes for MapStruct)
mvn clean compile

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=GameSessionSellingTest

# Package WAR
mvn clean package
```

**Important**:
- Run `mvn clean compile` after modifying entities to regenerate MapStruct mappers in `target/generated-sources/annotations/`
- Annotation processor order matters: Lombok → Lombok-MapStruct binding → MapStruct (configured in pom.xml)
- Backend tests live under `demo/src/test/java/com/reseller/game/service/impl/` — plain JUnit 5 + Mockito + AssertJ, no `@SpringBootTest` (the main `application.properties` points at a live PostgreSQL with no test profile). `GameSessionTestSupport` is the shared fixture.

### Frontend (from `frontend/` directory)
```bash
# Install dependencies (required on first setup)
npm install

# Development server (proxies to backend at :8080)
npm start
# Opens at http://localhost:4200

# Production build
npm run build

# Run tests
npm test

# Watch mode for development
npm run watch
```

### Full Stack Development
1. Start backend: `cd demo && mvn spring-boot:run` (runs on :8080)
2. Start frontend: `cd frontend && npm start` (runs on :4200)
3. Access application at http://localhost:4200

**Note**: Frontend uses `proxy.conf.json` to route `/api/*` and `/rg/*` (WebSocket) requests to backend at :8080

API testing (Swagger, REST mirror of WebSocket actions): see [docs/api.md](docs/api.md) — read when adding/testing an endpoint.

## Claude Assistant Guidelines

- Follow SOLID principles and clean code practices
- Use Java 17 patterns (records, text blocks, pattern matching where appropriate)
- Service layer contains business logic; controllers handle routing and WebSocket messaging only
- MapStruct mappers bridge entities and DTOs (never manually convert) — see [docs/architecture.md](docs/architecture.md) for the one hand-written exception and why
- Lombok reduces boilerplate (@Getter, @Setter, @Builder, @RequiredArgsConstructor)
- Always understand whether data lives in database (persistent) or in-memory session (temporary)
- Do not run the project self
- Do not comment anywhere, only in the hard methods

## Further Reading (load on demand)

- [docs/database.md](docs/database.md) — persistence model detail, DB config, H2 switch, backend project structure, car template/instance pattern, tuning properties storage. Read when working with entities, repositories, or `application.properties`.
- [docs/architecture.md](docs/architecture.md) — backend/frontend stack, WebSocket broadcasting & communication patterns, MapStruct conventions. Read when touching mappers or WebSocket broadcast code.
- [docs/game-mechanics.md](docs/game-mechanics.md) — turn steps, buying/selling phase order, dice/profit rules, known design gaps. Read when changing turn progression or sale/profit logic in `GameSessionServiceImpl`.
- [docs/frontend.md](docs/frontend.md) — Angular zone-management pattern (available pools / zone snapshots / purchased state), frontend project structure. Read when touching card rendering in `game-room.component.ts`.
