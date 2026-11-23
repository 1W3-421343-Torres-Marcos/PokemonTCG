# Tablero de Progreso — Pokémon TCG Web (PvE)

**Estados:** `[ ]` pendiente | `[~]` en curso | `[x]` completada

## Resumen de fases

| ID | Fase | Estado | Avance |
| :--- | :--- | :---: | :---: |
| F1 | Monorepo, Infraestructura y Catálogo | `[x]` | `100%` |
| F2 | Usuarios, Deck Builder y Mazos Prearmados | `[x]` | `100%` |
| F3 | Motor PvE Básico (MVP Jugable vs. Máquina) | `[x]` | `100%` |
| F4 | Reglas Completas e IA Mejorada | `[x]` | `100%` |

---

## F1 — Monorepo, Infraestructura y Catálogo

- [x] **F1.1** — Inicializar `/backend` (Spring Boot 3.x + Java 21 + Maven), `/frontend` (Angular 18+ Standalone) y `/database` (`docker-compose.yml` PostgreSQL 16 + integración Flyway). | *Módulos/archivos:* `/backend`, `/frontend`, `/database`. | *Hecho:* PostgreSQL levantado, `mvn test` y `ng build` pasan en verde.
- [x] **F1.2** — Crear migración `V1__catalog_schema.sql`, cliente `RestClient` a TCGdex v2 y servicio de sincronización idempotente del set `base1`. | *Módulos/archivos:* `catalog-sync`, `/database/migrations`. | *Hecho:* set `base1` sincronizado en PostgreSQL y test unitario con mock de TCGdex en verde.
- [x] **F1.3** — Exponer API REST de catálogo (`GET /api/sets`, `GET /api/cards` paginado/filtrable, `GET /api/cards/{id}`, `POST /api/admin/catalog/sync`). | *Módulos/archivos:* `catalog-sync`. | *Hecho:* filtros y paginación verificados por tests de integración/controlador y consulta HTTP.
- [x] **F1.4** — Implementar layout base, routing, Home (`/`) con accesos rápidos, tokens de `docs/design.md`, `CardTile`, `CardDetailModal` y Catálogo (`/catalog`) con grilla paginada, buscador por nombre y filtros por tipo y categoría. | *Módulos/archivos:* `web-app-core`, `docs/design.md`. | *Hecho:* catálogo navegable en navegador consumiendo solo `/api/**` local.

*Entregable usable:* Catálogo web funcional de cartas de Base Set servido íntegramente desde PostgreSQL.

---

## F2 — Usuarios, Deck Builder y Mazos Prearmados

- [x] **F2.1** — Crear migración `V2__users_schema.sql`, seguridad stateless JWT + BCrypt y endpoints `/api/auth/register`, `/api/auth/login`, `/api/users/me` y favoritas. | *Módulos/archivos:* `auth-users`, `/database/migrations`. | *Hecho:* flujo registro/login/token verificado y tests de seguridad en verde.
- [x] **F2.2** — Crear migración `V3__decks_schema.sql`, validador puro `DeckValidator` (60 cartas, máx. 4 copias por nombre salvo Energías Básicas, ≥1 Pokémon Básico) y CRUD `/api/decks`. | *Módulos/archivos:* `deck-builder`, `/database/migrations`. | *Hecho:* batería JUnit 5 de reglas de mazo y tests CRUD en verde.
- [x] **F2.3** — Implementar seeder idempotente de ≥2 mazos prearmados (`starter decks` válidos de `base1` reutilizables por jugador y CPU) + endpoint de listado y clonado. | *Módulos/archivos:* `deck-builder`. | *Hecho:* starters cargados pasan `DeckValidator` y se listan/clonan vía API.
- [x] **F2.4** — Implementar Auth (`/login`, `/register`) con formularios reactivos, `AuthStore` (Signals), interceptor JWT, `authGuard` y resumen de perfil en Home (`/`). | *Módulos/archivos:* `web-app-core`, `auth-users`. | *Hecho:* usuario se autentica en la UI, persiste sesión y accede a rutas protegidas.
- [x] **F2.5** — Implementar Deck Builder: listado de mazos propios y starters clonables (`/decks`) y editor a 2 paneles (`/decks/:id`) con barra de validación en vivo (60 cartas, máx. 4 por nombre salvo Energías Básicas, ≥1 Básico). | *Módulos/archivos:* `web-app-core`, `deck-builder`. | *Hecho:* usuario arma/edita/guarda un mazo válido o clona un starter desde el navegador.

