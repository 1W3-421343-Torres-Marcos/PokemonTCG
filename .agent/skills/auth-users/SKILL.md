---
name: auth-users
description: Usar al implementar registro, login con JWT, Spring Security stateless, perfil de usuario, cartas favoritas y estadísticas PvE.
---

# Módulo: `auth-users`

## Alcance
Gestión de identidad, autenticación JWT stateless, perfil del jugador, lista de cartas favoritas y registro de estadísticas PvE.

## Especificaciones técnicas
- **Esquema SQL (`V2__users_schema.sql`)**:
  - `users`: `id` (`UUID` PK), `username` (`UNIQUE`), `email` (`UNIQUE`), `password_hash`, `role` (`USER` | `ADMIN`), `matches_played`, `matches_won`, `created_at`.
  - `user_favorite_cards`: `user_id` (FK -> `users`), `card_id` (FK -> `cards`), PK compuesta `(user_id, card_id)`, `created_at`.
- **Seguridad (`Spring Security`)**:
  - Sesiones `SessionCreationPolicy.STATELESS`, CSRF deshabilitado para API REST, contraseñas hasheadas con `BCryptPasswordEncoder`.
  - `JwtService` + `JwtAuthenticationFilter` con firma HS256, secreto inyectado por configuración y TTL configurable (default `24h`).
  - Rutas públicas: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/sets/**`, `GET /api/cards/**`.
  - Rutas protegidas: `/api/users/**`, `/api/decks/**`, `/api/matches/**`.
  - Ruta restringida (`ADMIN` o perfil `dev`): `POST /api/admin/catalog/sync`.
- **Endpoints REST**:
  - `POST /api/auth/register` -> `201 Created` con `{ token, user }`.
  - `POST /api/auth/login` -> `200 OK` con `{ token, user }`.
  - `GET /api/users/me` -> perfil, estadísticas (`matchesPlayed`, `matchesWon`) e IDs de favoritas.
  - `PUT /api/users/me/favorites/{cardId}` / `DELETE /api/users/me/favorites/{cardId}` -> marca/desmarca carta favorita.
- **Contrato interno**: `UserStatsService.recordMatchResult(UUID userId, boolean won)` invocado por `match-api` al finalizar una partida.

## Reglas de negocio
- **Privacidad**: nunca devolver `password_hash` en DTOs ni logs; errores de login responden `401` uniforme sin revelar si falló email o contraseña.
- **Unicidad**: `username` y `email` duplicados devuelven `409 Conflict`.
- **Colección abierta en MVP**: todo usuario autenticado puede usar cualquier carta sincronizada del catálogo al construir mazos; las favoritas son solo marcadores de perfil.

## Archivos que toca
- `database/migrations/V2__users_schema.sql`
- `backend/src/main/java/com/pokemontcg/auth/**`
- `backend/src/test/java/com/pokemontcg/auth/**`

## Criterios de aceptación
- [ ] Migración `V2__users_schema.sql` aplicada limpiamente.
- [ ] Registro y login emiten JWT válido y autentican rutas protegidas con `JwtAuthenticationFilter`.
- [ ] Endpoints de perfil (`/api/users/me`) y favoritas validados por tests de seguridad/controlador.
- [ ] `UserStatsService.recordMatchResult` actualiza `matches_played` y `matches_won` tras fin de partida.
