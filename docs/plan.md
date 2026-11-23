# Tablero de Progreso — Pokémon TCG Web (PvE)

**Estados:** `[ ]` pendiente | `[~]` en curso | `[x]` completada

## Resumen de fases

| ID | Fase | Estado | Avance |
| :--- | :--- | :---: | :---: |
| F1 | Monorepo, Infraestructura y Catálogo | `[ ]` | `0%` |
| F2 | Usuarios, Deck Builder y Mazos Prearmados | `[ ]` | `0%` |
| F3 | Motor PvE Básico (MVP Jugable vs. Máquina) | `[ ]` | `0%` |
| F4 | Reglas Completas e IA Mejorada | `[ ]` | `0%` |

---

## F1 — Monorepo, Infraestructura y Catálogo

- [ ] **F1.1** — Inicializar `/backend` (Spring Boot 3.x + Java 21 + Maven), `/frontend` (Angular 18+ Standalone) y `/database` (`docker-compose.yml` PostgreSQL 16 + integración Flyway). | *Módulos/archivos:* `/backend`, `/frontend`, `/database`. | *Hecho:* PostgreSQL levantado, `mvn test` y `ng build` pasan en verde.
- [ ] **F1.2** — Crear migración `V1__catalog_schema.sql`, cliente `RestClient` a TCGdex v2 y servicio de sincronización idempotente del set `base1`. | *Módulos/archivos:* `catalog-sync`, `/database/migrations`. | *Hecho:* set `base1` sincronizado en PostgreSQL y test unitario con mock de TCGdex en verde.
- [ ] **F1.3** — Exponer API REST de catálogo (`GET /api/sets`, `GET /api/cards` paginado/filtrable, `GET /api/cards/{id}`, `POST /api/admin/catalog/sync`). | *Módulos/archivos:* `catalog-sync`. | *Hecho:* filtros y paginación verificados por tests de integración/controlador y consulta HTTP.
- [ ] **F1.4** — Implementar layout base de Angular, tokens de `docs/design.md`, `CardTile` y pantalla de Catálogo con filtros, paginación y modal de detalle. | *Módulos/archivos:* `web-client`, `docs/design.md`. | *Hecho:* catálogo navegable en navegador consumiendo solo `/api/**` local.

*Entregable usable:* Catálogo web funcional de cartas de Base Set servido íntegramente desde PostgreSQL.

---

## F2 — Usuarios, Deck Builder y Mazos Prearmados

- [ ] **F2.1** — Crear migración `V2__users_schema.sql`, seguridad stateless JWT + BCrypt y endpoints `/api/auth/register`, `/api/auth/login`, `/api/users/me` y favoritas. | *Módulos/archivos:* `auth-users`, `/database/migrations`. | *Hecho:* flujo registro/login/token verificado y tests de seguridad en verde.
- [ ] **F2.2** — Crear migración `V3__decks_schema.sql`, validador puro `DeckValidator` (60 cartas, máx. 4 copias por nombre salvo Energías Básicas, ≥1 Pokémon Básico) y CRUD `/api/decks`. | *Módulos/archivos:* `deck-builder`, `/database/migrations`. | *Hecho:* batería JUnit 5 de reglas de mazo y tests CRUD en verde.
- [ ] **F2.3** — Implementar seeder idempotente de ≥2 mazos prearmados (`starter decks` válidos de `base1` reutilizables por jugador y CPU) + endpoint de listado y clonado. | *Módulos/archivos:* `deck-builder`. | *Hecho:* starters cargados pasan `DeckValidator` y se listan/clonan vía API.
- [ ] **F2.4** — Implementar pantallas de Login/Registro, `AuthStore` (Signals), interceptor JWT, `authGuard` y vista de perfil en Angular. | *Módulos/archivos:* `web-client`, `auth-users`. | *Hecho:* usuario se autentica en la UI, persiste sesión y accede a rutas protegidas.
- [ ] **F2.5** — Implementar pantalla de Deck Builder en Angular con buscador de cartas, contador 60/60, estado de validez en vivo y clonado de starters. | *Módulos/archivos:* `web-client`, `deck-builder`. | *Hecho:* usuario arma/edita/guarda un mazo válido o clona un starter desde el navegador.

*Entregable usable:* Sistema de cuentas JWT + constructor de mazos validados con mazos prearmados listos para combatir.

---

## F3 — Motor PvE Básico (MVP Jugable vs. Máquina)