*Entregable usable:* Sistema de cuentas JWT + constructor de mazos validados con mazos prearmados listos para combatir.

---

## F3 — Motor PvE Básico (MVP Jugable vs. Máquina)

- [x] **F3.1** — Modelar `GameState`, `PlayerState`, `GameAction`, `GameEvent`, proyección `PlayerViewDTO` (ocultando mano/mazo rival) y máquina de estados de turno en Java puro. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 validan transiciones de fase y ausencia de fugas de información oculta.
- [x] **F3.2** — Implementar flujo jugable núcleo en `game-engine` (setup + mulligan, 6 premios, robo, bajar Básicos a banca, 1 energía/turno, ataque daño base + costo, KO, premios, promoción y victoria) emitiendo `GameEvent`. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 corren partidas núcleo en milisegundos sin Spring ni BD.
- [x] **F3.3** — Implementar interfaz `BotStrategy`, `BasicBotStrategy` y `BotTurnRunner` en Java puro usando solo `PlayerViewDTO` del Bot y emitiendo `GameAction` válidos. | *Módulos/archivos:* `bot-ai`, `game-engine`. | *Hecho:* test JUnit 5 simula partida completa Bot vs. Bot sin errores ni bucles infinitos.
- [x] **F3.4** — Crear migración `V4__matches_schema.sql` (snapshot `JSONB` + `@Version`) y API REST PvE (`POST /api/matches`, `GET /api/matches/{id}`, `POST /api/matches/{id}/actions`, `POST /api/matches/{id}/concede`) que ejecuta acción humana + turno Bot y devuelve `PlayerViewDTO` + `List<GameEvent>`. | *Módulos/archivos:* `match-api`, `/database/migrations`. | *Hecho:* partida completa verificada por tests de API/controlador con eventos humano+CPU.
- [x] **F3.5** — Implementar Setup de Partida (`/play/setup`), botón de retomar partida activa en Home (`/`) y Tablero de Combate (`/play/:matchId`) con Zona Rival, Zona Jugador, HUD de ataques/turno/fase, descarte inspeccionable, Historial de Combate secuencial (`GameEvent` del Bot) y modal de Victoria/Derrota. | *Módulos/archivos:* `web-battle-board`, `web-app-core`, `match-api`. | *Hecho:* usuario completa una partida jugable de principio a fin contra la CPU desde la web.

*Entregable usable:* Partida PvE jugable de principio a fin en navegador contra la CPU con historial de eventos.

---

## F4 — Reglas Completas e IA Mejorada

- [x] **F4.1** — Añadir evoluciones (Stage 1/2 con restricción de turno), retirada con pago de energía y cálculo completo de Debilidades (`x2`) y Resistencias (`-30`) en `game-engine`. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 cubren cadenas evolutivas, retirada y modificadores de tipo.
- [x] **F4.2** — Implementar los 5 estados alterados (`POISONED`, `BURNED`, `ASLEEP`, `PARALYZED`, `CONFUSED`) con `RandomProvider` inyectable, `POKEMON_CHECKUP` y limpieza al evolucionar/retirar. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 deterministas validan cada estado y sus interacciones.
- [x] **F4.3** — Implementar `CardEffectRegistry` (Strategy/Registry por ID TCGdex) con bloques componibles (daño, robo, curación, estado, cambio, descarte energía) y efectos clave de Entrenadores y ataques de `base1`. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 validan efectos registrados sin condicionales monolíticos.
- [x] **F4.4** — Implementar `TacticalBotStrategy` (`BASIC`/`TACTICAL` en `bot-ai` y `match-api`) evaluando evoluciones, ventaja de tipo, retirada defensiva y uso de Entrenadores. | *Módulos/archivos:* `bot-ai`, `match-api`. | *Hecho:* simulación JUnit 5 Táctico vs. Básico valida legalidad y ventaja táctica.
- [x] **F4.5** — Integrar en `/play/:matchId` acciones completas (`EVOLVE`, `RETREAT`, `PLAY_TRAINER`), resaltado de cartas jugables en mano, íconos de estado alterado, animaciones secuenciales de `List<GameEvent>` y actualización de stats al finalizar partida. | *Módulos/archivos:* `web-battle-board`, `match-api`, `auth-users`. | *Hecho:* partida PvE completa con reglas avanzadas, animaciones legibles y stats persistidas.

