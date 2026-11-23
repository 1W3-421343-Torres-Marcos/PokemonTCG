---
name: web-app-core
description: Usar al desarrollar el shell de Angular, routing, guards JWT, Auth (/login, /register), Home (/), Catálogo (/catalog) o Deck Builder (/decks, /decks/:id).
---

# Módulo: `web-app-core`

## Alcance
Núcleo de la SPA Angular 18+ en `/frontend` fuera del tablero de combate: layout base, navegación, sesión JWT, Home, Catálogo local y Deck Builder.

## Especificaciones técnicas
- **Arquitectura Angular**: Standalone Components, `ChangeDetectionStrategy.OnPush`, Signals (`signal`, `computed`, `effect`), `@if`/`@for`/`@switch`, `ReactiveFormsModule`, `provideRouter` y `provideHttpClient(withInterceptors([jwtInterceptor]))`.
- **Rutas y pantallas**:
  - **Auth (`/login`, `/register`)**: formularios reactivos con validación de formato/longitud, manejo de errores de API, persistencia de sesión en `AuthStore` y protección de rutas privadas con `authGuard` (y `guestGuard` para usuarios autenticados).
  - **Home (`/`)**: accesos directos a **Catálogo** (`/catalog`), **Mis Mazos** (`/decks`) y **Jugar vs. CPU** (`/play/setup`), resumen de estadísticas PvE del usuario y botón/banner destacado para **Retomar partida en curso** (`/play/:matchId`) cuando exista una partida activa (`IN_PROGRESS`).
  - **Catálogo (`/catalog`)**: grilla paginada de cartas (`CardTile`), buscador por nombre con debounce, filtros combinables por tipo de energía y categoría (`Pokemon`, `Trainer`, `Energy`), y modal de inspección ampliada (`CardDetailModal`).
  - **Deck Builder (`/decks`, `/decks/:id`)**:
    - `/decks`: listado de mazos propios (badge `Válido 60/60` o `Borrador`, editar/eliminar) y listado de mazos prearmados (*starter decks*) clonables.
    - `/decks/:id`: editor a **2 paneles** (izquierda: buscador y filtros del catálogo; derecha: lista agrupada del mazo actual con controles `+/-`) y **barra de validación en vivo (`DeckValidationBar`)**: contador `N/60`, máximo 4 copias por nombre (salvo Energías Básicas) y mínimo 1 Pokémon Básico.
- **Stores compartidos (`core/auth/`, `features/`)**: `AuthStore`, `CatalogStore` y `DeckBuilderStore` basados en Signals.

## Reglas de negocio
- **Cero llamadas externas a TCGdex**: toda consulta de catálogo y mazos va exclusivamente a `/api/**` del backend.
- **Cliente tonto con guía visual**: `DeckValidationBar` informa requisitos faltantes en vivo y permite guardar borradores (`isValid = false`), pero la validación definitiva reside en el backend.

## Archivos que toca
- `frontend/src/styles/**`
- `frontend/src/app/core/**`
- `frontend/src/app/shared/**`
- `frontend/src/app/features/home/**`
- `frontend/src/app/features/auth/**`
- `frontend/src/app/features/catalog/**`
- `frontend/src/app/features/deck-builder/**`

## Criterios de aceptación
- [x] Layout base, tokens de `docs/design.md`, Home (`/`) con accesos rápidos y `/catalog` paginado/filtrable con modal de detalle operativos en F1.
- [x] `/login` y `/register` con formularios reactivos, `AuthStore`, interceptor JWT y `authGuard` operativos en F2.
- [x] `/decks` (propios y *starter decks* clonables) y editor a 2 paneles `/decks/:id` con `DeckValidationBar` en vivo operativos en F2.
- [x] Opción de retomar partida activa (`IN_PROGRESS`) desde Home (`/`) operativa en F3.
