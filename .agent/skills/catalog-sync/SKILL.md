---
name: catalog-sync
description: Usar al trabajar en la sincronización con TCGdex API v2, el esquema local de sets/cartas en PostgreSQL o los endpoints REST del catálogo.
---

# Módulo: `catalog-sync`

## Alcance
Integración servidor-servidor con TCGdex API v2, almacenamiento local en PostgreSQL y exposición REST del catálogo para Deck Builder, combate y cliente Angular.

## Especificaciones técnicas
- **Cliente externo (`TcgdexClient`)**: `RestClient` de Spring apuntando a `https://api.tcgdex.net/v2/{lang}/sets/{setId}` y `/cards/{cardId}` (set inicial: `base1`, idioma por defecto `en`).
- **Esquema SQL (`V1__catalog_schema.sql`)**:
  - `card_sets`: `id` (PK, ej. `base1`), `name`, `series`, `release_date`, `total_cards`, `logo_url`, `synced_at`.
  - `cards`: `id` (PK, ej. `base1-4`), `set_id` (FK), `local_id`, `name`, `category` (`Pokemon` | `Trainer` | `Energy`), `hp`, `stage` (`Basic` | `Stage1` | `Stage2`), `evolves_from`, `retreat_cost`, `rarity`, `illustrator`, `image_low_url`, `image_high_url`, `types` (`JSONB`), `attacks` (`JSONB`), `weaknesses` (`JSONB`), `resistances` (`JSONB`), `effect_text`, `raw_payload` (`JSONB`).
  - Índices recomendados sobre `name`, `set_id`, `category` y `stage`.
- **Normalización de imágenes**: construir y persistir `image_low_url` (`{image}/low.webp`) e `image_high_url` (`{image}/high.webp`) cuando TCGdex devuelva URL base; permitir `null` seguro si falta imagen.
- **Endpoints REST**:
  - `GET /api/sets`: lista sets sincronizados.
  - `GET /api/cards`: búsqueda paginada (`page`, `size`) con filtros opcionales `q` (nombre), `setId`, `category`, `type`, `stage`.
  - `GET /api/cards/{id}`: detalle completo de carta.
  - `POST /api/admin/catalog/sync?setId=base1&lang=en`: sincronización manual idempotente bajo demanda.

## Reglas de negocio
- **Sin llamadas externas en tiempo de juego**: el frontend y el resto de módulos consultan únicamente PostgreSQL local; prohibido consumir `pokemontcg.io`.
- **Upsert idempotente**: re-sincronizar un set actualiza registros existentes por `id` sin duplicar filas ni romper FKs desde mazos o favoritos.
- **Tolerancia a campos opcionales**: cartas de Energía y Entrenador carecen de `hp`, `stage` o `attacks`; persistir `null` o colecciones vacías sin fallar la deserialización.

## Archivos que toca
- `database/migrations/V1__catalog_schema.sql`
- `backend/src/main/java/com/pokemontcg/catalog/**`
- `backend/src/test/java/com/pokemontcg/catalog/**`

## Criterios de aceptación
- [x] Migración `V1__catalog_schema.sql` aplicada limpiamente en PostgreSQL vía Flyway.
- [x] Sincronización de `base1` completada e idempotente (re-ejecutar mantiene conteo exacto sin errores).
- [x] Endpoints `GET /api/sets`, `GET /api/cards` (filtros + paginación) y `GET /api/cards/{id}` verificados.
- [x] Tests unitarios de `TcgdexClient` / servicio de sincronización con payload mockeado en verde.