*Entregable usable:* Simulador PvE completo con evoluciones, estados alterados, Entrenadores, Bot táctico y animaciones.

---

## Registro de avance

| Fecha | Paso | Resumen de lo hecho y validación ejecutada | Siguiente paso |
| :--- | :--- | :--- | :--- |
| 2026-11-23 | Docs | Estructura modular del monorepo creada (`AGENTS.md`, `docs/*`, 8 skills en `.agent/skills/` con `web-app-core` y `web-battle-board` + pantallas/HUD), reglas Git, `.gitignore` y repositorio inicializado en `main`. | F1.1 |
| 2026-11-23 | F1.1 | Infraestructura base creada (`/database` PostgreSQL 16 + `V0__baseline.sql`, `/backend` Spring Boot 3.3.5 + Flyway sin H2, `/frontend` Angular 18+ Standalone + `_tokens.scss`). Verificado: contenedor `pokemontcg-postgres` healthy, `mvn test` (2/2) y `ng build` en verde. | F1.2 |
| 2026-11-23 | F1.2 | Creada migración `V1__catalog_schema.sql` (`card_sets`, `cards` con `sort_order` y columnas explícitas + `card_types`), `TcgdexClient`, `CatalogSyncService` (HTTP fuera de transacción) y `CatalogPersistenceService` (`@Transactional`). Verificado: Flyway aplicó `V1`, `mvn test` (7/7) en verde y `base1` sincronizado en PostgreSQL (102 cartas: 69 Pokémon, 26 Trainer, 6 Basic Energy, 1 Special Energy). | F1.3 |
| 2026-11-23 | F1.3 | Expuestos DTOs `record`, `CardSpecifications` (filtro `type` sobre `card_types`), `CatalogQueryService` (orden numérico `sort_order`), `CatalogController` (`GET /api/sets`, `GET /api/cards`, `GET /api/cards/{id}`) y `CatalogAdminController`. Verificado: `mvn test` (12/12 incluyendo `@WebMvcTest` e integración con PostgreSQL real) y consulta HTTP en vivo (`102` cartas orden `1..5`, filtro `Fire`, `base1-96` `Special`). | F1.4 |
| 2026-11-23 | F1.4 | Implementada SPA Angular 18+ (`AppShellComponent`, `HomeComponent`, `CatalogStore`, `CatalogPageComponent`, `CardTileComponent`, `CardDetailModalComponent`, `EnergyBadgeComponent`, `FeaturePlaceholderComponent` para `/decks`, `/play/setup`, `/login`, `/register`). Verificado: `ng build`, `ng test` (`2/2` ChromeHeadless) y 0 llamadas externas a TCGdex. **Fase F1 completada (100%).** | F2.1 |
| 2026-11-23 | F2.1 | Creada migración `V2__users_schema.sql` (`users` con índices únicos funcionales `LOWER(username)` y `LOWER(email)` + `user_favorite_cards`), `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig` stateless, `AuthController`, `UserController` y `UserStatsService`. Verificado: Flyway aplicó `V2` en PostgreSQL y `mvn test` (`20/20` tests unitarios, seguridad y PostgreSQL) en verde. | F2.2 |
| 2026-11-23 | F2.2 | Creada migración `V3__decks_schema.sql`, validador puro `DeckValidator` (consolidación de `cardId`, 60 cartas, máx. 4 copias por nombre salvo Energías Básicas `'Basic'`/`'Normal'`, `≥1` Pokémon Básico) y CRUD `/api/decks` (`DeckService`, `DeckController`) con borradores y control `403`. Verificado: Flyway aplicó `V3` y `mvn test` (`27/27`) en verde. | F2.3 |
| 2026-11-23 | F2.3 | Anotados `CatalogStartupInitializer` (`@Order(10)`) y `StarterDeckSeeder` (`@Order(20)`) sembrando 3 *starter decks* de 60 cartas de `base1` (`Brushfire`, `Zap! & Torrent`, `Blackout`) + endpoints `GET /api/decks/starters` y `POST /api/decks/starters/{id}/clone`. Verificado: `mvn test` (`28/28`) en verde comprobando validez, idempotencia y clonado en PostgreSQL. | F2.4 |
| 2026-11-23 | F2.4 | Implementadas vistas `/login` y `/register` con formularios reactivos, `AuthStore` (Signals + persistencia JWT), `jwtInterceptor`, `authGuard`/`guestGuard`, resumen de perfil en Home (`/`) y toggle de favoritas en `CardDetailModalComponent`. Verificado: `ng build` y `ng test` (`4/4` ChromeHeadless) en verde. | F2.5 |
| 2026-11-23 | F2.5 | Implementado Deck Builder en Angular (`DeckListPageComponent` `/decks`, editor a 2 paneles `DeckEditorPageComponent` `/decks/new` y `/decks/:id`, `DeckBuilderStore` y `DeckValidationBarComponent` en vivo) protegido por `authGuard`. Verificado: `ng build` y `ng test` (`6/6` ChromeHeadless) en verde. **Fase F2 completada (100%).** | F3.1 |
| 2026-11-23 | F3.1 | Modelado núcleo `com.pokemontcg.engine` en Java puro mediante `record`s sin prefijos `get*`/`is*` (`GameState`, `PlayerState`, `PokemonInPlay`, `CardDefinition`, `GameAction` sellada, `GameEvent`, `EnergyCostMatcher` y `PlayerViewProjector` con `AttackViewDTO.canPay` y ocultación estricta de mano/mazo/premios rival). Verificado: `mvn test` (`34/34`) incluyendo `EngineArchitectureTest`, `EnergyCostMatcherTest` y `PlayerViewProjectorTest`. | F3.2 |
| 2026-11-23 | F3.2 | Implementado `GameEngine` y `RandomProvider` en Java puro (`initializeMatch`, mulligan por diferencia neta acotado al mazo, `turnNumber` incremental por turno, límite de 5 en banca, 1 energía/turno, bloqueo de ataque en Turno 1, KO + premio + promoción obligatoria y 4 condiciones de victoria). Verificado: `mvn test` (`41/41`), corriendo `GameEngineTest` en `52 ms` sin Spring ni BD. | F3.3 |
| 2026-11-23 | F3.3 | Implementada IA CPU en Java puro (`BotDifficulty`, `BotStrategy`, `BasicBotStrategy` leyendo solo `PlayerViewDTO` + `attack.canPay()`, y `BotTurnRunner` con guardia `MAX_ACTIONS_PER_TURN = 25`). Verificado: `mvn test` (`48/48`) en verde incluyendo `BotArchitectureTest`, `BasicBotStrategyTest` y `BotVsBotSimulationTest` (5 partidas completas de 60 cartas hasta `GAME_OVER`). | F3.4 |
| 2026-11-23 | F3.4 | Creada migración `V4__matches_schema.sql`, `MatchEntity` (`state_json JSONB` + `@Version`), `CatalogCardMapper` (inferencia de energías `base1-96..102` y parseo de daño numérico), `MatchService`, `MatchController` (`/api/matches`, `/active`, `/{id}`, `/actions`, `/concede`), actualización de `UserStatsService` y mapeo `400`/`409` en `GlobalExceptionHandler`. Verificado: Flyway aplicó `V4` en PostgreSQL y `mvn test` (`53/53`) en verde incluyendo `GameStateJsonRoundTripTest`, `CatalogCardMapperTest`, `MatchControllerTest` y `MatchIntegrationTest`. | F3.5 |
| 2026-11-23 | F3.5 | Implementado Setup (`/play/setup`), retomar partida activa en Home (`/`), Tablero `/play/:matchId` (`MatchSetupPageComponent`, `BattleBoardPageComponent`, `MatchStore`, `EventAnimatorService` sin simular estados intermedios, ataques regidos por `attack.canPay`, `HpBarComponent`, `DiscardPileModalComponent`, `EventLogFeedComponent`, `MatchResultModalComponent`). Corregido error `NG0600` y ajuste de `HpBar` tras E2E en navegador (`1366x864`). Verificado: `ng test` (`8/8` ChromeHeadless), `ng build` y flujo completo en vivo sin errores de consola/HTTP. **Fase F3 completada (100%).** | F4.1 |
| 2026-11-23 | F4.1 | Añadidos `ModifierType`, `ActiveModifier` (con expiración `expiresAfterTurn` en fin de turno), `DamageCalculator` (Debilidad `×2`, Resistencia `-30`, `Harden`, `PlusPower`/`Defender`), `EvolveAction` (Stage 1/2 con restricción de turno y limpieza de estados/modificadores), `RetreatAction` (1/turno y pago de costo de retirada), y proyección autoritativa de `canRetreat`, `CardViewDTO.playable` y `validTargetIds`. Verificado: `mvn test` (`56/56`) en verde incluyendo `GameEngineEvolutionRetreatDamageTest`, `EngineArchitectureTest` y `MatchIntegrationTest`. | F4.2 |
| 2026-11-23 | F4.2 | Implementados los 5 estados alterados (`POISONED`, `BURNED`, `ASLEEP`, `PARALYZED`, `CONFUSED`) con `StatusRules` y `RandomProvider` inyectable en `GameEngine`, `POKEMON_CHECKUP` entre turnos (diferenciando `TurnPhase` en `PromoteActiveAction` para evitar doble checkup tras KO en checkup), moneda de confusión (`30` daño propio si falla) y resolución de KO simultáneo (promoción encadenada y desempate determinista). Verificado: `mvn test` (`61/61`) en verde incluyendo `StatusConditionCheckupTest`. | F4.3 |
| 2026-11-23 | F4.3 | Creado paquete puro `com.pokemontcg.engine.effects` (`CardEffectRegistry`, `TrainerEffect`, `AttackEffect`, `EffectContext`, `AttackContext` y 8 bloques componibles `DrawCardsBlock`, `HealBlock`, `ApplyStatusBlock`, `SwitchActiveBlock`, `DiscardEnergyBlock`, `DamageBlock`, `CoinFlipConditionalBlock`, `CombatBuffBlock`) registrando los 10 Entrenadores y los ataques especiales de los 3 starter decks `base1`. Integrado `PlayTrainerAction` y `AttackEffect` en `GameEngine`, helpers en `CardDefinition` y sobrecarga de `PlayerViewProjector`. Verificado: `mvn test` (`67/67`) en verde incluyendo `CardEffectRegistryTest` y `EngineArchitectureTest`. | F4.4 |
| 2026-11-23 | F4.4 | Implementada IA `TacticalBotStrategy` + `BotCardHelper` en Java puro consumiendo únicamente `PlayerViewDTO` (uso de Entrenadores, evoluciones, ventaja `×2`/resistencia `-30`, retirada defensiva/`Switch` y evitación de auto-KO por recoil), conectada a `BotDifficulty.TACTICAL` en `MatchService.resolveStrategy`. Verificado: `mvn test` (`74/74`) en verde incluyendo `TacticalBotStrategyTest`, `BotArchitectureTest`, `MatchIntegrationTest` y `TacticalSimulationTest` (10 partidas `TACTICAL` vs. `BASIC` + 9 permutaciones `TACTICAL` vs. `TACTICAL` entre los 3 arquetipos *starter*). | F4.5 |
| 2026-11-23 | F4.5 | Habilitado **Bot Táctico** (`TACTICAL`) en `/play/setup`, creado `StatusBadgeComponent` en los Activos, e integradas acciones completas (`EVOLVE`, `RETREAT` con `canRetreat`, `PLAY_TRAINER` instantáneo y por objetivos en tablero/mano), resaltado autoritativo vía `card.playable`/`validTargetIds` y animaciones CSS reactivas por evento (`prefers-reduced-motion`). Verificado: `ng test` (`9/9` ChromeHeadless), `ng build`, y prueba E2E en vivo en `/play/:matchId` contra Bot Táctico (`0` errores de consola/HTTP). **Fase F4 completada (100%).** | — |
