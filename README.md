![img](logo/Logo_first.png)

# ResellerGame

Multiplayer reseller simulation game. Players join a game room over WebSocket to simulate car reselling: buying cars, applying tuning, and selling to clients with dice-roll mechanics to reach a profit goal.

- **Backend**: Spring Boot 3.4.5, Java 17, WebSocket/STOMP, PostgreSQL (H2 for dev)
- **Frontend**: Angular 19, standalone components, SSR

## Quick Start

```bash
# Backend (from demo/) - runs on :8080
cd demo && mvn spring-boot:run

# Frontend (from frontend/) - runs on :4200, proxies to backend
cd frontend && npm install && npm start
```

Open <http://localhost:4200>.

## Documentation

- [CLAUDE.md](CLAUDE.md) — architecture overview, dev commands, and guidelines for working in this codebase
- [docs/](docs/) — detailed reference: persistence model, mapper conventions, game mechanics, frontend patterns
- [demo/SWAGGER_GUIDE.md](demo/SWAGGER_GUIDE.md) — REST API testing via Swagger UI
- [NOTES.md](NOTES.md) — known gaps and polish backlog
