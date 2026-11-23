---
name: web-client
description: Usar al desarrollar la aplicación Angular 18+ en /frontend (catálogo, auth, deck builder, tablero PvE con Signals y animación de GameEvent).
---

# Módulo: `web-client`

## Alcance
Aplicación SPA Angular 18+ en `/frontend` actuando como **cliente reactivo puro ("cliente tonto")** sobre `docs/design.md`: catálogo local, autenticación JWT, constructor de mazos, selector PvE y tablero animado con historial de eventos.

## Especificaciones técnicas
- **Arquitectura Angular**: Standalone Components, `ChangeDetectionStrategy.OnPush`, Signals (`signal`, `computed`, `effect`), control flow `@if`/`@for`/`@switch`, `provideHttpClient` con interceptor JWT funcional y `authGuard`.
- **Estructura de carpetas (`frontend/src/app/`)**:
  - `core/`: `auth/` (`AuthStore`, `jwtInterceptor`, `authGuard`), `api/` (servicios HTTP tipados hacia `/api/**`), `models/` (DTOs espejo del backend).
  - `shared/ui/`: componentes reutilizables de `docs/design.md` (`CardTile`, `CardDetailModal`, `EnergyBadge`, `HpBar`, `StatusBadge`, `ActionButton`, `EventLogFeed`, `DeckCounterBar`).
  - `features/`: `catalog/`, `auth/` (login/register/perfil), `deck-builder/`, `battle/` (`MatchLobbyComponent`, `BattleBoardComponent`, `EventAnimatorService`).
  - `styles/`: `_tokens.scss` con CSS custom properties de `docs/design.md`.
- **Stores reactivos con Signals**: `AuthStore`, `CatalogStore`, `DeckBuilderStore` y `MatchStore`.
- **Animador de eventos (`EventAnimatorService`)**: consume `List<GameEvent>` secuencialmente (`450–700ms`/evento), ilumina objetivo/daño, actualiza la vista paso a paso, bloquea acciones del usuario durante la reproducción y ofrece toggle `1x`/`2x` y botón *Saltar*.

## Reglas de negocio
- **Cero lógica de dominio en frontend**: no calcular daño, debilidades, legalidad de jugadas ni IA en TypeScript; solo deshabilitar controles evidentes por fase/turno según `PlayerViewDTO` y enviar `GameAction` al backend.
- **Origen exclusivo de datos/imágenes**: consultar únicamente `/api/**` del backend; nunca invocar `api.tcgdex.net` ni `pokemontcg.io`.
- **Fidelidad visual**: respetar paleta, proporción `5 / 7` de cartas y tablero desktop `100dvh` sin scroll definidos en `docs/design.md`.

## Archivos que toca
- `frontend/src/styles/**`
- `frontend/src/app/**`

## Criterios de aceptación
- [ ] Shell con navegación, tokens de `docs/design.md` y catálogo paginado/filtrable operativo en F1.
- [ ] Login/registro JWT, interceptor, `authGuard` y Deck Builder con contador `60/60` + clonado de starters operativos en F2.
- [ ] Lobby PvE y tablero jugable de principio a fin contra la CPU con `EventLogFeed` operativo en F3.
- [ ] Acciones completas (`EVOLVE`, `RETREAT`, `PLAY_TRAINER`), badges de estado y cola de animaciones de `GameEvent` operativos en F4.
