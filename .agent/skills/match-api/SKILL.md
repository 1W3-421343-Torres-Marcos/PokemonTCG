---
name: match-api
description: Usar al trabajar en la persistencia de partidas PvE, los endpoints REST de combate, la orquestación del turno CPU y la proyección PlayerViewDTO + GameEvent.
---

# Módulo: `match-api`

## Alcance
Capa Spring Boot que persiste el estado de partidas PvE en PostgreSQL, adapta cartas del catálogo al dominio del motor, ejecuta jugadas humanas + turno CPU en una llamada REST y devuelve `PlayerViewDTO` + `List<GameEvent>`.

## Especificaciones técnicas
- **Esquema SQL (`V4__matches_schema.sql`)**:
  - `matches`: `id` (`UUID` PK), `user_id` (FK -> `users`), `player_deck_id` (FK -> `decks`), `cpu_deck_id` (FK -> `decks`), `bot_difficulty` (`BASIC` | `TACTICAL`), `status` (`IN_PROGRESS` | `FINISHED`), `winner` (`PLAYER` | `CPU` | `null`), `state_json` (`JSONB`), `event_seq` (`bigint`), `version` (`bigint` con `@Version`), `created_at`, `updated_at`.
- **Adaptador (`CatalogCardMapper`)**: traduce `CardEntity` de PostgreSQL a `CardDefinition` inmutable de `game-engine`.
- **Endpoints REST**:
  - `POST /api/matches`: body `{ playerDeckId, cpuDeckId, botDifficulty }` -> crea partida, ejecuta setup inicial y devuelve `MatchTurnResponse`.
  - `GET /api/matches/{id}`: devuelve `MatchTurnResponse` con estado proyectado actual (sin eventos nuevos).
  - `POST /api/matches/{id}/actions`: recibe `GameAction`, ejecuta en `GameEngine`, si cambia turno a CPU ejecuta `BotTurnRunner`, persiste nuevo `state_json`, retorna `MatchTurnResponse(matchId, PlayerViewDTO, List<GameEvent>)`.
  - `POST /api/matches/{id}/concede`: rinde al jugador y cierra la partida.
  - `GET /api/matches/history`: historial breve de partidas del usuario.

## Reglas de negocio
- **Sin WebSockets ni fugas de estado**: comunicación 100% REST síncrona; jamás serializar `GameState` crudo hacia el cliente.
- **Validaciones previas**: el mazo del jugador debe pertenecerle y tener `is_valid = true`; el mazo CPU debe tener `is_valid = true` y `cpu_eligible = true`.
- **Seguridad y concurrencia**: solo el dueño de la partida puede consultarla o actuar (`403 Forbidden`); acciones inválidas del motor devuelven `400 Bad Request`; colisiones de `@Version` devuelven `409 Conflict`.
- **Cierre de partida**: al entrar en `GAME_OVER`, marcar `status = FINISHED`, fijar `winner` e invocar `UserStatsService.recordMatchResult(userId, won)` una única vez.

## Archivos que toca
- `database/migrations/V4__matches_schema.sql`
- `backend/src/main/java/com/pokemontcg/match/**`
- `backend/src/test/java/com/pokemontcg/match/**`

## Criterios de aceptación
- [x] Migración `V4__matches_schema.sql` aplicada limpiamente con `state_json` (`JSONB`) y `@Version`.
- [x] `POST /api/matches` y `POST /api/matches/{id}/actions` devuelven `PlayerViewDTO` (ocultando mano/mazo CPU) y `List<GameEvent>` cronológica.
- [x] Al finalizar una partida (`GAME_OVER` o rendición), el resultado persiste y actualiza estadísticas del usuario en `auth-users`.
- [x] Tests de controlador/integración validan reglas de propiedad (`403`), acción ilegal (`400`) y ciclo completo humano + Bot.
