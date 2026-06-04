# Webpecker

Webpecker is a small web UI for running repeated HTTP GET requests against a list of URLs and visualizing timing and status data in real time. It uses a Spring Boot backend with an OkHttp3 client and a Vue 3 frontend with charts.

## Features
- Load a list of URLs and run request loops per URL.
- Control repeat count, delay between requests, timeout, and max concurrency.
- Live charts for response time, distribution, and request stages.
- WebSocket-based status/events stream.

## Tech stack
- Backend: Spring Boot + WebSocket + OkHttp3
- Frontend: Vue 3 + PrimeVue + ECharts
- Build: Gradle (multi-module) + Node

## Quick start (build + run)
1) Build and run the backend (serves the built frontend). This will also install frontend dependencies and build the UI on first run:
```bash
./gradlew :backend:bootRun
```
On Windows:
```bash
.\gradlew.bat :backend:bootRun
```
2) Open the app at:
```
http://localhost:8080
```

## WebSocket API (overview)
Endpoint:
```
ws://<host>/req
```

Client → server (examples):
```json
{ "action": "restore-state" }
{ "action": "reset-http-client" }
{ "action": "send-request", "id": 0, "url": "https://example.com", "repeat": 100 }
{ "action": "cancel-request", "id": 0 }
{ "action": "cancel-request" }
{ "action": "update-config", "delay": 100, "timeout": 600, "maxConcurrent": 3 }
```

Server → client: batches of JSON objects, including:
- State updates: `{ "id": 0, "state": "running" }`
- Iteration updates: `{ "id": 0, "iteration": 1, "result": "200" }`
- Timing events: `{ "id": 0, "iteration": 1, "event": "dnsStart", "time": 1700000000000, "msFromStart": 12 }`

## Project structure
```
backend/   Spring Boot app and WebSocket server
frontend/  Vue 3 UI
```
