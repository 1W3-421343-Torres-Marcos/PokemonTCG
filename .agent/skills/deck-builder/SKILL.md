---
name: deck-builder
description: Usar al trabajar en el CRUD de mazos, las validaciones oficiales de 60 cartas y los starter decks para jugador y CPU.
---

# Módulo: `deck-builder`

## Alcance
Persistencia CRUD de mazos de usuario, motor puro de validación oficial de Pokémon TCG y carga de mazos prearmados (*starter decks*) utilizables por el jugador y la CPU.

## Especificaciones técnicas
- **Esquema SQL (`V3__decks_schema.sql`)**:
  - `decks`: `id` (`UUID` PK), `owner_id` (FK -> `users`, `NULL` en starters de sistema), `name`, `is_starter` (`boolean`), `cpu_eligible` (`boolean`), `is_valid` (`boolean`), `created_at`, `updated_at`.
  - `deck_cards`: `deck_id` (FK -> `decks` ON DELETE CASCADE), `card_id` (FK -> `cards`), `quantity` (`int > 0`), PK compuesta `(deck_id, card_id)`.
- **Validador puro (`DeckValidator`)**:
  - Clase sin dependencias web que recibe las cartas del mazo y sus metadatos (`category`, `stage`, `name`) y retorna `DeckValidationResult(boolean valid, List<String> errors)`.
  - Reglas verificadas:
    1. Exactamente **60 cartas** en total (suma de `quantity`).
    2. Máximo **4 copias por nombre de carta** (agrupando por `card.name`, sin importar variantes de set), **excepto Energías Básicas** (`category == Energy` sin restricción de Energía Especial).
    3. Al menos **1 Pokémon Básico** (`category == Pokemon && stage == Basic`).
    4. Todas las `cardId` existen en el catálogo local y tienen `quantity >= 1`.
- **Seeder de Starters (`StarterDeckSeeder`)**:
  - Carga idempotente al iniciar (si existen cartas del set `base1`) de al menos 2 mazos temáticos válidos de 60 cartas (`is_starter = true`, `cpu_eligible = true`, `owner_id = null`), validados contra `DeckValidator`.
- **Endpoints REST**:
  - `GET /api/decks`: lista mazos del usuario autenticado con estado `isValid`.
  - `GET /api/decks/starters`: lista mazos prearmados disponibles para clonar o enfrentar a la CPU.
  - `GET /api/decks/{id}`, `POST /api/decks`, `PUT /api/decks/{id}`, `DELETE /api/decks/{id}`.
  - `POST /api/decks/starters/{id}/clone`: copia un starter al perfil del usuario como mazo editable.

## Reglas de negocio
- **Borradores permitidos**: un usuario puede guardar un mazo incompleto (`is_valid = false`), pero `match-api` rechazará iniciar partida con un mazo inválido.
- **Propiedad estricta**: solo el `owner_id` puede consultar/editar/eliminar sus mazos privados (`403 Forbidden` ante terceros); los starters de sistema son solo lectura.

## Archivos que toca
- `database/migrations/V3__decks_schema.sql`
- `backend/src/main/java/com/pokemontcg/deck/**`
- `backend/src/test/java/com/pokemontcg/deck/**`

## Criterios de aceptación
- [x] Migración `V3__decks_schema.sql` aplicada limpiamente.
- [x] `DeckValidator` probado con JUnit 5 cubriendo 60 cartas, límite de 4 por nombre, excepción de Energías Básicas y ≥1 Pokémon Básico.
- [x] Al menos 2 starter decks válidos de `base1` sembrados de forma idempotente y utilizables por jugador/CPU.
- [x] CRUD de mazos y clonado de starters protegidos por usuario autenticado y verificados con tests.