- [ ] **F3.1** — Modelar `GameState`, `PlayerState`, `GameAction`, `GameEvent`, proyección `PlayerViewDTO` (ocultando mano/mazo rival) y máquina de estados de turno en Java puro. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 validan transiciones de fase y ausencia de fugas de información oculta.
- [ ] **F3.2** — Implementar flujo jugable núcleo en `game-engine` (setup + mulligan, 6 premios, robo, bajar Básicos a banca, 1 energía/turno, ataque daño base + costo, KO, premios, promoción y victoria) emitiendo `GameEvent`. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 corren partidas núcleo en milisegundos sin Spring ni BD.
- [ ] **F3.3** — Implementar interfaz `BotStrategy`, `BasicBotStrategy` y `BotTurnRunner` en Java puro usando solo `PlayerViewDTO` del Bot y emitiendo `GameAction` válidos. | *Módulos/archivos:* `bot-ai`, `game-engine`. | *Hecho:* test JUnit 5 simula partida completa Bot vs. Bot sin errores ni bucles infinitos.
- [ ] **F3.4** — Crear migración `V4__matches_schema.sql` (snapshot `JSONB` + `@Version`) y API REST PvE (`POST /api/matches`, `GET /api/matches/{id}`, `POST /api/matches/{id}/actions`, `POST /api/matches/{id}/concede`) que ejecuta acción humana + turno Bot y devuelve `PlayerViewDTO` + `List<GameEvent>`. | *Módulos/archivos:* `match-api`, `/database/migrations`. | *Hecho:* partida completa verificada por tests de API/controlador con eventos humano+CPU.
- [ ] **F3.5** — Construir Lobby PvE y Tablero interactivo en Angular con Signals (Activo, Banca, Mano, Mazo, Descarte, Premios, acciones básicas y visor cronológico del Event Log). | *Módulos/archivos:* `web-client`, `match-api`. | *Hecho:* usuario completa una partida jugable de principio a fin contra la CPU desde la web.

*Entregable usable:* Partida PvE jugable de principio a fin en navegador contra la CPU con historial de eventos.

---

## F4 — Reglas Completas e IA Mejorada

- [ ] **F4.1** — Añadir evoluciones (Stage 1/2 con restricción de turno), retirada con pago de energía y cálculo completo de Debilidades (`x2`) y Resistencias (`-30`) en `game-engine`. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 cubren cadenas evolutivas, retirada y modificadores de tipo.
- [ ] **F4.2** — Implementar los 5 estados alterados (`POISONED`, `BURNED`, `ASLEEP`, `PARALYZED`, `CONFUSED`) con `RandomProvider` inyectable, `POKEMON_CHECKUP` y limpieza al evolucionar/retirar. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 deterministas validan cada estado y sus interacciones.
- [ ] **F4.3** — Implementar `CardEffectRegistry` (Strategy/Registry por ID TCGdex) con bloques componibles (daño, robo, curación, estado, cambio, descarte energía) y efectos clave de Entrenadores y ataques de `base1`. | *Módulos/archivos:* `game-engine`. | *Hecho:* tests JUnit 5 validan efectos registrados sin condicionales monolíticos.
- [ ] **F4.4** — Implementar `TacticalBotStrategy` (`BASIC`/`TACTICAL` en `bot-ai` y `match-api`) evaluando evoluciones, ventaja de tipo, retirada defensiva y uso de Entrenadores. | *Módulos/archivos:* `bot-ai`, `match-api`. | *Hecho:* simulación JUnit 5 Táctico vs. Básico valida legalidad y ventaja táctica.
- [ ] **F4.5** — Integrar acciones completas (`EVOLVE`, `RETREAT`, `PLAY_TRAINER`), badges de estado, cola secuencial de animaciones sobre `List<GameEvent>` y cierre con actualización de stats de usuario en `web-client`/`match-api`/`auth-users`. | *Módulos/archivos:* `web-client`, `match-api`, `auth-users`. | *Hecho:* partida PvE completa con reglas avanzadas, animaciones legibles y stats persistidas.

*Entregable usable:* Simulador PvE completo con evoluciones, estados alterados, Entrenadores, Bot táctico y animaciones.

---

## Registro de avance

| Fecha | Paso | Resumen de lo hecho y validación ejecutada | Siguiente paso |
| :--- | :--- | :--- | :--- |
| 2026-11-23 | Docs | Estructura modular del monorepo creada (`AGENTS.md`, `docs/*`, 7 skills en `.agent/skills/`), reglas Git, `.gitignore` y repositorio inicializado en `main`. | F1.1 |
