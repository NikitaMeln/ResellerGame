# API Testing & REST Mirror

Read when: adding or testing an endpoint, or exercising game logic without a STOMP client.

## Swagger

After starting the backend, access Swagger UI for REST API testing:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8080/api-docs

See [../demo/SWAGGER_GUIDE.md](../demo/SWAGGER_GUIDE.md) for detailed testing instructions and examples.

## REST Mirror of the WebSocket Actions

`GameRestController` mirrors the WebSocket actions (join room, start game, buy car, buy tuning, skip action, get room state) as plain REST calls — useful for testing game logic without a STOMP client.
